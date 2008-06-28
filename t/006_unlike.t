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

in="$0.in"
out="$0.out"
exp="$0.exp"

counter=1

print_result() {
	if [ $1 -ne 0 ]; then
		printf "not "
	fi
	printf "ok %d - %s\n" $counter "$2"
	counter=`expr $counter + 1`
}

try() {
	cat >$in <<EOF
(load-file "tap.clj")
(clojure/refer 'tap)
$1
EOF

	cat >$exp <<EOF
$2
EOF

	java clojure.lang.Script $in >$out
	cmp -s $exp $out
	print_result $? "$3"
}

cleanup() {
	rm -f $in $out $exp
}

trap cleanup 2 3 5 6 9 15

echo 1..4

try '(unlike? (.concat "foo" "bar") #"xx")' "ok 1" "true w/o description"
try '(unlike? (.concat "foo" "bar") #"o+b")' 'not ok 1
# Expected:     (.concat "foo" "bar")
# not to match: o+b
# string was:   "foobar"' "false w/o description"
try '(unlike? (.concat "foo" "bar") #"xx" "success")' "ok 1 - success" \
    "true w/ description"
try '(unlike? (.concat "foo" "bar") #"o+b" "failure")' 'not ok 1 - failure
# Expected:     (.concat "foo" "bar")
# not to match: o+b
# string was:   "foobar"' "false w/ description"

cleanup

# vim:ft=sh: