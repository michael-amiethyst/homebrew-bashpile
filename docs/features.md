# Features

At a high level Bashpile provides a modern Python like syntax with some ideas from Java.
The language compiles (transpiles) to Bash version 5 so it can run on virtually all Linux distros.
These features are also documented in the [wiki](https://github.com/designatevoid/bashpile/wiki).

We will go into details from simple language features like Types to complex ones like Functions.

## Whitespace ignoring

The Bash command `export var = "value"` fails but a Bashpile assign can be `var = "value"`.  Bashpile adds in `export`
or `local` keywords as needed behind the scenes.  E.g.

```
x: int = 42
_hello: str = "world"
```
The `int` and `str` are types.

## Strong Typing

We have strong tying where it makes sense.  We have an "unknown" type that matches everything when the type isn't
specified and can't be determined at compile time.  We have type casting (see below) as well.

## Comments and Documentation Blocks

```
// Comments are supported
/*
    Multiline comments
    are supported.
*/

/**
 * BashpileDocs are like JavaDocs.
 * They are treated like comments for now.
*/
```

## Print()
You can use an easy `print()` line.  E.g.

```
print(42)
action: str = "To boldly go"
print(action)
```

## Easy Calculations

You can use floating point math easily as well as parenthesis.  
The Linux command `bc` is used under the hood.  E.g. `print(3.14 * (1 + 2))`

## Automatic 'Strict Mode'
We add a `set -euo pipefail` at the top of the generated Bash script for easy debugging.
See [Bash Strict Mode](http://redsymbol.net/articles/unofficial-bash-strict-mode/).

## Multiple Shell Support
Bashpile is written to be extensible with different target script languages in the future, not just Bash 5.

## Anonymous Blocks and automatic Lexical Scoping

With a simple
```
block:
    pythonLikeBlocks: float = 1.0
```
you can declare an anonymous block and all declarations within will be lexically scoped
(undefined outside the block).

## Simplified Functions

In bashpile you can return strings from functions and don't need to worry about
the shell silently suppressing silly sub-shell slip-ups, circumventing superior string subroutines simply.

Without the alliteration we handle the portability of the syntax, subshell complexities and exit code quirks of Bash.

### Tags for functions and anonymous blocks

You can tag functions with the syntax

```
function myFunction: int(myFloat: float, myString: string) ["tag1" "tag2" "etc"]:
    ...
```

future tooling will pick up on these embedded comments.  
Tag functions for a given data flow, a layer or whatever you can think of!

### Function forward declarations

In Bash, you need to declare a (helper) function before you can use it.  This leads to constructions like:

```
helperFunction() {
    ... $1 ... $2 ...
}

someLargeFunction() {
    ...
    helperFunction $x $y
}
```

Now you can declare a function beforehand and implement it where it makes sense:
```
function helperFunction: int (arg1: int, arg2: int)

function someLargeFunction() ["mainFlow"]:
   ...
   helperFunction(x, y)
   
function helperFunction: int (arg1: int, arg2: int):
    ... arg1 ... arg2
    ...
print("args are also lexically scoped and undefined here")
```

This helps let the code flow more naturally from top to bottom.

## Shell Strings

Use `#(command)` syntax to pass the command directly to the shell.  
This is similar to how in C/C++ you can "drop into" assembly.  E.g. `#(pwd)`.

## Inlines (Command Substitutions)

A `$(command)` syntax works just as you would expect in Bash and also has you drop into Bash directly.

```
myFilename: str = "/tmp/tmp.txt"
fileContents: str = $(cat $myFilename)
// you can nest this syntax too
slurpString: str = $(cat $(echo $myFilename))
```

Note that in Bash version 5 you can't nest command substitutions directly.  Bashpile takes care of any workaround.

## Type Casting

At compile time the type of a Shell String or Inline can't be determined.  You can provide the type with a similar
syntax to declaring a variable.

```
fileContents: str = $(cat $filename)
// you can nest this syntax too
#(export filename=/tmp/tmp.txt)
slurpString: str = "Temporary file contents: " + $(cat $(echo $filename)): str
```

## Create statements

Similar to Java's try-with-resources you can declare a block like:
```
#(command) creates "filename":
    slurpString: str = #(cat $filename)
// filename will be deleted from the filesystem here

// if you figure out the filename from the command you can assign the result and use it immediately
// as an example
log: str = #(
    filename=$(printf "%d.txt" $$)
    printf "%s" "$filename" > "$filename"
    printf "%s" "$filename"
) creates log:
    #(cat "$log")
```

## Easy Running

You can either use Bashpile from the Shebang line or run `bashpile -inputFile=FILE transpile > BASH_SCRIPT`.
