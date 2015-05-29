grammar Java;

program: classDefinition;
classDefinition: modifier KW_CLASS name=ID OPEN_BRA classMember* CLOSE_BRA;
classMember: methodDefinition;
methodDefinition: 
    modifier returnType=typeQualifier name=ID parameters 
    (SEMI_COLON | OPEN_BRA statement+ CLOSE_BRA);
parameters: OPEN_PAR (parameter (COMMA parameter)*)? CLOSE_PAR;
parameter: type=typeQualifier name=ID;
typeQualifier: ID (DOT ID)*;
modifier: accessModifier? KW_ABSTRACT? KW_STATIC?;
accessModifier: KW_PUBLIC | KW_PRIVATE | KW_PROTECTED;
statement: delimitedStatement SEMI_COLON;
delimitedStatement: 
    returnStatement | variableDeclaration | expression;
returnStatement: KW_RETURN expression;
variableDeclaration: type=typeQualifier name=ID (OP_ASSIGN value=expression);
expression: variableAssignment | leafExpression;
variableAssignment: name=ID OP_ASSIGN value=expression;
leafExpression: ambigousName | intLiteral | stringLiteral;
ambigousName: ID ({_input.LT(2).getType() != OPEN_PAR}? DOT ID)*;
intLiteral: INT;
stringLiteral: STRING;

OP_ASSIGN: '=';
SEMI_COLON: ';';
DOT: '.';
COMMA: ',';
OPEN_PAR: '(';
CLOSE_PAR: ')';
OPEN_BRA: '{';
CLOSE_BRA: '}';
KW_RETURN: 'return';
KW_PUBLIC: 'public';
KW_PRIVATE: 'private';
KW_PROTECTED: 'protected';
KW_ABSTRACT: 'abstract';
KW_STATIC: 'static';
KW_CLASS: 'class';
fragment DIGIT: [0-9];
fragment LETTER: [A-Z]|[a-z];
ID: (LETTER | '_') (LETTER | '_' | DIGIT)*;

INT: DIGIT+ (DOT DIGIT+)?;
STRING: '"' (EscapeSequence | ~[\\"])* '"';
fragment HexDigit: [0-9a-fA-F];
fragment EscapeSequence: '\\' [btnfr"'\\] | UnicodeEscape | OctalEscape;
fragment OctalEscape: '\\' [0-3] [0-7] [0-7] | '\\' [0-7] [0-7] | '\\' [0-7];
fragment UnicodeEscape: '\\' 'u' HexDigit HexDigit HexDigit HexDigit;

WS: [ \n\t\r]+ -> skip;
SINGLE_LINE_COMMENT: '//' ~('\r' | '\n')* -> skip;
MULTI_LINE_COMMENT: '/*' .*? '*/' -> skip;