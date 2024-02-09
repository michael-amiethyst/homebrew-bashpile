parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

program: statement+;

// statements, in descending order of complexity
statement
    : ShellLine Newline                   # shellLineStatement
    | shellString Creates (String|Id)
           Colon INDENT statement+ DEDENT # createsStatement
    | Function Id paramaters (Arrow type)?# functionForwardDeclarationStatement
    | Function Id paramaters tags? (Arrow type)?
                      Colon functionBlock # functionDeclarationStatement
    | Block tags? Colon functionBlock     # anonymousBlockStatement
    | If Not? expression Colon indentedStatements
         (elseIfClauses)*
         (Else Colon indentedStatements)? # conditionalStatement
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
typedId     : Id Colon modifier* type;
type        : Type (LessThan Type MoreThan)?;
modifier    : Exported | Readonly;
argumentList: expression (Comma expression)*;
elseIfClauses     : ElseIf Not? expression Colon indentedStatements;
indentedStatements: INDENT statement+ DEDENT;

// Force the final statement to be a return.
// This is a work around for Bash not allawing the return keyword with a string.
// Bash will interpret the last line of a function (which may be a string) as the return if no return keyword.
// see https://linuxhint.com/return-string-bash-functions/ example 3
functionBlock       : INDENT statement* (returnPsudoStatement | statement) DEDENT;
returnPsudoStatement: Return expression? Newline;

// in operator precedence order
expression
    : expression Colon type             # typecastExpression
    | shellString                       # shellStringExpression
    | Id OParen argumentList? CParen    # functionCallExpression
    // operator expressions
    | OParen expression CParen          # parenthesisExpression
    | expression
         op=(Multiply|Divide|Add|Minus)
                             expression # calculationExpression
    | unaryPrimary expression           # unaryPrimaryExpression
    | expression binaryPrimary
                             expression # binaryPrimaryExpression
    | expression combiningOperator
                             expression # combiningExpression
    | argumentsBuiltin                  # argumentsBuiltinExpression
    | ListOf OParen expression* CParen  # listOfBuiltinExpression
    // type expressions
    | Bool                              # boolExpression
    | <assoc=right> Minus? Number       # numberExpression
    | String                            # stringExpression
    | Id                                # idExpression
    | Id OBracket Number CBracket       # listExpression
    ;

shellString        : HashOParen shellStringContents* CParen;
shellStringContents: shellString
                   | DollarOParen shellStringContents* CParen
                   | OParen shellStringContents* CParen
                   | ShellStringText
                   | ShellStringEscapeSequence;

// full list at https://tldp.org/LDP/Bash-Beginners-Guide/html/sect_07_01.html
unaryPrimary: Isset | Unset | Empty | NotEmpty;

// one line means logically equal precidence (e.g. LessThan in the same as MoreThanOrEquals)
binaryPrimary: LessThan | LessThanOrEquals | MoreThan | MoreThanOrEquals
             | IsStrictlyEqual | InNotStrictlyEqual | IsEqual | IsNotEqual;

combiningOperator: And | Or;

argumentsBuiltin: Arguments OBracket Number CBracket;