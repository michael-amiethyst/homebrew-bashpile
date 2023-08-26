# 1.0 features
1. Redesign - get rid of level-counter and most other state.  Use translationMetadata and translation API instead in parent node
2. Change bpt output from `export` to `declare -x` (and type)
3. Arrays, Hashes, Refs
4. Loops
5. String interpolation with $[]
6. Exceptions and raise statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)

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