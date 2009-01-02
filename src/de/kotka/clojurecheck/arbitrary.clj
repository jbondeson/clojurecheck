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

; (:use [clojure.contrib.def :only (defvar)])

(clojure.core/in-ns 'de.kotka.clojurecheck)

(defvar *prng*
  (new java.util.Random)
  "The PRNG used to generate the test data.")

(defn with-prng*
  "Install the given PRNG while running the given thunk. It must conform
  to the interface of java.util.Random!"
  [prng thunk]
  (binding [*prng* prng]
    (thunk)))

(defmacro with-prng
  "Install the given PRNG while running the given body. It must conform
  to the interface of java.util.Random!"
  [prng & body]
  `(with-prng* ~prng (fn [] ~@body)))

(defmulti
  #^{:doc
  "The arbitrary multimethod defines generators for different types.
  It takes at least two arguments: the type and a size argument. The
  type may be a class or keyword defining which value to generator. It
  is also possible to pass more arguments to the method. However the
  type will always be the first one, size the last."}
  arbitrary
  (fn [x & _] x))

(defmethod arbitrary :default
  [x size]
  (x nil size))

(defn- apply-generator
  [g s]
  (if (vector? g)
    (apply arbitrary (conj g s))
    (arbitrary g s)))

(defn- make-binding-vector
  [size gen-bindings]
  (vec (mapcat (fn [[v g]]
                 [v `((ns-resolve (symbol "de.kotka.clojurecheck")
                                  (symbol "apply-generator"))
                        ~g ~size)])
               (partition 2 gen-bindings))))

(defvar *max-checks*
  100
  "The maximum number of iterations, which are done by for-all.")

(defn for-all*
  "This is the driver for the for-all macro. Should not be called
  directly."
  [gen test-fn]
  (loop [i 0]
    (let [h     (make-batch-harness)
          input (gen i)]
      (binding [*the-harness* h]
        (try
          (test-fn input)
          (catch Exception e
            (report-result *mode* false nil)
            (diag (str "Exception was thrown: " e)))))
      (if (and (< i *max-checks*) (.getResult h))
        (recur (inc i))
        [h input]))))

(defmacro for-all
  "for-all binds the given generators to the given values and runs the
  body. The body might define any tests (and even a plan) since it is
  run against its own harness."
  [gen-bindings & body]
  (let [size    (gensym "for-all_size__")
        xs      (take-nth 2 gen-bindings)
        gen     `(fn [~size]
                   (let ~(make-binding-vector size gen-bindings)
                     (hash-map ~@(mapcat (fn [x] `[(quote ~x) ~x]) xs))))
        test-fn `(fn [~(hash-map :syms (into [] xs))] ~@body)]
    `(for-all* ~gen ~test-fn)))

(defn holds?*
  "This is the driver function for the holds? macro and should not be
  called directly."
  [prop desc]
  (let [[h vs] (prop)]
    (if (.getResult h)
      (report-result *mode* true desc)
      (do
        (report-result *mode* false desc)
        (diag "Property failed, counter example is:")
        (doseq [[vr vl] vs]
          (diag (str "  " vr " => " vl)))
        (diag "\nDiagnostics were:")
        (diag (.getDiagnostics h))))))

(defmacro holds?
  "holds? tests the given property. A property is defined by for-all."
  [prop & desc]
  `(holds?* (fn [] ~prop) ~(first desc)))
