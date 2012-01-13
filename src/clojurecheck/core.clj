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
      (-> (- a b) Math/abs (mod 180)))

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
        [a    (cc/int)
         n    (cc/int)
         diff (cc/int :lower -180 :upper 180)]
        (let [b (+ a (* 360 n) diff)]
          (is (= (angular-diff a b) (Math/abs diff))))))

  And a result:
    my.package=> (run-tests)
    Testing my.package

    FAIL in (angular-diff-property) (core.clj:305)
    falsified 'angular-diff is smallest angel between a and b' in 5 attempts
    inputs where:
      a = -2
      n = 1
      diff = -3
    failed assertions where:
      expected: (= (angular-diff a b) (Math/abs diff))
        actual: (not (= 177 3))

    Ran 2 tests containing 7 assertions.
    1 failures, 0 errors.
    {:type :summary, :test 2, :pass 6, :fail 1, :error 0}"}
  clojurecheck.core
  (:refer-clojure
    :exclude (int float list vec set sorted-set hash-map sorted-map))
  (:use clojure.test))

(defprotocol Generator
  (^{:added "2.1"} arbitrary
    [generator size]
    "Choose an arbitrary value by virtue of the given generator."))

(deftype DomainValue [domain value])

(extend-protocol Generator
  clojure.lang.Fn
  (arbitrary [this size] (DomainValue. this (this size)))

  clojure.lang.PersistentVector
  (arbitrary [this size] (arbitrary (seq this) size))

  clojure.lang.ISeq
  (arbitrary
    [this size]
    (let [[generators values]
          (reduce (fn [[generators values] gen]
                    (let [dval (arbitrary gen size)]
                      [(conj generators (.domain dval))
                       (conj values (.value dval))]))
                  [[] []] this)]
      (DomainValue. generators values)))

  clojure.lang.IPersistentMap
  (arbitrary
    [this size]
    (let [ks    (keys this)
          gens  (vals this)
          dvals (arbitrary gens size)]
      (DomainValue. (zipmap ks (.domain dvals))
                    (zipmap ks (.value dvals)))))

  Object
  (arbitrary [this size] (arbitrary (constantly this) size)))

(deftype NumberDomain
  [random lower upper]
  Generator
  (arbitrary
    [this size]
    (let [[low high] (if size
                       [(max (- size) lower) (min size upper)]
                       [lower upper])]
      (DomainValue. this (+ low (random (- high low)))))))

(defn int
  "Generates a random integral number between lower and upper.
  The interval is limited by the size guidance."
  {:added "2.0"}
  [& {:keys [lower upper] :or {lower -32768 upper 32767}}]
  (NumberDomain. rand-int lower upper))

(defn float
  "Generates a random floating point number between lower and upper.
  The interval is limited by the size guidance."
  {:added "2.0"}
  [& {:keys [lower upper] :or {lower -32768.0 upper 32767.0}}]
  (NumberDomain. rand lower upper))

(def ^{:doc "Generates a random boolean value. Ignores the size guidance"
       :added "2.0"}
  bool
  (reify
    Generator
    (arbitrary [this _size] (DomainValue. this (< (rand) 0.5)))))

(defn frequency
  "Chooses one of the given generators based on the associated
  weights. The size guidance is passed verbatim to the chosen
  generator."
  {:added "2.0"}
  [choices]
  (let [freqs   (reductions + (vals choices))
        total   (last freqs)
        freqs   (map #(-> % (/ total) clojure.core/float) freqs)
        choices (map vector (keys choices) freqs)
        choose  (fn []
                  (let [dice (rand)]
                    ; XXX: c cannot be nil, because it is a generator.
                    (some (fn [[c f]] (when (< dice f) c)) choices)))]
    (reify
      Generator
      (arbitrary [this size] (arbitrary (choose) size)))))

(defn one-of
  "Chooses one of the given generators with equal probability.
  The size guidance is passed verbatim to the chosen generator."
  {:added "2.0"}
  [choices]
  (frequency (zipmap choices (repeat 1))))

(defn element
  "Choose one of the given elements with equal probability.
  Since the elements are \"constant\" generators the size
  guidance is ignored."
  {:added "2.0"}
  [choices]
  (one-of (map constantly choices)))

(defn guard
  "Guard the given generator with the predicate. But note, that this
  can be quite inefficient. It is better to generate only interesting
  values in the first place."
  {:added "2.1"}
  [generator pred]
  (reify
    Generator
    (arbitrary
      [this size]
      (when-let [dom-value (arbitrary generator size)]
        (when (pred (.value dom-value))
          dom-value)))))

(deftype BoundDomain [generators values])

(def ^:private assert-args
  (ns-resolve 'clojure.core 'assert-args))

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
  {:added "2.0"}
  [bindings expr]
  (assert-args
       (vector? bindings)       "a vector for its bindings"
       (even? (count bindings)) "an even number of forms in the bindings vector")
  (let [size       (gensym "size__")
        locals     (gensym "locals__")
        generators (gensym "generators__")
        values     (gensym "values__")
        emit-g     (fn [[local gen] body]
                     `(when-let [dval# (arbitrary ~gen ~size)]
                        (let [~local      (.value dval#)
                              ~generators (conj ~generators (.domain dval#))
                              ~values     (conj ~values ~local)
                              ~locals     (conj ~locals ~(keyword local))]
                          ~body)))
        emit-p     (fn [pred body]
                     `(when ~pred
                        ~body))
        emit-l     (fn [bindings body]
                     `(let ~bindings
                        ~body))]
    `(let [~locals     []
           ~generators []
           ~values     []]
       (reify
         Generator
         (~'arbitrary
           [this# ~size]
             ~(reduce
                (fn [body [v t :as bs]]
                  (case t
                    :when (emit-p v body)
                    :let  (emit-l v body)
                    (emit-g [t v] body)))
                `(DomainValue. (BoundDomain. (zipmap ~locals ~generators)
                                             (zipmap ~locals ~values))
                               ~expr)
                (partition 2 (rseq bindings))))))))

(defn list
  "Generates a list based on the given generator. The length of
  the list is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the
  item generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (let-gen [len      length
            elements (repeat len item)]
    elements))

(defn vec
  "Generates a vector based on the given generator. The length of
  the vector is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the item
  generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (let-gen [elems (list item :length length)]
    (clojure.core/vec elems)))

(defn set
  "Generates a set based on the given generator. The size of
  the set is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the
  item generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (let-gen [elems (list item :length length)]
    (clojure.core/set elems)))

(defn sorted-set
  "Generates a sorted-set based on the given generator. The size of
  the sorted-set is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the item
  generator."
  {:added "2.0"}
  [item & {:keys [length] :or {length (int)}}]
  (let-gen [elems (list item :length length)]
    (apply clojure.core/sorted-set elems)))

(defn hash-map
  "Generates a hash-map based on the given generators. The size of
  the hash-map is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the key
  and value generators."
  {:added "2.0"}
  [keys vals & {:keys [length] :or {length (int)}}]
  (let-gen [len length
            ks  (list keys :length (constantly len))
            vs  (list vals :length (constantly len))]
    (zipmap ks vs)))

(defn sorted-map
  "Generates a sorted-map based on the given generators. The size of
  the sorted-map is an integer generator. The default grows with the
  size guidance. The size guidance is passed verbatim to the key and
  value generators."
  {:added "2.0"}
  [keys vals & {:keys [length] :or {length (int)}}]
  (let-gen [len length
            ks  (list keys :length (constantly len))
            vs  (list vals :length (constantly len))]
    (apply clojure.core/sorted-map (interleave ks vs))))

(defn string
  "Generates a string taking characters from the given generator. The
  length of the string is an integer generator. The default grows with
  the size guidance. The size guidance is passed verbatim to the
  character generator."
  {:added "2.1"}
  [characters & {:keys [length] :or {length (int)}}]
  (let-gen [chs (list characters :length length)]
    (apply str chs)))

(defn sized
  "Modify the size guidance according to f and pass it on to the
  given generator. If f is not a function it will be taken turned
  into a function returning the given value as constant."
  {:added "2.0"}
  [f gen]
  (let [f (if (fn? f) f (constantly f))]
    (reify Generator (arbitrary [this size] (arbitrary gen (f size))))))

(def ^{:doc "Number of trials a property is tested with generated input.
  Default is 1000."
       :added "2.0"
       :dynamic true}
  *trials*
  1000)

(defn generate-input
  "Try *trials* times to generate a valid random input."
  {:added "2.0"}
  [gen size]
  (loop [n *trials*]
    (if (pos? n)
      (if-let [dval (arbitrary gen size)]
        dval
        (recur (dec n)))
      (throw (Exception. "trials exhausted while generating input")))))

(defn *size-scale*
  "The scale function used to scale up the size guidance with increasing
  trials while testing a property with generated input."
  {:added "2.0"
   :dynamic true}
  [n]
  (if (even? n)
    (/ n 2)
    (/ (inc n) 2)))

(defn property*
    "The property* driver handles the work when testing a property. It
  expects:
    * a descriptive message for failure reporting
    * a list of locals (also for reporting)
    * a generator which takes the scaled size and returns the input
      for the property
    * the property test in form of a function of the generated
      input."
    {:added "2.0"}
    [msg locals gen prop]
    (let [build-msg (fn [input n]
                        (with-out-str
                          (println (str "Falsified '" (if msg msg "property") "'")
                                   "in" n "attempts.")
                          (println "inputs where:")
                          (doseq [[local value] (map vector locals input)]
                            (println " " local "=" (pr-str value))))
                        )
           results   (atom [])
           report-fn #(swap! results conj %)]
       (loop [n 1]
         (reset! results [])
         (if (< *trials* n)
           (do (report {:type :pass}) true)
           (let [input (->> n *size-scale* (generate-input gen))
                 failures (do
                             (binding [report report-fn]
                               (prop (.value input)))
                             (filter #(-> % :type (not= :pass)) @results))]
             (if (not (seq failures))
               (recur (inc n))
               (doseq [failure failures]
                 (do-report
                  (assoc failure
                    :message (build-msg (.value input) n)))))))))
  )

(defmacro property
  "Defines a property consisting of a binding vector as for let-gen
  which associates locals with the given generators. When testing the
  property the locals will be assigned the values generated.

  The body is a normal deftest body."
  {:added "2.0"}
  [msg bindings & body]
  (let [locals (remove keyword? (take-nth 2 bindings))]
    `(property* ~msg
                (quote ~locals)
                (let-gen ~bindings [~@locals])
                (fn [[~@locals]] ~@body))))