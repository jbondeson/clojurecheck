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

(defmacro ok?
  "Simple yes/no test. <ok?> simply tests whether the given test evaluates to
  true. In this nature it cannot give much of diagnostic information, but it
  is sufficient for simple predicate tests.

  Example:

  | => (ok? (pressure-save? (flogiston-pressure)) „flogiston pressure is save“)
  | not ok 1 - flogiston pressure is save
  | # Expected: (pressure-save? (flogiston-pressure)) to be true"
  [t & desc]
  `(test-driver (fn [] ~t)
                (quote ~t)
                (fn [] nil)
                ~(first desc)
                (fn [e# a#] a#)
                (fn [e# a# r#]
                  (diag (.. "Expected: "
                            (concat a#)
                            (concat " to be true"))))))

(defmacro is?
  "Comparison with „=“. <is?> does evaluate the given expected value and the
  actual value. It compares both using „=“. If this fails, <is?> presents the
  user some more diagnostics about why the test failed.

  Example:

  | => (is? (flogiston-pressure) *normal-flogiston-pressure*
  |      „flogiston pressure is normal“)
  | not ok 1 - „flogiston pressure is normal“
  | # Expected: (flogiston-pressure)
  | # to be:    125
  | # but was:  58"
  [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (= e# a#))
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to be:    " e#))
                  (diag (.concat "but was:  " r#)))))

(defmacro isnt?
  "Compare using „not=“. <isnt?> is similar to <is?>, but actually succeeds
  when the actual is not equal to the expected value."
  [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (not= e# a#))
                (fn [e# a# r#]
                  (diag (.concat "Expected:  " a#))
                  (diag (.concat "not to be: " e#)))))

(defmacro like?
  "String checking with regular expressions. <like?> checks whether the given
  string matches the supplied regular expression."
  [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (not (nil? (re-find e# a#))))
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to match: " e#)))))

(defmacro unlike?
  "String checking with regular expressions. <unlike?> checks whether the given
  string does *not* match the supplied regular expression."
  [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (nil? (re-find e# a#)))
                (fn [e# a# r#]
                  (diag (.concat "Expected:     " a#))
                  (diag (.concat "not to match: " e#))
                  (diag (.concat "string was:   " r#)))))

(defmacro throws?
  "Check whether given the Exception is thrown. In case the supplied body runs
  through without throwing an exception or if an Exception different from the
  named is thrown, the test fails."
  [exn body & desc]
  `(test-driver (fn []
                  (try
                    (do
                      ~body
                      false)
                    (catch ~exn e#
                      true)))
                (quote ~body)
                (fn [] ~exn)
                ~(first desc)
                (fn [e# a#] a#)
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to throw: " e#)))))

(defmacro runs?
  "The code does not throw an Exception. This is not really a test at all.
  <runs?> just runs the supplied body and succeeds if body runs through
  without throwing an Exception. It returns whatever the body returns.

  Example:

  | => (def flogiston-reactor (runs? (new FlogistonReactor)
  |                             „created new flogiston reactor“))
  | ok 1 - created new flogiston reactor"
  [body & desc]
  `(test-driver (fn [] ~body)
                (quote ~body)
                (fn [] nil)
                ~(first desc)
                (fn [e# a#] true)
                (fn [e# a# r#]
                  (diag (.concat "Expected " a#
                                 " to run through w/o exception.")))))
