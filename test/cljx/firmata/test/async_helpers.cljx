(ns firmata.test.async-helpers
  (:require #+clj
            [clojure.core.async :as a :refer [go <! <!! timeout alts!!]]
            #+cljs
            [cljs.core.async    :as a :refer [<!]])
  #+cljs 
  (:require-macros [cljs.core.async.macros :refer [go]]))

#+clj
(defn get-event
  [ch f]
  (f (first (alts!! [ch (timeout 100)]))))

#+cljs
(defn get-event
  [ch f]
  (go 
    (let [x (first (a/alts! [ch (a/timeout 100)]))]
      (f x))))

(defn wait-for-it 
  ([f] (wait-for-it 200 f))
  ([wait-time f]
    #+clj 
    (do 
      (<!! (timeout wait-time))
      (f))
    #+cljs
    (js/setTimeout #(f) wait-time)))