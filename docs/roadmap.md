# 1.0 features
1. Update intro docs to sell the idea 
   1. (abstract shell, awk and sed)
   2. Bash iceburg
   3. Floating point arithmatic as an example
2. Use bashpile.rb test section
3. Terraform + docker for multi-OS tests
4. Have a way to declare 'loose mode' (not strict) for whole file and per shell-string
5. Arrays, Hashes, Refs
6. Loops
7. String interpolation with $[]
   1. have bpr use arguments, arguments[all] (args/argv alias?)
8. Exceptions and raise statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)
9. Enforce 'readonly' 
   1. Currently on the honor system, has to be implemented by Bashpile, not by `declare` due to workaround

# Later
(Not ordered)
* 'Gnuize' the utilities used (ls?, sed, etc)
* getopts
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