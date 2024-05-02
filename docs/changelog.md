* 0.1.0 - Initial checkins.  Simple grammar, JUnit5 tests, Log4j2 logging, Bash output, *nix command line class
* 0.2.0 - Factored out translation to script language to support multiple shells
* 0.3.0 - Anonymous blocks, lexical scoping, floats
* 0.4.0 - Functions, tags
* 0.5.0 - Function forward declarations, line number comments in translated Bash
* 0.6.0 - Single line comments, Multiline Comments, BashpileDocs (like JavaDocs)
* 0.7.0 - Shell Strings, Inlines (Bash Command Substitutions)
* 0.8.0 - Create statement
* 0.9.0 - Easy running from the command line ("Drink your own champaign"/dogfooding)
* 0.10.0 - Support deployment with brew
* 0.11.0 - Conditionals, comparison operators and boolean logic
* 0.11.1 - Fix for `brew install`
* 0.11.2 - Permissions fix for OSX
* 0.12.0 - Redesign of the logic, favor Translations with flags over checking state/context
* 0.12.1 - bpr.bps uses POSIX compliant `tail` options
* 0.13.0 - Reduced need for #() shell string syntax
* 0.14.0 - Using the Bash type system with `declare` instead of `export`
* 0.15.0 - Changed function syntax
* 0.16.0 - Equality and comparison Operators
* 0.17.0 - Implemented `bpr -c <COMMAND>` for running from command line directly.
    Also implemented many primaries, else-if clauses and combining expressions.  Also bugfixes.
* 0.18.0 - Implemented Bashpile Brew test section, started Docker testing
* 0.19.0 - Lists
* 0.20.0 - while loops
* 0.21.0 - switch/case statements
* 0.21.1 - Getopt switch fix
* 0.21.2 - If regularFileExists (-f), directoryExists (-d).  Fixed a few TODOs, Kotlin integration
