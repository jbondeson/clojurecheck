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

(defn make-standard-harness
  "make-standard-harness creates a new standard harness."
  []
  (let [current-test (ref 1)]
    (proxy [IHarness] []
      (plan
        [count]
        (print "1..")
        (print count)
        (newline)
        (flush))

      (diag
        [msg]
        (doseq l (.split msg "\n")
          (print "# ")
          (print l)
          (newline)
          (flush)))

      (bailOut
        [msg]
        (print "Bail out!")
        (when msg
          (print " ")
          (print msg))
        (newline)
        (flush)
        (.exit java.lang.System 1))

      (reportResult
        [m t desc]
        (if t
          (print "ok ")
          (print "not ok "))
        (print (dosync
                 (let [c @current-test]
                   (alter current-test inc)
                   c)))
        (cond
          (= m :todo) (print " # TODO")
          (= m :skip) (print " # SKIP"))
        (when-not (nil? desc)
          (print " - ")
          (print desc))
        (newline)
        (flush)))))

(defvar *the-harness*
  (make-standard-harness)
  "The handlers. This actually implements the TAP protocol itself, but may be
  re-bound via binding to enable different behaviour.")