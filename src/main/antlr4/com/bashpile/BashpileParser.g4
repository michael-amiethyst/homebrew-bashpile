parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stat+;

stat: expr NL       # printExpr
    | ID EQ expr NL # assign
    | NL            # blank
    ;

expr: expr op=(MUL|DIV) expr# MulDiv
    | expr op=(ADD|SUB) expr# AddSub
    | INT                   # int
    | ID                    # id
    | OPAREN expr CPAREN    # parens
    ;