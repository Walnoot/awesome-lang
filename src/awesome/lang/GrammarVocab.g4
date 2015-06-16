lexer grammar GrammarVocab;

LCB: '{';
RCB: '}';

LB: '(';
RB: ')';

IF: 'if';
WHILE: 'while';
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

STRING: '"' (~'"')* '"';

WS: [\t\r\n ]+ -> skip;

LETTER: [A-Za-z];
DIGIT: [0-9];
