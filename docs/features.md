# Features

## Code Nodes

We support function hoisting and multiple target shells with code nodes.

A Code Node is a (think linked list node) Java Object that represents a 'chunk' of code (e.g. a method declaration).
It is an abstraction of the user's intent.  As an example a HashMap CodeNode may translate to something very wierd in Bash3, which doesn't have HashMaps natively.