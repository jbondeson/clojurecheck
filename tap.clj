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

(defmacro is? [actual exp & desc]
  (let [tmp_e (gensym "is__")
        tmp_a (gensym "is__")
        tmp_r (gensym "is__")
        tmp_es (gensym "is__")
        tmp_as (gensym "is__")
        tmp_rs (gensym "is__")]
    `(let [~tmp_e ~exp
           ~tmp_a ~actual
           ~tmp_r (= ~tmp_e ~tmp_a)]
       (if (nil? ~(first desc))
         (ok? ~tmp_r)
         (ok? ~tmp_r ~(first desc)))
       (when-not ~tmp_r
         (let [~tmp_es (print-str ~tmp_e)
               ~tmp_as (print-str (quote ~actual))
               ~tmp_rs (print-str ~tmp_a)]
           (diag (.concat "Expected: " ~tmp_as))
           (diag (.concat "to be:    " ~tmp_es))
           (diag (.concat "but was:  " ~tmp_rs)))))))

(defmacro isnt? [actual exp & desc]
  (let [tmp_e (gensym "is__")
        tmp_a (gensym "is__")
        tmp_r (gensym "is__")
        tmp_es (gensym "is__")
        tmp_as (gensym "is__")
        tmp_rs (gensym "is__")]
    `(let [~tmp_e ~exp
           ~tmp_a ~actual
           ~tmp_r (not= ~tmp_e ~tmp_a)]
       (if (nil? ~(first desc))
         (ok? ~tmp_r)
         (ok? ~tmp_r ~(first desc)))
       (when-not ~tmp_r
         (let [~tmp_es (print-str ~tmp_e)
               ~tmp_as (print-str (quote ~actual))
               ~tmp_rs (print-str ~tmp_a)]
           (diag (.concat "Expected:  " ~tmp_as))
           (diag (.concat "not to be: " ~tmp_es)))))))
