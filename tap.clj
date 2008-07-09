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

(clojure/in-ns 'tap)
(clojure/refer 'clojure)

(defn plan [count]
  (print "1..")
  (print count)
  (newline)
  (flush))

(defn diag [msg]
  (doseq l (.split msg "\n")
    (print "# ")
    (print l)
    (newline)
    (flush)))

(defn bail-out [& msg]
  (print "Bail out!")
  (when-not (nil? msg)
    (print " ")
    (print (first msg)))
  (newline)
  (flush)
  (.exit java.lang.System 1))

(def mode :normal)
(def skip-reason :none)

(defn todo* [body]
  (binding [mode :todo]
    (body)))

(defmacro todo [& body]
  `(todo* (fn [] ~@body)))

(defn skip* [reason body]
  (binding [mode :skip
            skip-reason reason]
     (body)))

(defmacro skip [reason & body]
  `(skip* ~reason (fn [] ~@body)))

(defn skip-if* [t reason body]
  (if t
    (skip* reason body)
    (body)))

(defmacro skip-if [t reason & body]
  `(skip-if* ~t ~reason (fn [] ~@body)))

(def test-agent (agent 1))

(defn print-result [c m t desc]
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
  (newline))

(defn test-driver [actual qactual exp desc pred diagnose]
  (await
    (send test-agent
          (fn [c m sr]
            (if (= m :skip)
              (print-result c m true sr)
              (try
                (let [e (exp)
                      a (actual)
                      r (pred e a)]
                  (print-result c m r desc)
                  (when-not r
                    (let [es (pr-str e)
                          as (pr-str qactual)
                          rs (pr-str a)]
                      (diagnose es as rs))))
                (catch Exception e
                  (print-result c m false desc)
                  (diag (str "Exception was thrown: " e)))))
            (flush)
            (+ c 1))
          mode
          skip-reason)))

(defmacro ok? [t & desc]
  `(test-driver (fn [] ~t)
                (quote ~t)
                (fn [] nil)
                ~(first desc)
                (fn [e# a#] a#)
                (fn [e# a# r#]
                  (diag (.. "Expected: "
                            (concat a#)
                            (concat " to be true"))))))

(defmacro is? [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (= e# a#))
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to be:    " e#))
                  (diag (.concat "but was:  " r#)))))

(defmacro isnt? [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (not= e# a#))
                (fn [e# a# r#]
                  (diag (.concat "Expected:  " a#))
                  (diag (.concat "not to be: " e#)))))

(defmacro like? [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (not (nil? (re-find e# a#))))
                (fn [e# a# r#]
                  (diag (.concat "Expected: " a#))
                  (diag (.concat "to match: " e#)))))

(defmacro unlike? [actual exp & desc]
  `(test-driver (fn [] ~actual)
                (quote ~actual)
                (fn [] ~exp)
                ~(first desc)
                (fn [e# a#] (nil? (re-find e# a#)))
                (fn [e# a# r#]
                  (diag (.concat "Expected:     " a#))
                  (diag (.concat "not to match: " e#))
                  (diag (.concat "string was:   " r#)))))
