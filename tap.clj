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

(in-ns 'tap)
(clojure/refer 'clojure)

(def counter (ref 1))

(defn plan [count]
  (print "1..")
  (print count)
  (newline))

(defn diag [msg]
  (doseq l (.split msg "\n")
    (print "# ")
    (print l)
    (newline)))

(defn ok? [t & desc]
  (if t
    (print "ok ")
    (print "not ok "))
  (dosync
    (print @counter)
    (commute counter + 1))
  (when desc
    (print " - ")
    (print (first desc)))
  (newline))

(defn- ok-driver [actual exp desc pred diagnose]
  (let [tmp_e (gensym "tap__")
        tmp_a (gensym "tap__")
        tmp_r (gensym "tap__")
        tmp_es (gensym "tap__")
        tmp_as (gensym "tap__")
        tmp_rs (gensym "tap__")]
    `(let [~tmp_e ~exp
           ~tmp_a ~actual
           ~tmp_r (~pred ~tmp_e ~tmp_a)]
       (if (nil? ~desc)
         (ok? ~tmp_r)
         (ok? ~tmp_r ~desc))
       (when-not ~tmp_r
         (let [~tmp_es (pr-str ~tmp_e)
               ~tmp_as (pr-str (quote ~actual))
               ~tmp_rs (pr-str ~tmp_a)]
           (~diagnose ~tmp_es ~tmp_as ~tmp_rs))))))

(defmacro is? [actual exp & desc]
  (ok-driver actual exp (first desc)
             #(= %1 %2)
             (fn [e a r]
               (diag (.concat "Expected: " a))
               (diag (.concat "to be:    " e))
               (diag (.concat "but was:  " r)))))

(defmacro isnt? [actual exp & desc]
  (ok-driver actual exp (first desc)
             #(not= %1 %2)
             (fn [e a r]
               (diag (.concat "Expected:  " a))
               (diag (.concat "not to be: " e)))))

(defmacro like? [actual exp & desc]
  (ok-driver actual exp (first desc)
             (fn [e a] (not (nil? (re-find e a))))
             (fn [e a r]
                (diag (.concat "Expected: " a))
                (diag (.concat "to match: " e)))))

(defmacro unlike? [actual exp & desc]
  (ok-driver actual exp (first desc)
             (fn [e a] (nil? (re-find e a)))
             (fn [e a r]
                (diag (.concat "Expected:     " a))
                (diag (.concat "not to match: " e))
                (diag (.concat "string was:   " r)))))
