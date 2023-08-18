parser grammar BashpileParser;
options { tokenVocab = BashpileLexer; }

program: statement+;

// statements, in descending order of complexity
statement
    : (typedId Equals)? shellString Creates (String|Id)
           Colon INDENT statement+ DEDENT # createsStatement
    | Function typedId paramaters         # functionForwardDeclarationStatement
    | Function typedId paramaters tags?
                      Colon functionBlock # functionDeclarationStatement
    | Block tags? Colon INDENT statement+
                                   DEDENT # anonymousBlockStatement
    | If expression Colon INDENT
                        statement+ DEDENT # conditionalStatement
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
typedId     : Id Colon Type;
argumentList: expression (Comma expression)*;

// Force the final statement to be a return.
// This is a work around for Bash not allawing the return keyword with a string.
// Bash will interpret the last line of a function (which may be a string) as the return if no return keyword.
// see https://linuxhint.com/return-string-bash-functions/ example 3
functionBlock       : INDENT statement* returnPsudoStatement DEDENT;
returnPsudoStatement: Return expression? Newline;

// in operator precedence order?
expression
    : expression Colon Type             # typecastExpression
    | shellString                       # shellStringExpression
    | inline                            # inlineExpression
    | Id OParen argumentList? CParen    # functionCallExpression
    // operator expressions
    | OParen expression CParen          # parenthesisExpression
    | expression
         op=(Multiply|Divide|Add|Minus)
                             expression # calculationExpression
    | primary
           (String|Id|argumentsBuiltin) # primaryExpression
    // type expressions
    | Bool                              # boolExpression
    | <assoc=right> Minus? Number       # numberExpression
    | String                            # stringExpression
    | Id                                # idExpression
    ;

shellString        : HashOParen shellStringContents* CParen;
shellStringContents: ShellStringText | ShellStringEscapeSequence | inline | shellString;

inline        : DollarOParen inlineContents* CParen;
inlineContents: InlineText
              | InlineEscapeSequence
              | shellString
              | inline;

// full list at https://tldp.org/LDP/Bash-Beginners-Guide/html/sect_07_01.html
primary: Unset;

argumentsBuiltin: Arguments OBracket Number CBracket;