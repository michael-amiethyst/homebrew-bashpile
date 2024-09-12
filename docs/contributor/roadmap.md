# 1.0 features
1. increment / decrement
2. Remove `creates` statement in favor of tempfile (mktemp on Fedora) calls
3. Loops
   1. do while
   2. for
   3. foreach
4. Hashes
5. Refs
6. Have a way to declare 'loose mode' (not strict) for whole file and per shell-string
7. String interpolation with $[]
   1. have bpr use arguments, arguments[all] (args/argv alias?)
8. Exceptions and raise statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)
9. Enforce 'readonly' 
   1. Currently on the honor system, has to be implemented by Bashpile, not by `declare` due to workaround

# Fixes and improvements
* Near term
  * checkin pipeline in GitHub
  * builtin logging
  * better log messages in bpr
* LATER
  * change to some Kotlin, especially in BashTranslationEngine and helper
  * remove dev on WSL

# Later
(Not ordered)
* 'Gnuize' the utilities used (ls?, sed, etc)
* Implement int / float (built-in or `expr` vs `bc`)
* OOP (use / be inspired by bash infinity?)
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
* `unless`
* `expr AND expr` like Bash's `(expr;expr)` without the subshell (maybe)
* typecasts on IDs and function calls - implemented in the translated output

# much later
* tooling (IDE integrations, code highlighting)
* native run instead of just on bash

# maybe
* function overloading 
   * behind the scenes name the function name_returnType_arg1Type_arg2Type_...)
* reflections api