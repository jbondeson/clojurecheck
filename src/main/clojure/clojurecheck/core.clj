; Copyright 2010 © Meikel Brandmeyer.
; All rights reserved.
; 
; Permission is hereby granted, free of charge, to any person obtaining a
; copy of this software and associated documentation files (the "Software"),
; to deal in the Software without restriction, including without limitation
; the rights to use, copy, modify, merge, publish, distribute, sublicense,
; and/or sell copies of the Software, and to permit persons to whom the
; Software is furnished to do so, subject to the following conditions:
; 
; The above copyright notice and this permission notice shall be included
; in all copies or substantial portions of the Software.
; 
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
; OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
; THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
; FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
; DEALINGS IN THE SOFTWARE.

(ns ^{:author "Meikel Brandmeyer"
      :doc
  "clojurecheck - property based testing

  clojurecheck is an extensions to clojure.test. It provides generators
  for different values and datastructures. With their help random input
  for test cases are generated to test the behaviour of the code under
  test with more and more complex input.

  Example:
    (ns my.package
      (:use clojure.test)
      (:require [clojurecheck.core :as cc]))

    (defn angular-diff
      [a b]
      (-> (- a b) (mod 180) Math/abs))

    (deftest angular-diff-standard-test
      (are [x y] (= x y)
        (angular-diff   0   0) 0
        (angular-diff  90  90) 0
        (angular-diff   0  45) 45
        (angular-diff  45   0) 45
        (angular-diff   0 270) 90
        (angular-diff (* 360 2) (+ (* 360 4) 23)) 23))

    (deftest angular-diff-property
      (cc/property „angular-diff is smallest angel between a and b“
        [diff (cc/float :lower -180.0 :upper 180.0)
         a    (cc/float :lower 0.0 :upper 360.0)
         n    (cc/int)]
        (let [b (+ a (* 360 n) diff)]
          (is (= (angular-diff a b) (Math/abs diff))))))

  And a result:
    user=> (run-tests)
    Testing my.package

    FAIL in (angular-diff-property) (core.clj:288)
    falsified angular-diff is smallest angel between a and b in 3 attempts
    inputs where:
      diff = 1.3374370509881066
      a = 0.5071098240831757
      n = -1
    failed assertions where:
      expected: (= (angular-diff a b) (Math/abs diff))
        actual: (not (= 178.66256294901189 1.3374370509881066))

    Ran 2 tests containing 7 assertions.
    1 failures, 0 errors.
    {:type :summary, :test 2, :pass 6, :fail 1, :error 0}"}
  clojurecheck.core
  (:refer-clojure :exclude (int float))
  (:use clojure.test))

(defn- gen-number
  [random lower upper size]
  (let [[low high] (if size
                     [(max (- size) lower) (min size upper)]
                     [lower upper])]
    (+ low (random (- high low)))))

(defn int
  "Generates a random integral number between lower and upper.
  The interval is limited by the size guidance."
  {:added "1.0"}
  [& {:keys [lower upper] :or {lower -32768 upper 32767}}]
  (fn [size]
    (gen-number rand-int lower upper size)))

(defn float
  "Generates a random floating point number between lower and upper.
  The interval is limited by the size guidance."
  {:added "1.0"}
  [& {:keys [lower upper] :or {lower -32768.0 upper 32767.0}}]
  (fn [size]
    (gen-number rand lower upper size)))

(def ^{:doc "Generates a random boolean value. Ignores the size guidance"
       :added "1.0"}
  bool
  (fn [size]
    (< (rand) 0.5)))

(defn frequency
  "Chooses one of the given generators based on the associated
  weights. The size guidance is passed verbatim to the chosen
  generator."
  {:added "1.0"}
  [choices]
  (let [freqs   (reductions + (vals choices))
        total   (last freqs)
        freqs   (map #(-> % (/ total) clojure.core/float) freqs)
        choices (map vector (keys choices) freqs)
        choose  (fn []
                  (let [dice (rand)]
                    (some (fn [[c f]] (when (< f dice) c)) choices)))]
    (fn [size]
      ((choose) size))))

(defn one-of
  "Chooses one of the given generators with equal probability.
  The size guidance is passed verbatim to the chosen generator."
  {:added "1.0"}
  [choices]
  (frequency (zipmap choices (repeat 1))))

(defn element
  "Choose one of the given elements with equal probability.
  Since the elements are \"constant\" generators the size
  guidance is ignored."
  {:added "1.0"}
  [choices]
  (one-of (map constantly choices)))

(def ^{:doc "Number of maximum retries to generate a valid value."
       :added "1.0"}
  *retries*
  2000)

(defmacro let-gen
  "Takes a vector of let-like bindings. let-gen returns itself
  a generator. When called it evaluates the generators on the
  right hand side and assigns the result to the corresponding
  local. Later generator definitions may refer to previous locals
  as in a usual let.

  Similar to for and doseq you can intersperse the bindings with
  directives, which modify the behaviour.

    * :when (pred? ...):
      In case the predicate evaluates to false the generation
      process is cancelled and retried.
    * :let [...]:
      Takes a normal let-style binding and makes the bindings
      available to the following generator definitions."
  {:added "1.0"}
  [bindings & body]
  (@#'clojure.core/assert-args let-gen
       (vector? bindings)       "a vector for its bindings"
       (even? (count bindings)) "an even number of forms in the bindings vector")
  (let [size   (gensym "size__")
        emit-g (fn [[local gen] body]
                 `(let ~[local (clojure.core/list gen size)]
                    ~body))
        emit-p (fn [pred body]
                 `(if ~pred
                    ~body
                    [:retry]))
        emit-l (fn [bindings body]
                 `(let ~bindings
                    ~body))]
    `(fn [~size]
       (loop [n# *retries*]
         (if-not (zero? n#)
           (let [[result# value#]
                 ~(reduce
                    (fn [body [v t :as bs]]
                      (case t
                        :when (emit-p v body)
                        :let  (emit-l v body)
                        (emit-g [t v] body)))
                    `[:value (do ~@body)]
                    (partition 2 (rseq bindings)))]
             (if (= result# :retry)
               (recur (dec n#))
               value#))
           (throw
             (Exception.
               (str "Retries exhausted (" *retries* " attempts)"))))))))
