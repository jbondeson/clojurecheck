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

(gen-interface
  :name    de.kotka.clojurecheck.IHarness
  :methods [[plan [Integer] Object]
            [diag [String] Object]
            [bailOut [String] Object]
            [reportResult [Object Boolean String] Object]
            [getResult [] Object]
            [getDiagnostics [] String]])

(import '(de.kotka.clojurecheck IHarness))

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
        (doseq [l (.split msg "\n")]
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

(defn make-batch-harness
  "Create a new batch harness suitable to run recursive tests. So one
  can specify tests, which themselves contain other tests."
  []
  (let [our-plan     (ref :noplan)
        current-test (ref 1)
        failed-test  (ref false)
        diagnostics  (ref "")]
    (proxy [IHarness] []
      (plan
        [count]
        (dosync (ref-set our-plan count)))

      (diag
        [msg]
        (dosync (commute diagnostics #(str %1 \newline %2) msg)))

      (bailOut
        [msg]
        (dosync (commute diagnostics #(str %1 "Bailing out!"
                                           (when msg (str " " msg)))))
        (throw (new FatalTestError)))

      (reportResult
        [m t desc]
        (when-not t
          (dosync (ref-set failed-test true)))
        (dosync (alter current-test inc)))

      (getResult
        []
        (and (or (= @our-plan :noplan)
                 (= @our-plan (dec @current-test)))
             (not @failed-test)))

      (getDiagnostics
        []
        @diagnostics))))

(defvar *the-harness*
  (make-standard-harness)
  "The handlers. This actually implements the TAP protocol itself, but may be
  re-bound via binding to enable different behaviour.")

(defn with-harness*
  [harness thunk]
  (binding [*the-harness* harness]
    (thunk)
    harness))

(defmacro with-harness
  [harness & body]
  `(with-harness* ~harness (fn [] ~@body)))
