grammar Bashpile;

prog: stat+;

stat: expr NL
    | ID '=' expr NL
    | NL;

expr: expr ('*'|'/') expr
    | expr ('+'|'-') expr
    | INT
    | ID
    | '(' expr ')';

ID: [a-zA-Z]+;
INT: [0-9]+;
NL: '\r'? '\n';
WS: [ \t] -> skip;