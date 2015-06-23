grammar Grammar;

import GrammarVocab;

program: block;

block: LCB stat* RCB;

stat: varSubStat SEMI										#varStat
	| ASM STRING SEMI										#asmStat
	| PRINT LB expr RB SEMI									#printStat //temp, move to func later
	| IF LB expr RB stat  (ELSE stat)?						#ifStat
	| WHILE LB expr RB stat									#whileStat
	| FOR LB varSubStat SEMI expr SEMI varSubStat RB stat	#forStat
	| DO stat WHILE LB expr RB SEMI							#doStat
	| block													#blockStat
	;
	
varSubStat: type ID #declStat
	   | target ASSIGN expr #assignStat
	   | type ID ASSIGN expr #declAssignStat
	   ;

//target of assignment
target: ID				#idTarget
	  | ID LSB expr RSB	#arrayTarget
	  ;

type: INT						#intType
	| BOOL						#boolType
	| LSB type COLON NUM RSB	#arrayType
	;

/** Expression. */
expr: prefixOp expr			#prefixExpr
	| expr addSubOp expr	#addSubExpr
	| expr multDivOp expr	#multDivExpr
	| expr compOp expr		#compExpr
	| expr boolOp expr		#boolExpr
	| LB expr RB			#parExpr
	| ID					#idExpr
	| ID LSB expr RSB		#arrayExpr
	| NUM					#numExpr
	| TRUE					#trueExpr
	| FALSE					#falseExpr
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
