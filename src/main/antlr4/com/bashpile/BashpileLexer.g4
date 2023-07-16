lexer grammar BashpileLexer;

tokens { INDENT, DEDENT }

@lexer::header {
  import com.yuvalshavit.antlr4.DenterHelper;
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
}

// keywords
Type    : 'empty' | 'bool' | 'int' | 'float' | 'str' | 'array' | 'map' | 'ref';
Function: 'function';
Block   : 'block';
Return  : 'return';
Print   : 'print';
Bool    : 'true' | 'false';
Creates : 'creates';

// ID and Numbers

Id: ID_START ID_CONTINUE*;

Number
 : Integer
 | Float
 ;

Integer
 : NON_ZERO_DIGIT DIGIT*
 ;

Float
 : INT_PART? FRACTION
 | INT_PART '.'
 ;

// newlines, whitespace and comments
Newline      : '\r'? '\n' ' '*;
Whitespace   : [ \t] -> skip;
BashpileDoc  : '/**' .*? '*/' -> skip;
Comment      : '//' ~[\r\n\f]* -> skip;
BlockComment : '/*' ( BlockComment | . )*? '*/' -> skip;

// single char tokens

// opening parenthesis
OParen  : '(';
// closing parenthesis
CParen  : ')';
Equals  : '=';
Multiply: '*';
Divide  : '/';
Add     : '+';
Minus   : '-';
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
DollarOParen: '$(' -> pushMode(COMMAND_SUBSTITUTION);

// modes

/** See https://github.com/sepp2k/antlr4-string-interpolation-examples/blob/master/with-duplication/StringLexer.g4 */
mode SHELL_STRING;

ShellStringHashOParen    : '#(' -> type(HashOParen), pushMode(SHELL_STRING);
ShellStringDollarOParen  : '$(' -> type(DollarOParen), pushMode(COMMAND_SUBSTITUTION);
ShellStringText          : (~[\\\r\n\f)#$]
                         // LookAhead 1 - don't match '#(' but match other '#' characters
                         | '#' {_input.LA(1) != '('}?
                         | '$' {_input.LA(1) != '('}?
                         )+;
ShellStringEscapeSequence: '\\' . | '\\' Newline;
ShellStringCParen        : ')' -> type(CParen), popMode;

mode COMMAND_SUBSTITUTION;

CommandSubstitutionHashOParen    : '#(' -> type(HashOParen), pushMode(SHELL_STRING);
CommandSubstitutionDollarOParen  : '$(' -> type(DollarOParen), pushMode(COMMAND_SUBSTITUTION);
CommandSubstitutionText          : (~[\\\r\n\f)$#]
                                 // LookAhead 1 - don't match '$(' but match other '$' characters
                                 | '$' {_input.LA(1) != '('}?
                                 | '#' {_input.LA(1) != '('}?
                                 )+;
CommandSubstitutionEscapeSequence: '\\' . | '\\' Newline;
CommandSubstitutionCParen        : ')' -> type(CParen), popMode;

// fragments

fragment ID_START: [a-zA-Z_];
fragment ID_CONTINUE: [a-zA-Z0-9_];

/// nonzerodigit   ::=  "1"..."9"
fragment NON_ZERO_DIGIT
 : [1-9]
 ;

/// digit          ::=  "0"..."9"
fragment DIGIT
 : [0-9]
 ;

fragment INT_PART
 : DIGIT+
 ;

/// fraction      ::=  "." digit+
fragment FRACTION
 : '.' DIGIT+
 ;