grammar Grammar;

import GrammarVocab;

program: imprt* (stat | function | enumDef | classDef)*;

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
	| NEXT SEMI												#nextStat
	| ACQUIRE target SEMI									#acquireStat //(Rejected if the scope is not global)
	| RELEASE target SEMI									#releaseStat //(Rejected if the scope is not global)
	| block													#blockStat 
	;

varSubStat : declStat | assignStat | declAssignStat;
declStat: type ID;
assignStat: target ASSIGN expr;
declAssignStat: type ID ASSIGN expr;
//varSubStat: type ID				#declStat
//	   | target ASSIGN expr		#assignStat
//	   | type ID ASSIGN expr	#declAssignStat
//	   ;

//target of assignment or expr
target: ID					#idTarget // default value is a zero for all types
	  | target LSB expr RSB	#arrayTarget
	  | target DOT ID		#classTarget
	  ;

// method definition parameter
argument: type ID;

enumDef: ENUM ID LCB (ID (COMMA ID)*)? RCB;

classDef: CLASS ID LCB (declStat SEMI | function)* RCB ;

function: (THREAD | type?) ID LB (argument (COMMA argument)*)? RB (COLON stat | ARROW expr SEMI);

functionCall: ID LB (expr (COMMA expr)*)? RB (ON expr)?;

newObject: NEW ID LB (expr (COMMA expr)*)? RB;
 
type: INT						#intType
	| FLOAT						#floatType
	| BOOL						#boolType
	| CHAR						#charType
	| LSB type /*COLON NUM*/ RSB#arrayType
	| LOCK						#lockType
	| ID						#enumOrClassType
	;

/** Expression. */
expr: prefixOp expr					#prefixExpr
	| expr addSubOp expr			#addSubExpr
	| expr MOD expr					#modExpr
	| expr multDivOp expr			#multDivExpr
	| expr compOp expr				#compExpr
	| expr boolOp expr				#boolExpr
	| LB expr RB					#parExpr
	| target						#targetExpr
	| READ expr						#readExpr
	| NUM							#numExpr
	| FLOATLITERAL					#floatExpr
	| FLOAT LB expr RB				#floatCastExpr
	| INT LB expr RB				#intCastExpr
	| CHARLITERAL					#charExpr
	| TRUE							#trueExpr
	| FALSE							#falseExpr
	| functionCall					#funcExpr
	| ID COLON ID					#enumExpr
	| LSB ( expr (COMMA expr)*) RSB	#arrayValueExpr
	| type LSB expr RSB				#arrayLengthExpr
	| newObject						#newObjectExpr
	| STRING						#stringExpr
	;

/** Prefix operator. */
prefixOp: SUB | NOT;

/** Multiplicative operator. */
multDivOp: MULT | DIV;

/** Additive operator. */
addSubOp: ADD | SUB;

/** Boolean operator. */
boolOp: AND | OR;

/** Comparison operator. */
compOp: LE | LT | GE | GT | EQ | NE;
