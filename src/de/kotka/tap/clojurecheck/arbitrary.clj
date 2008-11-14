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

(defvar
  *prng*
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