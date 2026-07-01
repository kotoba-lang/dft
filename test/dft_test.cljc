(ns dft-test
  (:require [clojure.test :refer [deftest is testing]]
            [dft]))
(deftest namespace-loads
  (testing "the restored CLJC namespace loads"
    (is (some? dft))))
