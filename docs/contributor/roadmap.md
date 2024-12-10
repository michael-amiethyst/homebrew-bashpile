# 1.0 Roadmap
1. Hardening (fix TODOs and bugs, simplify)
2. Refactor deployment
   1. move generated files from /bin to /target/bin
   2. make executable directly (shebang java -jar, then the jar binary)
   3. skip external tools with flag for GHA free runner compatability (e.g. no shfmt)
3. Remove unwinder, create lib functions to disable/enable strict mode
4. Change architecture to create Bashpile AST (bast) and render to String after full context / metadata known
   1. e.g. stop parsing and re-parsing strings with regex
5. More loops
   1. do while loop
   2. C style for loop
   3. foreach loop
6. Logging / debug so that we can debug functions that return strings
7. Syntax highlighting in Intellij
8. Hashes
9. Refs
10. Have a way to declare 'loose mode' (not strict) for whole file and per shell-string
     1. Maybe a 'guard' block where set +u/set -u is automatic (stop checking for unset)
     2. Or use default so checks for set work with -u in effect
11. String interpolation with $[]
    1. have bpr use arguments, arguments[all] (args/argv alias?)
12. Exceptions and raise statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)
13. Enforce 'readonly' 
    1. Currently on the honor system, has to be implemented by Bashpile, not by `declare` due to workaround

# Fixes and improvements
* Near term
  * checkin pipeline in GitHub
  * builtin logging
  * better log messages in bpr (-v option)

# Later
(Not ordered)
* 'Gnuize' the utilities used (ls?, sed, etc)
* OOP (use / be inspired by bash infinity?) or at least structs
* Factor out text for future localization and consistent end-user feel.
* Regexes for Strings, files
* imports
* subshells, Bash `()`, Bashpile `&()`?
* default values for functions
* operator overloading for functions
* exponents, other operators
* commas in large values (e.g. 1,001)
* Script super-blocks / sections
* Scriptinos and easy testing
* `until`
* `expr AND expr` like Bash's `(expr;expr)` without the subshell (maybe)

# much later
* tooling (IDE integrations, more syntax highlighting)
* native run instead of just on bash

# maybe
* function overloading 
   * behind the scenes name the function name_returnType_arg1Type_arg2Type_...)
* reflections api