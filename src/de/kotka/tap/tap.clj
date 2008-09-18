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

; Namespace: de.kotka.tap
;
; An implementation of the Test Anything Protocol. It is a simple protocol to
; transfer information from tests to a harness, which in turn extracts the
; information in various ways. The protocol itself is also human readable. It's
; widely used in the Perl community.
;
; By using TAP one separates the framework of running the tests from the
; logic which displays the information of the test results. This could be
; a console runner like Perl's „prove“ utility or a graphical program or
; even a script controlling the automatic build process. The easy structure
; of TAP makes it easy to implement parsers for it.

(clojure/ns de.kotka.tap
  (:refer-clojure)
  (:use clojure.contrib.def))

(defn plan
  "
  | Function: plan
  |
  | Print the test plan. Ie. the number of tests you intend to run. This gives
  | the harness a chance to see, whether the tests ran completely. It is not
  | strictly necessary to provide a plan. However it is strongly encouraged to
  | do so.
  |
  | Example:
  |
  | > => (plan 10)
  | > 1..10
  "
  [count]
  (print "1..")
  (print count)
  (newline)
  (flush))

(defn diag
  "
  | Function: diag
  |
  | Print diagnostics. Sometimes a test script wants to provide diagnostic
  | information to the user. Eg. <is?> and friends provide information about
  | the deviation from the expected outcome to the user. <diag> is a utility
  | which takes away the burden of the special formating of such information
  | from the test script.
  |
  | Example:
  |
  | > => (diag „flogiston pressure dropping rapidly“)
  | > # flogiston pressure dropping rapidly
  "
  [msg]
  (doseq l (.split msg "\n")
    (print "# ")
    (print l)
    (newline)
    (flush)))

(defn bail-out
  "
  | Function: bail-out
  |
  | Bail out of the test process. Sometimes the system or the environment is so
  | messed up, that further testing doesn't make sense. Then <bail-out> may be
  | used to stop further testing immediately. Optionally a reason about the
  | bailing out, may be given to provide to the user, why the testing stopped.
  |
  | Example:
  |
  | > => (bail-out)
  | > Bail out!
  | > => (bail-out „flogiston pressure too low“)
  | > Bail out! flogiston pressure too low
  "
  [& msg]
  (print "Bail out!")
  (when-not (nil? msg)
    (print " ")
    (print (first msg)))
  (newline)
  (flush)
  (.exit java.lang.System 1))

(defvar- *mode*        :normal)
(defvar- *skip-reason* :none)

(defn todo*
  "
  | Function: todo*
  |
  | This is the driver for the <todo> macro. This function should not
  | be called directly.
  "
  [body]
  (binding [*mode* :todo]
    (body)))

(defmacro todo
  "
  | Macro: todo
  |
  | Marking unfinished functionality. Wrapping tests in a <todo> call marks the
  | tests with the TODO directive. This information might be used by the
  | harness to provide further hints to the user. Perl's „prove“ utility
  | considers TODO tests to be irrelevant to the whole result of the test
  | script. They should fail however. Succeeding TODO tests are reported by
  | „prove“ in a special way, giving a hint that the TODO status of the test
  | should be revised.
  |
  | Example:
  |
  | > => (todo
  | >      (ok? (taken-over-the-world) „take over the world“))
  | > not ok 1 # TODO take over the world
  "
  [& body]
  `(todo* (fn [] ~@body)))

(defn skip*
  "
  | Function: skip*
  |
  | This is the driver for the <skip> macro. This function should not
  | be called directly.
  "
  [reason body]
  (binding [*mode*        :skip
            *skip-reason* reason]
     (body)))

(defmacro skip
  "
  | Macro: skip
  |
  | Skip certain tests. Sometimes certain functionality is disabled, eg. when
  | it is not applicable to the platform currently running on or when the
  | functionality is disabled on purpose. The tests wrapped in the <skip> call
  | are actually not run at all, but reported to succeed and marked with the
  | SKIP directive and the given reason.
  |
  | Note:
  |
  | Code between tests *is* run!
  |
  | Example:
  |
  | > => (skip „frobnicator library not available“
  | >      (ok? (frobnicator/do-frobnicate foo) „foo is frobnicatable“))
  | > ok 1 # SKIP frobnicator library not available
  |
  | The call to „do-frobnicate“ is actually not done. The test is always
  | reported to succeed marked with the SKIP directive and the reason, why the
  | test was skipped.
  "
  [reason & body]
  `(skip* ~reason (fn [] ~@body)))

(defn skip-if*
  "
  | Function: skip-if*
  |
  | This is the driver for the <skip-if> macro. This function should not
  | be called directly.
  "
  [t reason body]
  (if t
    (skip* reason body)
    (body)))

(defmacro skip-if
  "
  | Macro: skip-if
  |
  | Conditionally skip tests. In case the guard tests evaluates to „true“ the
  | given tests are run in <skip> call with the given reason. Otherwise the
  | tests are run normally.
  |
  | Example:
  |
  | > => (skip-if (< (flogiston-pressure) 100) „flogiston pressure too low“
  |        (ok? (inject-flogiston) „flogiston injection works“))
  "
  [t reason & body]
  `(skip-if* ~t ~reason (fn [] ~@body)))

(defn- print-result
  [c m t desc]
  (if t
    (print "ok ")
    (print "not ok "))
  (print c)
  (cond
    (= m :todo) (print " # TODO")
    (= m :skip) (print " # SKIP"))
  (when-not (nil? desc)
    (print " - ")
    (print desc))
  (newline)
  (flush))

(let [current-test (ref 1)]
  (defn test-driver
    "
    | Function: test-driver
    |
    | Driver function for the tests. This function should only be called, when
    | defining new test macros. The driver receives the actual form under test
    | as a closure as well as it's quoted form. Similarly the expected value is
    | transferred. The following description is optional and might be „nil“.
    | Finally two callbacks to compare the actual result against the expected
    | one and to print a diagnostic message in case of failure.
    |
    | In case an exception is thrown it is caught and reported via a diagnostic
    | message to the user. The test fails in that case.
    |
    | Example:
    |
    | > => (defmacro in-intervall?
    | >      [min max body & desc]
    | >      `(let [min# ~min
    | >             max# ~max]
    | >         (test-driver (fn [] ~body)
    | >                      '~body
    | >                      (fn [] nil)   ; Don't need „expected result“.
    | >                      ~(first desc) ; Might be „nil“.
    | >                      (fn [expected# actual#]
    | >                        (<= min# actual# max#))
    | >                      (fn [expected# actual# result#]
    | >                        (diag (str „Expected:      “ actual#))
    | >                        (diag (str „to be between: “ min#))
    | >                        (diag (str „and:           “ max#))
    | >                        (diag (str „but was:       “ result#))))))
    | >
    | > => (in-intervall? 100 150 (flogiston-pressure) „flogiston pressure ok“)
    | > not ok 1 - flogiston pressure ok
    | > # Expected:      (flogiston-pressure)
    | > # to be between: 100
    | > # and:           150
    | > # but was:       58
    "
    [actual qactual exp desc pred diagnose]
    (if (= *mode* :skip)
      (print-result @current-test *mode* true *skip-reason*)
      (try
        (let [e (exp)
              a (actual)
              r (pred e a)]
          (print-result @current-test *mode* r desc)
          (when-not r
            (let [es (pr-str e)
                  as (pr-str qactual)
                  rs (pr-str a)]
              (diagnose es as rs)))
          a)
        (catch Exception e
          (print-result @current-test *mode* false desc)
          (diag (str "Exception was thrown: " e))
          `test-failed)
        (finally
          (dosync (commute current-test inc)))))))

(defmacro ok?
  "
  | Macro: ok?
  |
  | Simple yes/no test. <ok?> simply tests whether the given test evaluates to
  | true. In this nature it cannot give much of diagnostic information, but it
  | is sufficient for simple predicate tests.
  |
  | Example:
  |
  | > => (ok? (pressure-save? (flogiston-pressure)) „flogiston pressure is save“)
  | > not ok 1 - flogiston pressure is save
  | > # Expected: (pressure-save? (flogiston-pressure)) to be true
  "
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
  "
  | Macro: is?
  |
  | Comparison with „=“. <is?> does evaluate the given expected value and the
  | actual value. It compares both using „=“. If this fails, <is?> presents the
  | user some more diagnostics about why the test failed.
  |
  | Example:
  |
  | > => (is? (flogiston-pressure) *normal-flogiston-pressure*
  | >      „flogiston pressure is normal“)
  | > not ok 1 - „flogiston pressure is normal“
  | > # Expected: (flogiston-pressure)
  | > # to be:    125
  | > # but was:  58
  "
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
  "
  | Macro: isnt?
  |
  | Compare using „not=“. <isnt?> is similar to <is?>, but actually succeeds
  | when the actual is not equal to the expected value.
  "
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
  "
  | Macro: like?
  |
  | String checking with regular expressions. <like?> checks whether the given
  | string matches the supplied regular expression.
  "
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
  "
  | Macro: unlike?
  |
  | String checking with regular expressions. <unlike?> checks whether the given
  | string does *not* match the supplied regular expression.
  "
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
  "
  | Macro: throws?
  |
  | Check whether given the Exception is thrown. In case the supplied body runs
  | through without throwing an exception or if an Exception different from the
  | named is thrown, the test fails.
  "
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
  "
  | Macro: runs?
  |
  | The code does not throw an Exception. This is not really a test at all.
  | <runs?> just runs the supplied body and succeeds if body runs through
  | without throwing an Exception. It returns whatever the body returns.
  |
  | Example:
  |
  | > => (def flogiston-reactor (runs? (new FlogistonReactor)
  | >                             „created new flogiston reactor“))
  | > ok 1 - created new flogiston reactor
  "
  [body & desc]
  `(test-driver (fn [] ~body)
                (quote ~body)
                (fn [] nil)
                ~(first desc)
                (fn [e# a#] true)
                (fn [e# a# r#]
                  (diag (.concat "Expected " a#
                                 " to run through w/o exception.")))))
