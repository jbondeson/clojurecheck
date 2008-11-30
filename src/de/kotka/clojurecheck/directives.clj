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

(defvar- *mode*        :normal)
(defvar- *skip-reason* :none)

(defn todo*
  "This is the driver for the <todo> macro. This function should not
  be called directly."
  [body]
  (binding [*mode* :todo]
    (body)))

(defmacro todo
  "Marking unfinished functionality. Wrapping tests in a <todo> call marks the
  tests with the TODO directive. This information might be used by the
  harness to provide further hints to the user. Perl's „prove“ utility
  considers TODO tests to be irrelevant to the whole result of the test
  script. They should fail however. Succeeding TODO tests are reported by
  „prove“ in a special way, giving a hint that the TODO status of the test
  should be revised.

  Example:

  | => (todo
  |      (ok? (taken-over-the-world) „take over the world“))
  | not ok 1 # TODO take over the world"
  [& body]
  `(todo* (fn [] ~@body)))

(defn skip*
  "This is the driver for the <skip> macro. This function should not
  be called directly."
  [reason body]
  (binding [*mode*        :skip
            *skip-reason* reason]
     (body)))

(defmacro skip
  "Skip certain tests. Sometimes certain functionality is disabled, eg. when
  it is not applicable to the platform currently running on or when the
  functionality is disabled on purpose. The tests wrapped in the <skip> call
  are actually not run at all, but reported to succeed and marked with the
  SKIP directive and the given reason.

  Note:

  Code between tests *is* run!

  Example:

  | => (skip „frobnicator library not available“
  |      (ok? (frobnicator/do-frobnicate foo) „foo is frobnicatable“))
  | ok 1 # SKIP frobnicator library not available

  The call to „do-frobnicate“ is actually not done. The test is always
  reported to succeed marked with the SKIP directive and the reason, why the
  test was skipped."
  [reason & body]
  `(skip* ~reason (fn [] ~@body)))

(defn skip-if*
  "This is the driver for the <skip-if> macro. This function should not
  be called directly."
  [t reason body]
  (if t
    (skip* reason body)
    (body)))

(defmacro skip-if
  "Conditionally skip tests. In case the guard tests evaluates to „true“ the
  given tests are run in <skip> call with the given reason. Otherwise the
  tests are run normally.

  Example:

  | => (skip-if (< (flogiston-pressure) 100) „flogiston pressure too low“
  |      (ok? (inject-flogiston) „flogiston injection works“))"
  [t reason & body]
  `(skip-if* ~t ~reason (fn [] ~@body)))

(defvar- *fatal* false)

(gen-class
  :name    de.kotka.clojurecheck.FatalTestError
  :extends Exception)

(import '(de.kotka.clojurecheck FatalTestError))

(defn fatal*
  "Executes the thunk in fatal context. That is a failing test will
  abort the thunk immediately. See also „fatal“ macro."
  [thunk]
  (binding [*fatal* true]
    (try
      (thunk)
      (catch FatalTestError e `test-failed))))

(defmacro fatal
  "Abort on failing tests. In case one has several tests, which depend on
  each other, one can specify a fatal block around the tests in question.
  Should a test fail, the rest of the tests of the block are skipped.

  Example:

  | => (fatal
  |      (ok? (save-flogistion-pressure?) „flogiston pressure is save“)
  |      (is? (open-reactor-door) :opened „reactor door opened“))
  | not ok 1 - flogiston pressure is save

  Note: the second test is not executed!"
  [& body]
  `(fatal* (fn [] ~@body)))
