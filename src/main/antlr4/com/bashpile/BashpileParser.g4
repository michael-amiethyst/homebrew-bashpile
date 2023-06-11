parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stat+;

stat: expr NL                           # printExpr
    | ID EQ expr NL                     # assign
    | FUNCTION ID paramaters COL block  # functionDecl
    | BLOCK COL block                   # anonBlock
    | NL                                # blank
    ;

paramaters: OPAREN ID* CPAREN;
block: INDENT stat+ DEDENT;

expr: expr (MUL|DIV|ADD|SUB) expr # Calc
    | NUMBER                      # number
    | ID                          # id
    | OPAREN expr CPAREN          # parens
    ;