parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stat+;

stat: expr NL                           # printExpr
    | ID EQ expr NL                     # assign
    | FUNCTION ID paramaters COL block  # functionDecl
    | BLOCK COL block                   # anonBlock
    | NL                                # blank
    ;

paramaters: OPAREN expr* CPAREN;
block: INDENT stat+ DEDENT;

expr: ID paramaters                 # functionCall
    | expr (MUL|DIV|ADD|MINUS) expr # Calc
    | MINUS? NUMBER                 # number
    | idRule                        # id
    | OPAREN expr CPAREN            # parens
    ;

// TODO simplify
idRule: ID;