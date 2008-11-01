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

(defn plan
  "Print the test plan. Ie. the number of tests you intend to run. This gives
  the harness a chance to see, whether the tests ran completely. It is not
  strictly necessary to provide a plan. However it is strongly encouraged to
  do so.

  Example:

  | => (plan 10)
  | 1..10"
  [count]
  (. *the-harness* plan count))

(defn diag
  "Print diagnostics. Sometimes a test script wants to provide diagnostic
  information to the user. Eg. <is?> and friends provide information about
  the deviation from the expected outcome to the user. <diag> is a utility
  which takes away the burden of the special formating of such information
  from the test script.

  Example:

  | => (diag „flogiston pressure dropping rapidly“)
  | # flogiston pressure dropping rapidly"
  [msg]
  (. *the-harness* diag msg))

(defn bail-out
  "Bail out of the test process. Sometimes the system or the environment is so
  messed up, that further testing doesn't make sense. Then <bail-out> may be
  used to stop further testing immediately. Optionally a reason about the
  bailing out, may be given to provide to the user, why the testing stopped.

  Example:

  | => (bail-out)
  | Bail out!
  | => (bail-out „flogiston pressure too low“)
  | Bail out! flogiston pressure too low"
  ([]    (. *the-harness* bailOut nil))
  ([msg] (. *the-harness* bailOut msg)))

(defn- report-result
  [m t d]
  (. *the-harness* reportResult m t d))

(defn test-driver
  "Driver function for the tests. This function should only be called, when
  defining new test methods. The driver receives the actual form under test
  as a closure as well as it's quoted form. Similarly the expected value is
  transferred. The following description is optional and might be „nil“.
  Finally two callbacks to compare the actual result against the expected
  one and to print a diagnostic message in case of failure.

  In case an exception is thrown it is caught and reported via a diagnostic
  message to the user. The test fails in that case.

  Example:

  | => (defmethod is* 'in-intervall?
  |      [t desc]
  |      (let [[min max body] (rest t)]
  |        `(let [min# ~min
  |               max# ~max]
  |           (test-driver (fn [] ~body)
  |                        (quote ~body)
  |                        (fn [] nil)   ; Don't need „expected result“.
  |                        ~desc         ; Might be „nil“.
  |                        (fn [expected# actual#]
  |                          (<= min# actual# max#))
  |                        (fn [expected# actual# result#]
  |                          (diag (str „Expected:      “ actual#))
  |                          (diag (str „to be between: “ min#))
  |                          (diag (str „and:           “ max#))
  |                          (diag (str „but was:       “ result#)))))))
  |
  | => (is (in-intervall? 100 150 (flogiston-pressure)) „flogiston pressure ok“)
  | not ok 1 - flogiston pressure ok
  | # Expected:      (flogiston-pressure)
  | # to be between: 100
  | # and:           150
  | # but was:       58"
  [actual qactual exp desc pred diagnose]
  (if (= *mode* :skip)
    (report-result *mode* true *skip-reason*)
    (try
      (let [e (exp)
            a (actual)
            r (pred e a)]
        (report-result *mode* r desc)
        (when-not r
          (let [es (pr-str e)
                as (pr-str qactual)
                rs (pr-str a)]
            (diagnose es as rs))
          (when *fatal*
            (throw (new de.kotka.tap.FatalTestError))))
        a)
      (catch de.kotka.tap.FatalTestError e
        (throw e))
      (catch Exception e
        (report-result *mode* false desc)
        (diag (str "Exception was thrown: " e))
        (if *fatal*
          (throw (new de.kotka.tap.FatalTestError))
          `test-failed)))))
