grammar Grammar;

import GrammarVocab;

program: imprt* (stat | function | enumDef)*;

imprt: IMPORT STRING SEMI;

block: LCB stat* RCB;

stat: varSubStat SEMI										#varStat
	| IF LB expr RB stat  (ELSE stat)?						#ifStat
	| WHILE LB expr RB stat									#whileStat // break?
	| FOR LB varSubStat SEMI expr SEMI varSubStat RB stat	#forStat   // break?
	| DO stat WHILE LB expr RB SEMI							#doStat    // break?
	| SWITCH LB expr RB LCB (CASE expr block)* (DEFAULT block)? RCB	#switchStat
	| functionCall SEMI										#funcStat
	| WRITE expr expr SEMI									#writeStat
	| RETURN expr SEMI										#returnStat
	| NEXT SEMI												#nextStat
	| ACQUIRE target SEMI									#acquireStat //TODO: typecheck
	| RELEASE target SEMI									#releaseStat //TODO: typecheck
	| block													#blockStat 
	;

varSubStat: type ID				#declStat
	   | target ASSIGN expr		#assignStat
	   | type ID ASSIGN expr	#declAssignStat
	   ;

//target of assignment or expr
target: ID					#idTarget
	  | target LSB expr RSB	#arrayTarget
	  ;

// method definition parameter
argument: type ID;

enumDef: ENUM ID LCB (ID (COMMA ID)*)? RCB;

function: (THREAD | type?) ID LB (argument (COMMA argument)*)? RB (COLON stat | ARROW expr SEMI);

functionCall: ID LB (expr (COMMA expr)*)? RB;

type: INT						#intType
	| BOOL						#boolType
	| LSB type /*COLON NUM*/ RSB#arrayType
	| LOCK						#lockType
	| ID						#enumType
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
	| TRUE							#trueExpr
	| FALSE							#falseExpr
	| functionCall					#funcExpr
	| ID DOT ID						#enumExpr
	| LSB ( expr (COMMA expr)*) RSB	#arrayValueExpr 
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
