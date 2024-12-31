# 1.0 Roadmap
1. Remove preambles concept
2. Convert Translation to a tree structure instead of a string concat model
3. Change Translation payload from String (body) to bastData (that renders to string)
4. Create lib functions to disable/enable strict mode
   1. Install to /etc/bashpile folder
5. Integrate imports into the type system
6. More loops
   1. do while loop
   2. C style for loop
   3. foreach loop
7. Logging / debug so that we can debug functions that return strings
8. Syntax highlighting in Intellij
9. Hashes
10. Refs
11. String interpolation with $[]
    1. have bpr use arguments, arguments[all] (args/argv alias?)
12. Exceptions and raise/throw statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)
    1. finally blocks
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