parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

program: statement+;
// TODO implement createsStatement
statement: expression NEWLINE             # expressionStatement
    | typedId (EQ expression)? NEWLINE    # assignmentStatement
    | ID EQ expression NEWLINE            # reassignmentStatement
    | PRINT OPAREN argumentList? CPAREN
                                  NEWLINE # printStatement
    | FUNCTION typedId paramaters         # functionForwardDeclarationStatement
    | FUNCTION typedId paramaters tags?
                        COL functionBlock # functionDeclarationStatement
    | BLOCK tags? COL INDENT statement+
                                   DEDENT # anonymousBlockStatement
//    | shellString CREATES STRING COL      # createsStatement
    | NEWLINE                             # blankStmt
    ;

tags: OBRACKET (STRING*) CBRACKET;
// like (x: str, y: str)
paramaters: OPAREN ( typedId (COMMA typedId)* )? CPAREN;
typedId: ID COL TYPE;
argumentList: expression (COMMA expression)*;

// Force the final statement to be a return.
// This is a work around for Bash not allawing the return keyword with a string.
// Bash will interpret the last line of a function (which may be a string) as the return if no return keyword.
// see https://linuxhint.com/return-string-bash-functions/ example 3
functionBlock: INDENT statement* returnPsudoStatement DEDENT;
returnPsudoStatement: RETURN expression? NEWLINE;

expression: shellString                 # shellStringExpression
    | ID OPAREN argumentList? CPAREN    # functionCallExpression
    // operator expressions
    | OPAREN expression CPAREN          # parenthesisExpression
    | <assoc=right> MINUS? NUMBER       # numberExpression      // has to be above calculationExpression
    | expression op=(MUL|DIV|ADD|MINUS)
                             expression # calculationExpression
    // type expressions
    | BOOL                              # boolExpression
    | STRING                            # stringExpression
    | ID                                # idExpression
    ;

shellString: HASH_OPAREN shellStringContents* CPAREN;
shellStringContents: SHELL_STRING_TEXT | SHELL_STRING_ESCAPE_SEQUENCE | shellString;
