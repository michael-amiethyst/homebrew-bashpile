# First -- High Risk features

1. check generated Bash with ShellCheck (https://www.shellcheck.net/)

# Next MVP features / drink my own champaign
Implement testrig.bp
1. Comments in Bashpile
2. command objects $()
   1. run as subshell or substitution
   2. support nesting
   3. optional xargs style {}'s
3. try-with-resources
   1. syntax: try ($(command) creates "filename")
4. Automatic script line detection (e.g. can write 'echo ...' or any other bash command)
5. Bash Blocks (see XML Island Detection in book)

# Later
remaining types (array, map, ref)
imports
default values for functions
exponents, other operators
Script super-blocks / sections
Scriptinos and caching, easy testing
`until`
Logical operators, comparison operators
conditionals
loops
reflections api?
objects?

# much later
tooling (IDE integrations, code highlighting)
native run instead of just on bash

# maybe
1. function overloading 
   1. (behind the scenes name the function name_returnType_arg1Type_arg2Type_...)