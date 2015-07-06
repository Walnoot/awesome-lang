grammar Grammar;

import GrammarVocab;

program: imprt* (stat | function | enumDef | classDef)*;

//import is a reserved keyword in ANTLR, therefore imprt
imprt: IMPORT STRING SEMI;

block: LCB stat* RCB;

stat: varSubStat SEMI										#varStat
	| IF LB expr RB stat  (ELSE stat)?						#ifStat
	| WHILE LB expr RB stat									#whileStat
	| FOR LB varSubStat SEMI expr SEMI varSubStat RB stat	#forStat
	| DO stat WHILE LB expr RB SEMI							#doStat
	| SWITCH LB expr RB LCB (CASE expr block)* (DEFAULT block)? RCB	#switchStat
	| functionCall SEMI										#funcStat
	| WRITE expr expr SEMI									#writeStat
	| RETURN expr SEMI										#returnStat
	| NEXT SEMI												#nextStat     //used in switch statement
	| ACQUIRE target SEMI									#acquireStat  //acquire a lock
	| RELEASE target SEMI									#releaseStat  //release a lock
	| block													#blockStat
	;

varSubStat : declStat | assignStat | declAssignStat;
declStat: type ID;
assignStat: target ASSIGN expr;
declAssignStat: type ID ASSIGN expr;

//target of assignment or expr
target: ID					#idTarget
	  | target LSB expr RSB	#arrayTarget
	  | target DOT ID		#classTarget
	  ;

// method definition parameter
argument: type ID;

//enumerated type
enumDef: ENUM ID LCB (ID (COMMA ID)*)? RCB;

//class definition
classDef: CLASS ID LCB (declStat SEMI | function)* RCB ;

//function definition
function: (THREAD | type?) ID LB (argument (COMMA argument)*)? RB (COLON stat | ARROW expr SEMI);

functionCall: ID LB (expr (COMMA expr)*)? RB (ON expr)?;

newObject: NEW ID LB (expr (COMMA expr)*)? RB;
 
type: INT			#intType
	| FLOAT			#floatType
	| BOOL			#boolType
	| CHAR			#charType
	| LSB type RSB	#arrayType
	| LOCK			#lockType
	| ID			#enumOrClassType
	;

//Expression
expr: prefixOp expr					#prefixExpr
	| expr addSubOp expr			#addSubExpr
	| expr MOD expr					#modExpr			//modulo
	| expr multDivOp expr			#multDivExpr
	| expr compOp expr				#compExpr
	| expr boolOp expr				#boolExpr
	| LB expr RB					#parExpr
	| target						#targetExpr			//gets the value of an variable
	| READ expr						#readExpr			//read from memory mapped device
	| NUM							#numExpr			//decimal value
	| FLOATLITERAL					#floatExpr			//floating-point value
	| FLOAT LB expr RB				#floatCastExpr		//convert from int to float
	| INT LB expr RB				#intCastExpr		//convert from float to int
	| CHARLITERAL					#charExpr			//character value
	| TRUE							#trueExpr
	| FALSE							#falseExpr
	| functionCall					#funcExpr
	| ID COLON ID					#enumExpr
	| LSB ( expr (COMMA expr)*) RSB	#arrayValueExpr		//array literal
	| type LSB expr RSB				#arrayLengthExpr	//construct array of <expr> size
	| newObject						#newObjectExpr
	| STRING						#stringExpr
	;

//Prefix operator, unary minus or boolean inverse
prefixOp: SUB | NOT;

//Multiplicative operator
multDivOp: MULT | DIV;

//Additive operator
addSubOp: ADD | SUB;

//Boolean operator
boolOp: AND | OR;

//Comparison operator
compOp: LE | LT | GE | GT | EQ | NE;
