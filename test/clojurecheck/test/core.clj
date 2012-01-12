(ns clojurecheck.test.core
  (:require [clojurecheck.core :as cc])
  (:use [clojure.test]))

(defn angular-diff
  [a b]
  (-> (- a b) Math/abs (mod 180)))

(deftest angular-diff-standard-test
  (are [x y] (= x y)
       (angular-diff   0   0) 0
       (angular-diff  90  90) 0
       (angular-diff   0  45) 45
       (angular-diff  45   0) 45
       (angular-diff   0 270) 90
       (angular-diff (* 360 2) (+ (* 360 4) 23)) 23))

(deftest multi-failure
  (is (= 1 2))
  (is (= 1 3)))

(deftest angular-diff-property
  (cc/property "angular-diff is smallest angel between a and b"
               [a    (cc/int)
                n    (cc/int)
                diff (cc/int :lower -180 :upper 180)]
               (let [b (+ a (* 360 n) diff)]
                 (is (= (angular-diff a b) (Math/abs diff)))
                 (is (= (* 15 (angular-diff a b) (Math/abs diff))))))
  
  #_(cc/property "doesn't blow up"
               [a (cc/int)]
               (is (throw "oh no!"))))
