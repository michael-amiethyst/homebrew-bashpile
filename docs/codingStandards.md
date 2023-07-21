# Coding Standards

In general we're using the Intellij default Java ("IDE") coding standards and default Java conventions.

The exceptions are mentioned below:

## Class Structure
Java's is officially
* Static Fields
* Instance Fields
* Constructors
* Methods

Ours is
* Static Fields
* Most Static Methods (besides kinds listed below)
* Instance Fields
* Static initializers (i.e. static methods that act like constructors)
* Constructors
* Instance Methods and static helpers
* Nested classes

## Trinary Operator
If the form is `object = test ? object.fluntInterfaceCall() : object` then the fluent interface call will always
be the first choice.  If the line is too long it will be broken up like
```java
object = test
        ? firstChoice
        : secondChoice;
// or
objectOnAReallyLongLine =
        test
                ? firstChoice
                : secondChoice;
```

## Long Complex Strings
May be broken up by line.  As an example from BashTranslationEngine.assignmentStatement:
```java
final String body = lineComment + "\n"
        + unnestedText    // includes newline
        + subcomment // includes newline
        + getLocalText() + variableName + "\n"
        + assignment; // includes newline
```

## Multiple Short Annotations
Many tests have `@Test @Order(num)` on one line.