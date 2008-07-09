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

echo 1..6

try '(skip "for test" (ok? (do (println "FAILED") true)))' \
	"ok 1 # SKIP - for test" "true SKIP w/o description"
try '(skip "for test" (ok? (do (println "FAILED") false)))' \
	'ok 1 # SKIP - for test' "false SKIP w/o description"
try '(skip "for test" (ok? (do (println "FAILED") true) "success"))' \
	"ok 1 # SKIP - for test" "true SKIP w/ description"
try '(skip "for test" (ok? (do (println "FAILED") false) "failure"))' \
	'ok 1 # SKIP - for test' "false SKIP w/ description"

try '(skip-if true "for test" (ok? (do (println "FAILED") false) "failure"))' \
	'ok 1 # SKIP - for test' "true skip-if w/ description"
try '(skip-if false "for test" (ok? (do (println "OK") true) "success"))' \
	'OK
ok 1 - success' "false skip-if w/ description"

cleanup

# vim:ft=sh: