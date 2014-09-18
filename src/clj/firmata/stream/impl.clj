(ns firmata.stream.impl
  (:require [firmata.stream.core :refer [FirmataStream ByteReader attempt-reconnect]]
            [serial.core :as serial]
            [clojure.core.async :as a :refer [go <! timeout]])
  (:import [java.net InetSocketAddress Socket]
           [java.io InputStream IOException]))

(defn- handle [firmata-stream handler is]
  (handler (reify ByteReader 
    (read! [_]
      (try 
        (.read is)
        (catch IOException e
          (attempt-reconnect firmata-stream)
          -1))))))


(defrecord SerialStream [port-name baud-rate]
  FirmataStream

  (open! [this]
    (let [serial-port (serial/open port-name :baud-rate baud-rate)]
      (assoc! this :serial-port serial-port)))

  (close! [this]
    (when-let [serial-port (:serial-port this)]
      (serial/close serial-port)
      (dissoc! this :serial-port)))

  (reconnect! [this])

  (connected? [this])

  (write [this data]
    (when-let [serial-port (:serial-port this)]
      (serial/write serial-port data)))

  (listen [this handler]
    (when-let [serial-port (:serial-port this)]
      (serial/listen serial-port #(handle this handler %) false))))

(defn create-serial-stream [port-name baud-rate]
  (SerialStream. port-name baud-rate))

(defrecord SocketClientStream [host port]
  FirmataStream

  (open! [this]
    (try
      (let [addr (InetSocketAddress. (:host this) (:port this))
            socket (Socket.)]
        (do
          (.setSoTimeout socket 0)
          (.connect socket addr)
          (assoc this :socket socket)))

      ; TODO: Is there a better way to deal with these?
      (catch java.net.UnknownHostException uhe
        (throw (RuntimeException. (str "Unknown host - " host) uhe)))

      (catch IllegalArgumentException iae
        (throw (RuntimeException. (str "Invalid port - " port) iae)))

      (catch java.net.SocketException se
        (throw (RuntimeException. (str "Unable to connect to " host ":" port) se)))))

  (close! [this]
    (when-let [socket (:socket this)]
      (try (.close (:socket this))
        (catch IOException se))
      (dissoc this :socket)))


  (reconnect! [this])

  (connected? [this])

  (write [this data]
    ; NOTE: This relies on the fact that we're using clj-serial,
    ; so we can use serial/to-bytes here
    (when-let [socket (:socket this)]
      (let [output-stream (.getOutputStream socket)]
        (.write output-stream (serial/to-bytes data))
        (.flush output-stream))))

  (listen [this handler]
    (when-let [socket (:socket this)]
      (go
        (let [in (.getInputStream socket)]
          (while (.isConnected socket)
            (if (pos? (.available in))
              (handle this handler in)
              ; slows the loop down to the the update rate of the device
              ; TODO: This should be configurable
              (<! (timeout 19)))))))))

(defn create-socket-client-stream [host port]
  (SocketClientStream. host port))

