# Features

## Whitespace ignoring

The Bash command `export var = "value"` fails but a Bashpile assign can be `var = value`.

## Easy Calculations

You can use floating point math easily as well as parenthesis.  
The *nix command `bc` is used under the hood.

## Automatic 'Strict Mode'
We add a `set -euo pipefail` at the top of the generated Bash script for easy debugging.
See [Bash Strict Mode](http://redsymbol.net/articles/unofficial-bash-strict-mode/).

## Multiple Shell Support
Bashpile is written to be extensible with different target script languages, not just Bash 5.

## Anonymous Blocks and automatic Lexcical Scoping

With a simple
```
block
    pythonLikeBlocks = 1.0
```
you can declare an anonymous block and all declarations within will be lexically scoped
(undefined outside of the block).