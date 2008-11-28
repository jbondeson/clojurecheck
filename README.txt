            ___________________________________            ______
            __  ____/__  /_____(_)_  ____/__  /_______________  /__
            _  /    __  /_____  /_  /    __  __ \  _ \  ___/_  //_/
            / /___  _  / ____  / / /___  _  / / /  __/ /__ _  ,<
            \____/  /_/  ___  /  \____/  /_/ /_/\___/\___/ /_/|_|
                         /___/

An implementation of the Test Anything Protocol. It is a simple protocol to
transfer information from tests to a harness, which in turn extracts the
information in various ways. The protocol itself is also human readable. It's
widely used in the Perl community.

By using TAP one separates the framework of running the tests from the
logic which displays the information of the test results. This could be
a console runner like Perl's „prove“ utility or a graphical program or
even a script controlling the automatic build process. The easy structure
of TAP makes it easy to implement parsers for it.

Meikel Brandmeyer <mb@kotka.de>
Frankfurt am Main, 2008
