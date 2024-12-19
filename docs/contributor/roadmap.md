# 1.0 Roadmap
1. Create lib functions to disable/enable strict mode
   1. Rename stdlib to bashpile-stdlib
   2. Install to /etc/bashpile folder
2. Move generated docs to /target, find a place to host
3. Change architecture to create Bashpile AST (bast) and render to String after full context / metadata known
   1. e.g. stop parsing and re-parsing strings with regex
4. Imports
   1. Especially function return values into the type system
5. More loops
   1. do while loop
   2. C style for loop
   3. foreach loop
6. Logging / debug so that we can debug functions that return strings
7. Syntax highlighting in Intellij
8. Hashes
9. Refs
10. String interpolation with $[]
    1. have bpr use arguments, arguments[all] (args/argv alias?)
11. Exceptions and raise/throw statements (see ConditionalsBashpileTest.ifWithInlineCanRaiseError)
    1. finally blocks
12. Enforce 'readonly' 
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