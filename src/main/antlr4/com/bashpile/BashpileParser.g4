parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stmt+;

stmt: expr NL                                   # exprStmt
    | ID EQ expr NL                             # assignStmt
    | PRINT OPAREN arglist? CPAREN NL           # printStmt
    | FUNCTION ID paramaters                    # functionForwardDeclStmt
    | FUNCTION ID paramaters tags? COL block    # functionDeclStmt
    | BLOCK tags? COL INDENT stmt+ DEDENT       # anonBlockStmt
    | returnRule                                # returnStmt
    | NL                                        # blankStmt
    ;

tags: OBRACKET (STRING*) CBRACKET;
paramaters: OPAREN (ID (COMMA ID)*)? CPAREN;
arglist: expr (COMMA expr)*;
// force the final statement to be a return to work around Bash not allawing the return keyword with a string
// but will interpret the last line of a function (which may be a string) as the return if no keyword
block: INDENT stmt* returnRule DEDENT;
returnRule: RETURN expr NL;

expr: ID OPAREN arglist? CPAREN     # functionCallExpr
    | expr (MUL|DIV|ADD|MINUS) expr # calcExpr
    | MINUS? NUMBER                 # numberExpr
    | ID                            # idExpr
    | STRING                        # stringExpr
    | OPAREN expr CPAREN            # parensExpr
    ;