# 1.0 features
1. More loops
   1. do while loop
   2. C style for loop
   3. foreach loop
2. Logging / debug so that we can debug functions that return strings
3. Syntax highlighting in Intellij
4. Hashes
5. Refs
6. Have a way to declare 'loose mode' (not strict) for whole file and per shell-string
    1. Maybe a 'guard' block where set +u/set -u is automatic (stop checking for unset)
    2. Or use default so checks for set work with -u in effect
7. String interpolation with $[]
   1. have bpr use arguments, arguments[all] (args/argv alias?)
8. Exceptions and raise statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)
9. Enforce 'readonly' 
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