parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

prog: stmt+;
// TODO convert names to a long form (e.g. exprStmt to expressionStatement)
stmt: expr NL                               # exprStmt
    | typedId (EQ expr)? NL                 # assignStmt
    | ID EQ expr NL                         # reAssignStmt
    | PRINT OPAREN arglist? CPAREN NL       # printStmt
    | FUNCTION typedId paramaters           # functionForwardDeclStmt
    | FUNCTION typedId paramaters tags? COL
                                  funcBlock # functionDeclStmt
    | BLOCK tags? COL INDENT stmt+ DEDENT   # anonBlockStmt
    | NL                                    # blankStmt
    ;

tags: OBRACKET (STRING*) CBRACKET;
// like (x: str, y: str)
paramaters: OPAREN ( typedId (COMMA typedId)* )? CPAREN;
typedId: ID COL TYPE;
arglist: expr (COMMA expr)*;

// force the final statement to be a return to work around Bash not allawing the return keyword with a string
// but will interpret the last line of a function (which may be a string) as the return if no keyword
// see https://linuxhint.com/return-string-bash-functions/ example 3
funcBlock: INDENT stmt* returnPsudoStmt DEDENT;
returnPsudoStmt: RETURN expr? NL;

expr: ID OPAREN arglist? CPAREN         # functionCallExpr
    // operator expressions
    | OPAREN expr CPAREN                # parenthesisExpr
    | <assoc=right> MINUS? NUMBER       # numberExpr      // has to be above calculationExpression
    | expr op=(MUL|DIV|ADD|MINUS) expr  # calculationExpr
    // type expressions
    | BOOL                              # boolExpr
    | STRING                            # stringExpr
    | ID                                # idExpr
    ;