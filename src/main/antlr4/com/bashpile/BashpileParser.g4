parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stat+;

// TODO rename to stmt, have all labels end with _stmt
stat: expr NL                           # printExpr
    | ID EQ expr NL                     # assign
    | FUNCTION ID paramaters COL block  # functionDecl
    | BLOCK COL block                   # anonBlock
    | RETURN expr NL                    # returnStmt
    | NL                                # blank
    ;

paramaters: OPAREN (ID (COMMA ID)*)? CPAREN;
arglist: expr (COMMA expr)*;
block: INDENT stat+ DEDENT;

// TODO have all expresson labels end with _expr
expr: ID OPAREN arglist CPAREN      # functionCall
    | expr (MUL|DIV|ADD|MINUS) expr # Calc
    | MINUS? NUMBER                 # number
    | ID                            # id
    | OPAREN expr CPAREN            # parens
    ;