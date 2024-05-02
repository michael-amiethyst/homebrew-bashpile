lexer grammar BashpileLexer;

tokens { INDENT, DEDENT }

@lexer::header {
  import com.yuvalshavit.antlr4.DenterHelper;
  import com.bashpile.Lexers;
}

@lexer::members {
  private final DenterHelper denter = DenterHelper.builder()
    .nl(Newline)
    .indent(BashpileLexer.INDENT)
    .dedent(BashpileLexer.DEDENT)
    .pullToken(BashpileLexer.super::nextToken);

  @Override
  public Token nextToken() {
    return denter.nextToken();
  }

  private boolean isLinuxCommand(CharStream input) {
    return Lexers.isLinuxCommand(input);
  }
}

// keywords
Type     : 'empty' | 'bool' | 'int' | 'float' | 'str' | 'list' | 'map' | 'ref';
Function : 'function';
Block    : 'block';
Return   : 'return';
Print    : 'print';
Creates  : 'creates';
Bool     : 'true' | 'false';
If       : 'if';
ElseIf   : 'else-if';
Else     : 'else';
Switch   : 'switch';
Case     : 'case';
Pass     : 'pass';
Arguments: 'arguments';
All      : 'all';
Exported : 'exported';
Readonly : 'readonly';
ListOf   : 'listOf';
While    : 'while';

// operators, in precidence order
// opening parenthesis
OParen  : '(';
// closing parenthesis
CParen  : ')';
// unary minus (minus defiend below)
Increment: '++';
Decrement: '--';
Not     : 'not';
Arrow   : '->';
// cast in parser
Multiply: '*';
Divide  : '/';
Add     : '+';
Minus   : '-';
Isset   : 'isset';
Unset   : 'unset';
Empty   : 'isEmpty';
NotEmpty: 'isNotEmpty';
FileExists: 'fileExists';
RegularFileExists: 'regularFileExists';
DirectoryExists: 'directoryExists';
LessThan: '<';
LessThanOrEquals: '<=';
MoreThan: '>';
MoreThanOrEquals: '>=';
IsStrictlyEqual: '===';
InNotStrictlyEqual: '!==';
IsEqual : '==';
IsNotEqual : '!=';
And     : 'and';
Or      : 'or';
Equals  : '=';
PlusEquals: '+=';

// shell lines using Semantic Predicate
ShellLine   : {isLinuxCommand(_input)}? (Id Equals (Number | String))* Id SHELL_LINE_WORD*;

// ID and Numbers

// must start with a letter or underscore, then may have numbers
Id: ID_START ID_CONTINUE*;

Number: Float | Integer;

// future proof for octals to start with '0' like in C
Integer: NON_ZERO_DIGIT DIGIT* | '0';

Float: INT_PART? FRACTION | INT_PART '.';

// newlines, whitespace and comments
Newline      : '\r'? '\n' ' '*;
Whitespace   : [ \t] -> skip;
BashpileDoc  : '/**' .*? '*/' -> skip;
Comment      : '//' ~[\r\n\f]* -> skip;
BlockComment : '/*' ( BlockComment | . )*? '*/' -> skip;

// small tokens

Colon   : ':';
Comma   : ',';
// opening square bracket
OBracket: '[';
// closing square bracket
CBracket: ']';

// strings

String
 : '\'' ( StringEscapeSequence | ~[\\\r\n\f'] )* '\''
 | '"'  ( StringEscapeSequence | ~[\\\r\n\f"] )* '"'
 ;

StringEscapeSequence: '\\' . | '\\' Newline;

// tokens for modes

HashOParen  : '#(' -> pushMode(SHELL_STRING);
DollarOParen: '$(' -> pushMode(SHELL_STRING);

// modes

/** See https://github.com/sepp2k/antlr4-string-interpolation-examples/blob/master/with-duplication/StringLexer.g4 */
mode SHELL_STRING;
ShellStringHashOParen    : '#(' -> type(HashOParen), pushMode(SHELL_STRING);
ShellStringDollarOParen  : '$(' -> type(DollarOParen), pushMode(SHELL_STRING);
ShellStringOParen        : '(' -> type(OParen), pushMode(SHELL_STRING);
ShellStringText          : (~[\\\f()#$]
                            // LookAhead 1 - don't match '#(' but match other '#' characters
                            | '#' {_input.LA(1) != '('}?
                            | '$' {_input.LA(1) != '('}?
                           )+;
ShellStringEscapeSequence: '\\' . | '\\' Newline;
ShellStringCParen        : ')' -> type(CParen), popMode;

// fragments

fragment SHELL_LINE_WORD: ( StringEscapeSequence | ~[\\\r\n\f] )+;

fragment ID_START   : [a-zA-Z_];
// same as Bash ID rules -- no '-' for snake-case
fragment ID_CONTINUE: [a-zA-Z0-9_];

fragment NON_ZERO_DIGIT: [1-9];
fragment DIGIT         : [0-9];

fragment INT_PART: DIGIT+;
fragment FRACTION: '.' DIGIT+;
