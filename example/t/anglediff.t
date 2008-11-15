#(comment
exec("java", "clojure.lang.Script", $0) or die "Cannot exec Java!";
__END__
)
(clojure.core/ns com.example.anglediff.test
  (:use
     com.example.anglediff
     [de.kotka.clojurecheck :only (is let-gen holds? for-all)])
  (:require
     [de.kotka.clojurecheck :as cc]))

(cc/plan 7)

(is (= (anglediff 0 0) 0) "zero at zero")
(is (= (anglediff 90 90) 0) "zero at 90")

(is (= (anglediff 0 45) 45) "0,45 => 45")
(is (= (anglediff 45 0) 45) "45,0 => 45")

(is (= (anglediff 0 270) 90) "0,270 => 90")

(let [a (* 2 360)
      b (* 4 360)]
  (is (= (anglediff a (+ b 23)) 23) (str a "," b "+23 => 23")))

(holds?
  (for-all [a Integer
            n Integer
            d [Integer -180 180]]
    (let [b (+ a (* n 360) d)]
      (is (= (anglediff a b) (Math/abs d)))))
  "anglediff satifies definition of angular diff")

; vim:ft=clojure:
