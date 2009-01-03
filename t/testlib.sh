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
(clojure.core/ns de.kotka.clojurecheck.tests
  (:use de.kotka.clojurecheck))
$1
(. java.lang.System exit 0)
EOF

	cat >$exp <<EOF
$2
EOF

	java clojure.main $in >$out
	cmp -s $exp $out
	print_result $? "$3"
}

cleanup() {
	rm -f $in $out $exp
}

trap cleanup 2 3 5 6 9 15

# vim:ft=sh:
