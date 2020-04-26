(ns clj-kondo.missing-body-in-when-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest missing-body-in-when-error-test
  (testing "test linting error of missing body in when for clojure"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing body in when expression"})
     (lint! "(when true)")))
  (testing "test linting error of missing body in when for cljs"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning,
        :message "Missing body in when expression"})
     (lint! "(when true)" "--lang" "cljs"))))

(deftest missing-body-in-when-valid-test
  (testing "test linting when with condition and body"
    (is (empty? (lint! "(when true (prn 1))"))))
  (testing "test linting when with multiple expresion in body"
    (is (empty? (lint! "(when true (prn 1) (+ 1 1))")))))
