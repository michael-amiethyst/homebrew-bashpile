# 1.0 features
1. Change bpt output from `export` to `declare -x` (and type)
2. Arrays, Hashes, Refs
3. Loops
4. String interpolation with $[]
5. Exceptions and raise statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)

# Later
(Not ordered)
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