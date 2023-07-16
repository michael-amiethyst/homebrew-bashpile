# MVP features / drink my own champaign
Implement testrig.bp
1. command substitution with $()
2. String interpolation with $[]
   1. include special __OPTIONS[] array for $@, $1, etc.
3. try-with-resources
   1. syntax: #(command) creates "filename":
4. Automatic script line detection (e.g. can write 'echo ...' or any other bash command)
5. Bash Blocks (see XML Island Detection in book)

# Later
(Not ordered)
* remaining types (array, map, ref)
* imports
* default values for functions
* exponents, other operators
* commas in large values (e.g. 1,001)
* Script super-blocks / sections
* Caching generated Bash
  * check generated Bash with ShellCheck (https://www.shellcheck.net/)
* Scriptinos and easy testing
* `until`
* Logical operators, comparison operators
  * `&&` like Java
  * `expr AND expr` like Bash's `(expr;expr)` without the subshell
* conditionals
* loops

# much later
* tooling (IDE integrations, code highlighting)
* native run instead of just on bash

# maybe
* function overloading 
   * behind the scenes name the function name_returnType_arg1Type_arg2Type_...)
* reflections api
* objects