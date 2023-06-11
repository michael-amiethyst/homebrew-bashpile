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

BLOCK: 'block';

ID: [a-zA-Z]+;
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

OPAREN: '(';
CPAREN: ')';
EQ: '=';
MUL: '*';
DIV: '/';
ADD: '+';
SUB: '-';

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