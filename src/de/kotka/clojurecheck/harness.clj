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

(defn make-standard-harness
  "Creates a new standard harness, which reports in TAP format to *out*."
  []
  (hash-map :type         ::Standard
            :current-test (ref 0)))

(defmethod plan ::Standard
  [cnt]
  (print "1..")
  (print cnt)
  (newline)
  (flush))

(defmethod diag ::Standard
  [msg]
  (doseq [l (.split msg "\n")]
    (print "# ")
    (print l)
    (newline)
    (flush)))

(defmethod bail-out ::Standard
  ([]
   (bail-out nil))
  ([msg]
   (print "Bail out!")
   (when msg
     (print " ")
     (print msg))
   (newline)
   (flush)
   (java.lang.System/exit 1)))

(defmethod report-result ::Standard
  [m t desc]
  (if t
    (print "ok ")
    (print "not ok "))
  (print (dosync (commute (*the-harness* :current-test) inc)))
  (condp = m
    :todo (print " # TODO")
    :skip (print " # SKIP")
    nil)
  (when desc
    (print " - ")
    (print desc))
  (newline)
  (flush))

(defn make-batch-harness
  "Create a new batch harness suitable to run recursive tests. So one
  can specify tests, which themselves contain other tests."
  []
  (hash-map :type         ::Batch
            :our-plan     (ref :noplan)
            :current-test (ref 1)
            :failed-test  (ref false)
            :diagnostics  (ref "")))

(defmethod plan ::Batch
  [cnt]
  (dosync (ref-set (*the-harness* :our-plan) cnt)))

(defmethod diag ::Batch
  [msg]
  (dosync (commute (*the-harness* :diagnostics) #(str %1 \newline %2) msg)))

(defmethod bail-out ::Batch
  [msg]
  (dosync (commute (*the-harness* :diagnostics)
                   #(str %1 "Bailing out!" (when msg (str " " msg)))))
  (throw (FatalTestError.)))

(defmethod report-result ::Batch
  [m t desc]
  (when (and (not t) (not= m :todo))
    (dosync (ref-set (*the-harness* :failed-test) true)))
  (dosync (commute (*the-harness* :current-test) inc)))

(defmethod get-result ::Batch
  [harness]
  (dosync
    (and (or (= (deref (harness :our-plan)) :noplan)
             (= (deref (harness :our-plan))
                (deref (harness :current-test))))
         (not (deref (harness :failed-test))))))

(defmethod get-diagnostics ::Batch
  [harness]
  (deref (harness :diagnostics)))

(defvar *the-harness*
  (make-standard-harness)
  "The harness. This actually implements the TAP protocol itself, but may be
  re-bound via binding to enable different behaviour.")

(defn with-harness*
  "Bind the harness to the given one for the execution of thunk. Returns
  the harness afterwards."
  [harness thunk]
  (binding [*the-harness* harness]
    (thunk)
    harness))

(defmacro with-harness
  "Binds the harness to the given one for the execution of the body.
  Return the harness afterwards."
  [harness & body]
  `(with-harness* ~harness (fn [] ~@body)))
