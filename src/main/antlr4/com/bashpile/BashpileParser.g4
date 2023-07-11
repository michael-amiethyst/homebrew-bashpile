parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

program: statement+;
statement: expression NL            # exprStmt
    | typedId (EQ expression)? NL   # assignStmt
    | ID EQ expression NL           # reAssignStmt
    | PRINT OPAREN argumentList?
                          CPAREN NL # printStmt
    | FUNCTION typedId paramaters   # functionForwardDeclStmt
    | FUNCTION typedId paramaters
            tags? COL functionBlock # functionDeclStmt
    | BLOCK tags? COL INDENT
                statement+ DEDENT   # anonBlockStmt
    | NL                            # blankStmt
    ;

tags: OBRACKET (STRING*) CBRACKET;
// like (x: str, y: str)
paramaters: OPAREN ( typedId (COMMA typedId)* )? CPAREN;
typedId: ID COL TYPE;
argumentList: expression (COMMA expression)*;

// force the final statement to be a return to work around Bash not allawing the return keyword with a string
// but will interpret the last line of a function (which may be a string) as the return if no keyword
// see https://linuxhint.com/return-string-bash-functions/ example 3
functionBlock: INDENT statement* returnPsudoStatement DEDENT;
returnPsudoStatement: RETURN expression? NL;

expression:
    ID OPAREN argumentList? CPAREN      # functionCallExpr
    // operator expressions
    | OPAREN expression CPAREN          # parenthesisExpr
    | <assoc=right> MINUS? NUMBER       # numberExpr      // has to be above calculationExpression
    | expression op=(MUL|DIV|ADD|MINUS)
                            expression  # calculationExpr
    // type expressions
    | BOOL                              # boolExpr
    | STRING                            # stringExpr
    | ID                                # idExpr
    ;