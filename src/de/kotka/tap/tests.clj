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

(defn- test-tag [t] (if (seq? t) (first t) t))
(defn- actual   [t] (second t))
(defn- expected [t] (second (rest t)))

(defmulti
  #^{:doc
  "is* is the driver for the is macro and should not be called directly."}
  is*
  (fn [x & _] (test-tag x)))

(defmethod is* :default
  [t desc]
  `(test-driver (fn [] ~t)
                (quote ~t)
                (fn [] nil)
                ~desc
                (fn [e# a#] a#)
                (fn [e# a# r#]
                  (diag (.. "Expected: "
                            (concat a#)
                            (concat " to be true"))))))

(defmethod is* '=
  [t desc]
  `(test-driver (fn [] ~(actual t))
                (quote ~(actual t))
                (fn [] ~(expected t))
                ~desc
                (fn [e# a#] (= e# a#))
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to be:    " e#))
                  (diag (.concat "but was:  " r#)))))

(defmethod is* 'not=
  [t desc]
  `(test-driver (fn [] ~(actual t))
                (quote ~(actual t))
                (fn [] ~(expected t))
                ~desc
                (fn [e# a#] (not= e# a#))
                (fn [e# a# r#]
                  (diag (.concat "Expected:  " a#))
                  (diag (.concat "not to be: " e#)))))

(defmethod is* 'like?
  [t desc]
  `(test-driver (fn [] ~(actual t))
                (quote ~(actual t))
                (fn [] ~(expected t))
                ~desc
                (fn [e# a#] (not (nil? (re-find e# a#))))
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to match: " e#)))))

(defmethod is* 'unlike?
  [t desc]
  `(test-driver (fn [] ~(actual t))
                (quote ~(actual t))
                (fn [] ~(expected t))
                ~desc
                (fn [e# a#] (nil? (re-find e# a#)))
                (fn [e# a# r#]
                  (diag (.concat "Expected:     " a#))
                  (diag (.concat "not to match: " e#))
                  (diag (.concat "string was:   " r#)))))

(defmethod is* 'throwing?
  [t desc]
  `(test-driver (fn []
                  (try
                    (do
                      ~(second (rest t))
                      false)
                    (catch ~(second t) e#
                      true)))
                (quote ~(second (rest t)))
                (fn [] ~(second t))
                ~desc
                (fn [e# a#] a#)
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to throw: " e#)))))

(defmethod is* 'running?
  [t desc]
  `(test-driver (fn [] ~(second t))
                (quote ~(second t))
                (fn [] nil)
                ~desc
                (fn [e# a#] true)
                (fn [e# a# r#]
                  (diag (.concat "Expected " a#
                                 " to run through w/o exception.")))))

(defmacro is
  "is* runs the given comparison and reports any error or Exception. Based on
  the predicate used further diagnostic information is provided. See below
  for a list of supported predicates and corresponding examples.

  Supported Predicates:

    :default  - a simply yes/no test executing the provided form, which
                should evaluate to false in case the test fails
    =         - compare the actual vs. the expected value using =.
    not=      - same but with not=
    like?     - use re-find to check whether the given string matches
                the given regular expression
    unlike?   - use re-find to check whether the given string does
                not match the given regular expression
    throwing? - check whether the form throws the given Exception
    running?  - check whether the form runs w/o throwing an Exception

  Examples:

  | => (is (pressure-save? (flogiston-pressure)) „flogiston pressure is save“)
  | not ok 1 - flogiston pressure is save
  | # Expected: (pressure-save? (flogiston-pressure)) to be true

  | => (is (= (flogiston-pressure) *normal-flogiston-pressure*)
  |      „flogiston pressure is normal“)
  | not ok 2 - „flogiston pressure is normal“
  | # Expected: (flogiston-pressure)
  | # to be:    125
  | # but was:  58

  | => (def flogiston-reactor (is (running? (new FlogistonReactor))
  |                               „created new flogiston reactor“))
  | ok 3 - created new flogiston reactor"
  [t & desc]
  (is* t (first desc)))
