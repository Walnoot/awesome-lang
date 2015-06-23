lexer grammar GrammarVocab;

LCB: '{';
RCB: '}';

LB: '(';
RB: ')';

LSB: '[';
RSB: ']';
COLON: ':';
COMMA: ',';

IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
DO: 'do';
PRINT: 'print';//temp, move to func later
ASM: 'asm';
IMPORT: 'import';
SEMI: ';';
ASSIGN: '=';

INT: 'int';
BOOL: 'bool';

TRUE: 'true';
FALSE: 'false';

EQ: '==';
NE: '!=';
GE: '>=';
GT: '>';
LE: '<=';
LT: '<';
ADD: '+';
SUB: '-';
MULT: '*';
DIV: '/';
AND: 'and';
OR: 'or';
NOT: 'not';

STRING: '"' (~'"' | '\\"')* '"';

ID: LETTER (LETTER | DIGIT)*;
NUM: DIGIT+;

WS: [\t\r\n ]+ -> skip;
LINECOMMENT: '//' (~[\r\n])* -> skip;
BLOCKCOMMENT: '/*' .*? '*/' -> skip;

LETTER: [A-Za-z];
DIGIT: [0-9];
