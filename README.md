# Expect4J

## Copyright

Copyright (c) 2007 Justin Ryan

Copyright (c) 2013 Chris Verges <chris.verges@gmail.com>

## Licensing

Licensed under the terms of the Apache Public License v2.0.

## Overview

A powerful feature of Tcl has always been its integration with the
Expect library (http://expect.nist.gov). As Tcl has been ported to the
Java platform (http://tcljava.sourceforge.net/) certain C-based
libraries have been left behind. Expect4j is an attempt to rewrite
Expect in Java and provide bindings to the TclJava interpreter. The goal
is to be able to port existing Tcl code bases that use Expect directly
to Java without changes. The current version has successfully run a
10,000 line Tcl script which heavily depends on Expect for it operation.

Expect is the kitchen sink of IO control. It supports control of
processes and sockets, and a complex method of match multiple patterns
at the same time. These are needed in some applications, but it
complicates the API a tad. Especially when it used in Java which doesn't
support closures natively. There are other libraries which offer a more
concise API, e.g. enchanter (http://code.google.com/p/enchanter/).
enchanter is a very good library and it is highly recommended for
automating ssh/telnet sessions. But when Expect syntax is needed,
Expect4j is the way to go.

Excellent article on using expect4j:
http://nikunjp.wordpress.com/2011/07/30/remote-ssh-using-jsch-with-expect4j/
