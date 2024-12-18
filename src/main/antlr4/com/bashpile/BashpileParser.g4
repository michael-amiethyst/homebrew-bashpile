parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

program: statement+;

// statements, in descending order of complexity
statement
    : ShellLine Newline                         # shellLineStatement
    | While expression Colon indentedStatements # whileStatement
    | Function Id paramaters (Arrow complexType)?      # functionForwardDeclarationStatement
    | Function Id paramaters tags? (Arrow complexType)?
                            Colon functionBlock # functionDeclarationStatement
    | Block tags? Colon functionBlock           # anonymousBlockStatement
    | If expression Colon indentedStatements
         (elseIfClauses)*
         (Else Colon indentedStatements)?       # conditionalStatement
    | Switch expression Colon INDENT
         (Case expression Colon indentedStatements)+ DEDENT
                                                # switchStatement
    | <assoc=right> typedId
             (Equals expression)? Newline       # assignmentStatement
    | <assoc=right> (Id | listAccess) assignmentOperator
                             expression Newline # reassignmentStatement
    | Print OParen argumentList? CParen Newline # printStatement
    | expression Newline                        # expressionStatement
    | Newline                                   # blankStmt
    ;

tags        : OBracket (StringValues*) CBracket;
// like (x: str, y: str = "Jordi")
paramaters  : OParen ( typedId (Comma typedId)* (Comma defaultedTypedId)* )? CParen
            | OParen ( defaultedTypedId (Comma defaultedTypedId)* ) CParen;
defaultedTypedId  : typedId Equals literal;
typedId     : Id Colon modifier* complexType;
complexType : types (LessThan types MoreThan)?;
modifier    : Exported | Readonly;
argumentList: expression (Comma expression)*;
elseIfClauses     : ElseIf Not? expression Colon indentedStatements;
indentedStatements: INDENT statement+ DEDENT;
assignmentOperator: Equals | PlusEquals;

// Force the final statement to be a return.
// This is a work around for Bash not allawing the return keyword with a string.
// Bash will interpret the last line of a function (which may be a string) as the return if no return keyword.
// see https://linuxhint.com/return-string-bash-functions/ example 3
functionBlock       : INDENT statement* (returnPsudoStatement | statement) DEDENT;
returnPsudoStatement: Return expression? Newline;

// in operator precedence order, modeled on Java precedence at https://introcs.cs.princeton.edu/java/11precedence/
expression
    : listAccess                        # listAccessExpression
    | expression op=(Increment | Decrement) # unaryPostCrementExpression
    | <assoc=right> Minus? NumberValues # numberExpression // covers the unary '-' as well
    | <assoc=right> unaryPrimary
                             expression # unaryPrimaryExpression
    | expression Colon complexType      # typecastExpression
    | shellString                       # shellStringExpression
    | Id OParen argumentList? CParen    # functionCallExpression
    // operator expressions
    | OParen expression CParen          # parenthesisExpression
    | expression
         op=(Multiply|Divide|Add|Minus)
                             expression # calculationExpression
    | expression binaryPrimary
                             expression # binaryPrimaryExpression
    | expression combiningOperator
                             expression # combiningExpression
    | argumentsBuiltin                  # argumentsBuiltinExpression
    | ListOf (OParen CParen | OParen expression (Comma expression)* CParen)
                                        # listOfBuiltinExpression
    // type expressions
    | literal                           # literalExpression
    | Id                                # idExpression
    ;

literal : StringValues | NumberValues | BoolValues | Empty;
types    : Unknown | Empty | Bool | Number | Int | Float | Str | List | Map | Ref;

shellString        : HashOParen shellStringContents* CParen;
shellStringContents: shellString
                   | DollarOParen shellStringContents* CParen
                   | OParen shellStringContents* CParen
                   | ShellStringText
                   | ShellStringEscapeSequence;

// full list at https://tldp.org/LDP/Bash-Beginners-Guide/html/sect_07_01.html
unaryPrimary: Not | Isset | Unset | IsEmpty | NotEmpty | FileExists | RegularFileExists | DirectoryExists;

// one line means logically equal precidence (e.g. LessThan in the same as MoreThanOrEquals)
binaryPrimary: LessThan | LessThanOrEquals | MoreThan | MoreThanOrEquals
             | IsStrictlyEqual | InNotStrictlyEqual | IsEqual | IsNotEqual;

combiningOperator: And | Or;

// translates to $1, $2, etc
argumentsBuiltin: Arguments OBracket (NumberValues | All) CBracket;

listAccess: Id OBracket (Minus? NumberValues | All) CBracket;
