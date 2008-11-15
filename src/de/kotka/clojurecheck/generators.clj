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

(clojure.core/in-ns 'de.kotka.clojurecheck)

(defmethod arbitrary Double
  ([_ mn mx _]
   (+ mn (* (- mx mn) (.nextDouble *prng*))))
  ([_ size]
   (arbitrary Double (- size) size nil)))

(defmethod arbitrary Integer
  ([_ mn mx _]
   (int (Math/round (arbitrary Double mn mx nil))))
  ([_ size]
   (arbitrary Integer (- size) size nil)))

(defmethod arbitrary Character
  ([_ source _]
   (.charAt source (arbitrary Integer 0 (dec (.length source)) nil)))
  ([_ _]
   (char (arbitrary Integer 32 255 nil))))

(defmethod arbitrary Boolean
  [_ _]
  (arbitrary (elements true false) nil))
