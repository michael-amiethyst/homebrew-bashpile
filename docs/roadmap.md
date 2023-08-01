# MVP features / drink my own champaign

# Implement testrig.bp
1. Update wiki, docs (and maybe create videos) for end-users

# Enhance testrig.bp
1. String interpolation with $[]
    1. include special __OPTIONS[] array for $@, $1, etc.
2. conditionals (if-elseif-else), comparison operators (<, =>, etc) and boolean logic
3. Factor out text for future localization and consistent end-user feel.

# Later
(Not ordered)
* remaining types (array, map, ref)
* imports
* subshells, Bash `()`, Bashpile `&()`?
* default values for functions
* operator overloading for functions
* exponents, other operators
* commas in large values (e.g. 1,001)
* Script super-blocks / sections
* Caching generated Bash
  * check generated Bash with ShellCheck (https://www.shellcheck.net/)
* Scriptinos and easy testing
* `until`
* `unless`
* `expr AND expr` like Bash's `(expr;expr)` without the subshell (maybe)
* loops

# much later
* tooling (IDE integrations, code highlighting)
* native run instead of just on bash

# maybe
* function overloading 
   * behind the scenes name the function name_returnType_arg1Type_arg2Type_...)
* reflections api
* objects