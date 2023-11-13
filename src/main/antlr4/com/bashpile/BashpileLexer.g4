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

  private boolean isLinuxCommand(String input) {
    return Lexers.isLinuxCommand(input);
  }
}

// keywords
Type     : 'empty' | 'bool' | 'int' | 'float' | 'str' | 'array' | 'map' | 'ref';
Function : 'function';
Block    : 'block';
Return   : 'return';
Print    : 'print';
Creates  : 'creates';
Bool     : 'true' | 'false';
If       : 'if';
ElseIf   : 'else if';
Else     : 'else';
Pass     : 'pass';
Arguments: 'arguments';
All      : 'all';
// TODO 'exported' 'readonly'

// operators, in precidence order
// opening parenthesis
OParen  : '(';
// closing parenthesis
CParen  : ')';
// unary minus (minus defiend below)
Not     : 'not';
// cast in parser
Multiply: '*';
Divide  : '/';
Add     : '+';
Minus   : '-';
// TODO FEATURE feature/relationalOperators relationals here
Unset   : 'unset';
Empty   : 'isEmpty';
NotEmpty: 'isNotEmpty';
// TODO FEATURE feature/equalityOperators equality here with == and ===
And     : 'and';
Or      : 'or';

// shell lines using Semantic Predicate
ShellLine   : {isLinuxCommand(_input.toString())}? (Id Equals (Number | String))* Id SHELL_LINE_WORD*;

// ID and Numbers

Id: ID_START ID_CONTINUE*;

Number: Integer | Float;

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

Equals  : '=';
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
fragment ID_CONTINUE: [a-zA-Z0-9_];

fragment NON_ZERO_DIGIT: [1-9];
fragment DIGIT         : [0-9];

fragment INT_PART: DIGIT+;
fragment FRACTION: '.' DIGIT+;
