lexer grammar GrammarVocab;

LCB: '{';
RCB: '}';

LB: '(';
RB: ')';

LSB: '[';
RSB: ']';
COLON: ':';
ARROW: '->';
COMMA: ',';

IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
DO: 'do';
SWITCH: 'switch';
CASE: 'case';
DEFAULT: 'default';
NEXT: 'next';
IMPORT: 'import';
RETURN: 'return';
WRITE: 'write';
READ: 'read';
THREAD: 'thread';
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
MOD: 'mod';
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
