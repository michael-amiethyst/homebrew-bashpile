======================
The Bashpile Tutorial
======================

From simple to complex commands:

C / Java like Comments
======================
// and ``/* */`` supported.  BashpileDoc (like JavaDoc) ``/** */`` also supported.

Print Statements
================
Python like print statements:
::

    print("Hello World")

Typed Declarations
==================
With compile time type-checking, like TypeScript
::

    x: int = 0
    print(x + 1)

    // floats too!
    y: float = .5
    print(x + y)

Conditionals
============
If statements with modifiers:
::

    if fileExists "path":
        echo "May be a directory"
    // or
    if regularFileExists "path":
        echo "A normal file like -f only"
    // if directoryExists also supported

    // also for arguments to the program
    if isset arguments[1] /* like $1 */:
        print("true")
    else:
        print("false")
    // unset also supported

    // also isEmpty and isNotEmpty for strings

Switches and Options with getopt
================================

Functions and Anonymous Blocks
==============================
With Python like indents:
::

    function squared(input: int): int
        return input * input
    // with lexical scoping
    print(input) // error!

Lists
=====
More like Kotlin than anything else:
::

    strList: list<str> = listOf("zero", "one", "two", "three")
    strList += "four"
    strList += listOf("five", "six")
    print(strList)
    print(strList[3]) // prints three

While Loops
===========
For loops, foreach loops and do-while loops on the roadmap_.
::

    i: int = 0
    while i < 5:
        print(i)
        i = i + 1

.. _roadmap: https://github.com/michael-amiethyst/homebrew-bashpile/blob/0.21.8/docs/contributor/roadmap.md

Shell Strings
=============
You can drop into Bash with #(Raw Bash here):
::

    print(#(ls))
    // or just a straight bash line
    ls
    #(
        echo
        echo "Multiline bash blocks too"
        echo
    )

Creates Statements
==================
Like Java try-with-resources:
::

    #(ls > ls.log) creates "ls.log":
        print("ls.log automatically deleted after the block ends")
    cat ls.log # Bash style comment needed, file not found