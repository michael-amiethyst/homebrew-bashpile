lexer grammar BashpileLexer;

tokens { INDENT, DEDENT }

@lexer::header {
  import com.yuvalshavit.antlr4.DenterHelper;
}

@lexer::members {
  private final DenterHelper denter = DenterHelper.builder()
    .nl(NL)
    .indent(BashpileLexer.INDENT)
    .dedent(BashpileLexer.DEDENT)
    .pullToken(BashpileLexer.super::nextToken);

  @Override
  public Token nextToken() {
    return denter.nextToken();
  }
}

TYPE: 'empty' | 'bool' | 'int' | 'float' | 'str' | 'array' | 'map' | 'ref';
FUNCTION: 'function';
BLOCK: 'block';
RETURN: 'return';
PRINT: 'print';
BOOL: 'true' | 'false';

ID: ID_START ID_CONTINUE*;

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

NL: '\r'? '\n' ' '*;
WS: [ \t] -> skip;
BASHPILE_DOC: '/**' .*? '*/' -> skip;
COMMENT: '//' ~[\r\n\f]* -> skip;
BLOCK_COMMENT: '/*' ( BLOCK_COMMENT | . )*? '*/' -> skip;

OPAREN: '(';
CPAREN: ')';
EQ: '=';
MUL: '*';
DIV: '/';
ADD: '+';
MINUS: '-';
COL: ':';
COMMA: ',';
OBRACKET: '[';
CBRACKET: ']';
DOLLAR: '$';
DOT: '.';

STRING
 : '\'' ( ~[\\\r\n\f'] )* '\''
 | '"' ( ~[\\\r\n\f"] )* '"'
 ;

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