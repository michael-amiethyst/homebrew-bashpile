parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stat+;

stat: expr NL
    | ID EQ expr NL
    | NL;

expr: expr (MUL|DIV) expr
    | expr (ADD|SUB) expr
    | INT
    | ID
    | OPAREN expr CPAREN;