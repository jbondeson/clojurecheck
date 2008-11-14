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
                            (if (vector? g)
                              [v `(arbitrary ~@g ~size)]
                              [v `(arbitrary ~g ~size)]))
                          (partition 2 gen-bindings)))
         ~@body))))

(defn unit
  "unit returns a generator, which always returns the given value."
  [x]
  (constantly x))
