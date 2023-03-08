lexer grammar BashpileLexer;

ID: [a-zA-Z]+;
INT: [0-9]+;
NL: '\r'? '\n';
WS: [ \t] -> skip;

OPAREN: '(';
CPAREN: ')';
EQ: '=';
MUL: '*';
DIV: '/';
ADD: '+';
SUB: '-';