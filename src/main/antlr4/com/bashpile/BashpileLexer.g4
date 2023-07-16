lexer grammar BashpileLexer;

tokens { INDENT, DEDENT }

@lexer::header {
  import com.yuvalshavit.antlr4.DenterHelper;
}

@lexer::members {
  private final DenterHelper denter = DenterHelper.builder()
    .nl(NEWLINE)
    .indent(BashpileLexer.INDENT)
    .dedent(BashpileLexer.DEDENT)
    .pullToken(BashpileLexer.super::nextToken);

  @Override
  public Token nextToken() {
    return denter.nextToken();
  }
}

// keywords
TYPE    : 'empty' | 'bool' | 'int' | 'float' | 'str' | 'array' | 'map' | 'ref';
FUNCTION: 'function';
BLOCK   : 'block';
RETURN  : 'return';
PRINT   : 'print';
BOOL    : 'true' | 'false';
CREATES : 'creates';

// ID and Numbers

ID: ID_START ID_CONTINUE*;

// TODO change most rules to ClassName format, keep modes and fragments as-is
NUMBER
 : INTEGER
 | FLOAT_NUMBER
 ;

INTEGER
 : NON_ZERO_DIGIT DIGIT*
 ;

FLOAT_NUMBER
 : INT_PART? FRACTION
 | INT_PART '.'
 ;

// newlines, whitespace and comments
NEWLINE      : '\r'? '\n' ' '*;
WS           : [ \t] -> skip;
BASHPILE_DOC : '/**' .*? '*/' -> skip;
COMMENT      : '//' ~[\r\n\f]* -> skip;
BLOCK_COMMENT: '/*' ( BLOCK_COMMENT | . )*? '*/' -> skip;

// single char tokens
OPAREN  : '(';
CPAREN  : ')';
EQ      : '=';
MUL     : '*';
DIV     : '/';
ADD     : '+';
MINUS   : '-';
COL     : ':';
COMMA   : ',';
OBRACKET: '[';
CBRACKET: ']';

// strings

STRING
 : '\'' ( STRING_ESCAPE_SEQ | ~[\\\r\n\f'] )* '\''
 | '"'  ( STRING_ESCAPE_SEQ | ~[\\\r\n\f"] )* '"'
 ;

STRING_ESCAPE_SEQ: '\\' . | '\\' NEWLINE;

// tokens for modes

HASH_OPAREN  : '#(' -> pushMode(SHELL_STRING);
DOLLAR_OPAREN: '$(' -> pushMode(COMMAND_SUBSTITUTION);

// modes
// TODO rename modes to end with '_MODE'

/** See https://github.com/sepp2k/antlr4-string-interpolation-examples/blob/master/with-duplication/StringLexer.g4 */
mode SHELL_STRING;

SHELL_STRING_HASH_OPAREN: '#(' -> type(HASH_OPAREN), pushMode(SHELL_STRING);
SHELL_STRING_TEXT: (~[\\\r\n\f)#]
                   // LookAhead 1 - don't match '#(' but match other '#' characters
                   | '#' {_input.LA(1) != '('}?
                   )+;
SHELL_STRING_ESCAPE_SEQUENCE: '\\' . | '\\' NEWLINE;
SHELL_STRING_CPAREN: ')' -> type(CPAREN), popMode;

mode COMMAND_SUBSTITUTION;

COMMAND_SUBSTITUTION_DOLLAR_OPAREN: '$(' -> type(DOLLAR_OPAREN), pushMode(COMMAND_SUBSTITUTION);
COMMAND_SUBSTITUTION_TEXT: (~[\\\r\n\f)$]
                           // LookAhead 1 - don't match '$(' but match other '$' characters
                           | '$' {_input.LA(1) != '('}?
                           )+;
COMMAND_SUBSTITUTION_ESCAPE_SEQUENCE: '\\' . | '\\' NEWLINE;
COMMAND_SUBSTITUTION_CPAREN: ')' -> type(CPAREN), popMode;

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