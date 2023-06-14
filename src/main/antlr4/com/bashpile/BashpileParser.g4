parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stmt+;

stmt: expr NL                           # exprStmt
    | ID EQ expr NL                     # assignStmt
    | FUNCTION ID paramaters COL block  # functionDeclStmt
    | BLOCK COL block                   # anonBlockStmt
    | RETURN expr NL                    # returnStmt
    | NL                                # blankStmt
    ;

paramaters: OPAREN (ID (COMMA ID)*)? CPAREN;
arglist: expr (COMMA expr)*;
block: INDENT stmt+ DEDENT;

expr: ID OPAREN arglist CPAREN      # functionCallExpr
    | expr (MUL|DIV|ADD|MINUS) expr # calcExpr
    | MINUS? NUMBER                 # numberExpr
    | ID                            # idExpr
    | OPAREN expr CPAREN            # parensExpr
    ;