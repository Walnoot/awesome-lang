grammar Grammar;

import GrammarVocab;

program: imprt* (stat | function)*;

imprt: IMPORT STRING SEMI;

block: LCB stat* RCB;

stat: varSubStat SEMI										#varStat
	| PRINT LB expr RB SEMI									#printStat //temp, move to func later
	| IF LB expr RB stat  (ELSE stat)?						#ifStat
	| WHILE LB expr RB stat									#whileStat // break?
	| FOR LB varSubStat SEMI expr SEMI varSubStat RB stat	#forStat   // break?
	| DO stat WHILE LB expr RB SEMI							#doStat    // break?
	| functionCall SEMI										#funcStat
	| RETURN expr SEMI										#returnStat
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

function: type ID LB (argument (COMMA argument)*)? RB COLON stat;

functionCall: ID LB (expr (COMMA expr)*)? RB;

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
	| target				#targetExpr
	| NUM					#numExpr
	| TRUE					#trueExpr
	| FALSE					#falseExpr
	| functionCall			#funcExpr
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
