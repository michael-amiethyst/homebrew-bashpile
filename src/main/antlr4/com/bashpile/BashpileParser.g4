parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stat+;

stat: expr NL                           # printExpr
    | ID EQ expr NL                     # assign
    | FUNCTION ID paramaters COL block  # functionDecl
    | BLOCK COL block                   # anonBlock
    | NL                                # blank
    ;

paramaters: OPAREN (ID (COMMA ID)*)? CPAREN;
arglist: expr (COMMA expr)*;
block: INDENT stat+ DEDENT;

expr: ID OPAREN arglist CPAREN      # functionCall
    | expr (MUL|DIV|ADD|MINUS) expr # Calc
    | MINUS? NUMBER                 # number
    | ID                            # id
    | OPAREN expr CPAREN            # parens
    ;