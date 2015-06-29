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


argument: type ID;

enumDef: ENUM ID LCB (ID (COMMA ID)*)? RCB;

function: (THREAD | type?) ID LB (argument (COMMA argument)*)? RB (COLON stat | ARROW expr SEMI);

functionCall: ID LB (expr (COMMA expr)*)? RB;

type: INT						#intType
	| BOOL						#boolType
	| LSB type COLON NUM RSB	#arrayType
	| ID						#enumType
	;

/** Expression. */
expr: prefixOp expr			#prefixExpr
	| expr addSubOp expr	#addSubExpr
	| expr MOD expr			#modExpr
	| expr multDivOp expr	#multDivExpr
	| expr compOp expr		#compExpr
	| expr boolOp expr		#boolExpr
	| LB expr RB			#parExpr
	| target				#targetExpr
	| READ expr				#readExpr
	| NUM					#numExpr
	| TRUE					#trueExpr
	| FALSE					#falseExpr
	| functionCall			#funcExpr
	| ID DOT ID				#enumExpr
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
