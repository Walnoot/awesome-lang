lexer grammar GrammarVocab;

LCB: '{';
RCB: '}';

LB: '(';
RB: ')';

LSB: '[';
RSB: ']';
COLON: ':';

IF: 'if';
ELSE: 'else';
WHILE: 'while';
PRINT: 'print';//temp, move to func later
ASM: 'asm';
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

ID: LETTER (LETTER | DIGIT)*;
NUM: DIGIT+;

STRING: '"' (~'"' | '""')* '"';

WS: [\t\r\n ]+ -> skip;
LINECOMMENT: '//' (~[\r\n])* -> skip;
BLOCKCOMMENT: '/*' .*? '*/' -> skip;

LETTER: [A-Za-z];
DIGIT: [0-9];
