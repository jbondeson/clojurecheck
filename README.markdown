# ClojureCheck – Property based testing for Clojure

*ClojureCheck* is an addon for `clojure.test`. It adds property based
testing to *clojure.test* following the lines of [*QuickCheck*][qc] for
Haskell.

## Properties

Testing with *ClojureCheck* is done via so-called properties. A property
consists of a set of bindings, which vary between trials, and „normal“
`clojure.test` assertions. The assertions are run several times. Each
time the defined locals are bound to new values generated automatically
by *ClojureCheck*.

## Example

    (ns my.package
      (:use clojure.test)
      (:require [clojurecheck.core :as cc]))

    (defn angular-diff
      [a b]
      (-> (- a b) (mod 180) Math/abs))

    (deftest angular-diff-standard-test
      (are [x y] (= x y)
        (angular-diff   0   0) 0
        (angular-diff  90  90) 0
        (angular-diff   0  45) 45
        (angular-diff  45   0) 45
        (angular-diff   0 270) 90
        (angular-diff (* 360 2) (+ (* 360 4) 23)) 23))

    (deftest angular-diff-property
      (cc/property "angular-diff is smallest angel between a and b"
        [diff (cc/float :lower -180.0 :upper 180.0)
         a    (cc/float :lower 0.0 :upper 360.0)
         n    (cc/int)]
        (let [b (+ a (* 360 n) diff)]
          (is (= (angular-diff a b) (Math/abs diff))))))

## Installation

To use *ClojureCheck* add it to your project dependencies. lein, maven,
gradle or ivy can then fetch it from clojars.

For lein:

    :dev-dependencies [[clojurecheck 1.0.0]]

For gradle:

    dependencies { testCompile 'clojurecheck:clojurecheck:1.0.0' }

## Contact

I always appreciate feedback. Please report bugs on the tracker at
bitbucket: <http://bitbucket.org/kotarak/clojurecheck/issues>. Also
improvement ideas etc. are always welcome!

Meikel Brandmeyer <mb@kotka.de>
Erlensee, Germany, 2010
