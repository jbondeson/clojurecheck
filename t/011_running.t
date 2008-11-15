#! /bin/sh
#-
# Copyright 2008 (c) Meikel Brandmeyer.
# All rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

. t/testlib.sh

echo 1..4

try '(is (= (is (running? 1)) 1))' 'ok 1
ok 2' 'success w/o message'
try '(is (= (is (running? 1) "success") 1))' 'ok 1 - success
ok 2' 'success w/ message'

try '(is (= (is (running? (do (throw (new Exception "x")) "reached")))
            (symbol "de.kotka.clojurecheck" "test-failed")))' 'not ok 1
# Exception was thrown: java.lang.Exception: x
ok 2' 'failure w/o message'
try '(is (= (is (running? (do (throw (new Exception "x")) "reached")) "failure")
            (symbol "de.kotka.clojurecheck" "test-failed")))' \
'not ok 1 - failure
# Exception was thrown: java.lang.Exception: x
ok 2' 'failure w/ message'

cleanup

# vim:ft=sh:
