;-
; Copyright 2008 (c) Meikel Brandmeyer.
; All rights reserved.
;
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
;
; The above copyright notice and this permission notice shall be included in
; all copies or substantial portions of the Software.
;
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
; THE SOFTWARE.

(clojure/in-ns 'de.kotka.tap)

(defn- apply-generator
  [g s]
  (if (vector? g)
    (apply arbitrary (conj g s))
    (arbitrary g s)))

(defmacro let-gen
  "let-gen creates a new generator, which binds the given generators
  to the given variables and then executes the body. It is similar to
  for-all, which is used to define a test case. However the let-gen
  is not supposed to be run with test cases in the body. The body must
  not have side effects."
  [gen-bindings & body]
  (let [size (gensym "let-gen_size__")]
    `(fn [_# ~size]
       (let ~(vec (mapcat (fn [[v g]]
                            [v `((ns-resolve (symbol "de.kotka.tap")
                                             (symbol "apply-generator"))
                                   ~g ~size)])
                          (partition 2 gen-bindings)))
         ~@body))))

(defn unit
  "unit returns a generator, which always returns the given value."
  [x]
  (constantly x))

(defmacro with-size
  "Although normally not necessary, it is sometimes desirable to have
  access to the size parameter when building a generator. This can be
  achieved by the with-size combinator. The body must return a generator.

  Example:

  | => (with-size s (let-gen [x [Integer 0 size]] x))"
  [sv & body]
  `(fn [_# size#] (arbitrary ((fn [~sv] ~@body) size#) size#)))

(defn one-of
  "one-of chooses one of the given generators with equal probability."
  [& gens]
  (let [len (dec (count gens))]
    (let-gen [l [Integer 0 len]
              v (nth gens l)]
      v)))

(defn frequency
  "frequency takes a list of of generators, each prefix with weight.
  The weights have to sum up to 100. The higher the weight,
  the more often the following generator is chosen."
  [& weights-and-gens]
  (let [weights-and-gens (partition 2 weights-and-gens)
        weights-and-gens (reduce (fn [w-n-g [w g]]
                                   (let [p-w (first (peek w-n-g))]
                                     (conj w-n-g [(+ p-w w) g])))
                                 [(first weights-and-gens)]
                                 (rest weights-and-gens))]
    (let-gen [guess [Integer 1 100]
              v     (first (drop-while #(< (first %) guess)
                                       weights-and-gens))]
      v)))

(defn elements
  "elements returns a generator, which chooses one of the given values."
  [& elems]
  (let [len (dec (count elems))]
    (let-gen [l [Integer 0 len]]
      (nth elems l))))
