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

## Instance Order in BashpileVisitor, TranslationEngine and TranslationEngine classes

They will follow the order in which they are defined in BashpileParser.g4.

## Trinary Operator
If the form is `object = test ? object.fluntInterfaceCall() : object` then the fluent interface call will always
be the first choice.  

Similarly, if the form is `object = notNullOrNotEmptyTest ? work : default` then the default will be the second choice.

If the line is too long it will be broken up like
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

## NonNull, Nullable and final
All fields and params will be annotated with `@NonNull` or `@Nullable` and marked as final as appropriate.
Final object references should not be mutated.