grammar Grammar;

import GrammarVocab;

program: block;

block: LCB stat* RCB;

stat: type ID SEMI						#declStat
	| target ASSIGN expr SEMI			#assignStat
	| type ID ASSIGN expr SEMI			#declAssignStat
	| ASM STRING SEMI					#asmStat
	| PRINT LB expr RB SEMI				#printStat //temp, move to func later
	| IF LB expr RB stat  (ELSE stat)?	#ifStat
	| WHILE LB expr RB stat				#whileStat
	| block								#blockStat
	;

//TODO: rename this to variable, fix everything that breaks, methods in generator that return the address of a variable
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
