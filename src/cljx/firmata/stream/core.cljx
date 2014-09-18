(ns firmata.stream.core
  (:require #+clj 
            [clojure.core.async :as a :refer [go <! timeout]]
            #+cljs
            [cljs.core.async    :as a :refer [<! timeout]])
  #+cljs
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defprotocol ByteReader
  "Enables reading on byte at a time."

  (read! [this] "reads a byte, and removes it from the stream"))


(defprotocol FirmataStream
  "A FirmataStream provides methods for creating connections, writing
  values to and listening for events on a Firmata-enabled device."

  (open! [this] "opens the stream")
  (close! [this] "closes the stream")

  (reconnect! [this] "reconnects the stream")
  (connected? [this] "Returns true if connected")

  (listen [this handler] "listens for data on this stream")
  (write [this data]))


(defn attempt-reconnect 
  "attempts to reconnect the stream with an exponential falloff"
  [stream]
  (go 
    (loop [wait-time 1000
           retry-limit 10] ; TODO: Configure this
      (<! (timeout wait-time))

      (println "Attempting reconnect") ; TODO: Configure logging
      (reconnect! stream)

      (if (not (and (connected? stream) (pos? retry-limit)))
        (recur (max 30000 (* 2 wait-time))
               (dec retry-limit))))))