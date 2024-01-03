parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

program: statement+;

// statements, in descending order of complexity
statement
    : ShellLine Newline                   # shellLineStatement
    | (typedId Equals)? shellString Creates (String|Id)
           Colon INDENT statement+ DEDENT # createsStatement
    | Function Id paramaters (Arrow Type)?# functionForwardDeclarationStatement
    | Function Id paramaters tags? (Arrow Type)?
                      Colon functionBlock # functionDeclarationStatement
    | Block tags? Colon functionBlock     # anonymousBlockStatement
    | If Not? expression Colon INDENT statement+
            DEDENT (Else Colon elseBody)? # conditionalStatement
    | typedId (Equals expression)? Newline# assignmentStatement
    | Id Equals expression Newline        # reassignmentStatement
    | Print OParen argumentList? CParen
                                  Newline # printStatement
    | expression Newline                  # expressionStatement
    | Newline                             # blankStmt
    ;

tags        : OBracket (String*) CBracket;
// like (x: str, y: str)
paramaters  : OParen ( typedId (Comma typedId)* )? CParen;
typedId     : Id Colon modifier* Type;
modifier    : Exported | Readonly;
argumentList: expression (Comma expression)*;
elseBody    : INDENT statement+ DEDENT;

// Force the final statement to be a return.
// This is a work around for Bash not allawing the return keyword with a string.
// Bash will interpret the last line of a function (which may be a string) as the return if no return keyword.
// see https://linuxhint.com/return-string-bash-functions/ example 3
functionBlock       : INDENT statement* (returnPsudoStatement | statement) DEDENT;
returnPsudoStatement: Return expression? Newline;

// in operator precedence order?
expression
    : expression Colon Type             # typecastExpression
    | shellString                       # shellStringExpression
    | Id OParen argumentList? CParen    # functionCallExpression
    // operator expressions
    | OParen expression CParen          # parenthesisExpression
    | expression
         op=(Multiply|Divide|Add|Minus)
                             expression # calculationExpression
    | primary expression                # primaryExpression
    | argumentsBuiltin                  # argumentsBuiltinExpression
    // type expressions
    | Bool                              # boolExpression
    | <assoc=right> Minus? Number       # numberExpression
    | String                            # stringExpression
    | Id                                # idExpression
    ;

shellString        : HashOParen shellStringContents* CParen;
shellStringContents: shellString
                   | DollarOParen shellStringContents* CParen
                   | OParen shellStringContents* CParen
                   | ShellStringText
                   | ShellStringEscapeSequence;

// full list at https://tldp.org/LDP/Bash-Beginners-Guide/html/sect_07_01.html
primary: Unset | Empty | NotEmpty;

argumentsBuiltin: Arguments OBracket Number CBracket;