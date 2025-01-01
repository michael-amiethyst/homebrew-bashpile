# 1.0 Roadmap
1. Change Translation payload from String (body) to bastData (that renders to string)
2. Create lib functions to disable/enable strict mode
   1. Install to /etc/bashpile folder
3. Integrate imports into the type system
4. More loops
   1. do while loop
   2. C style for loop
   3. foreach loop
5. Logging / debug so that we can debug functions that return strings
6. Syntax highlighting in Intellij
7. Hashes
8. Refs
9. String interpolation with $[]
   1. have bpr use arguments, arguments[all] (args/argv alias?)
10. Exceptions and raise/throw statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)
    1. finally blocks
11. Enforce 'readonly' 
    1. Currently on the honor system, has to be implemented by Bashpile, not by `declare` due to workaround
12. Improve runtime
    1. Caching in /etc?
    2. Use graalvm?

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
* Handle colorized text
* subshells, Bash `()`, Bashpile `&()`?
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