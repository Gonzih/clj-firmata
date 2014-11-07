(ns firmata.test.stream.core
  (:require [firmata.stream.core :as sc]
            #+clj 
            [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs
            [cemerick.cljs.test :as t])
  #+cljs 
  (:require-macros [cemerick.cljs.test
                       :refer (is deftest with-test run-tests testing test-var)]))

(deftest test-attempt-reconnect
    ; TODO: Implement tests
    (is (= true false)))