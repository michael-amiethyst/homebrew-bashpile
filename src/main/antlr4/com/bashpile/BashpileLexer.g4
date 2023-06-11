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
INT: [0-9]+;
NL: '\r'? '\n' ' '*;
WS: [ \t] -> skip;

OPAREN: '(';
CPAREN: ')';
EQ: '=';
MUL: '*';
DIV: '/';
ADD: '+';
SUB: '-';