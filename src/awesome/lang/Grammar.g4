grammar Grammar;

import GrammarVocab;

program: block;

block: LCB stat* RCB;

stat: type ID SEMI						#declStat
	| ID ASSIGN expr SEMI				#assignStat
	| type ID ASSIGN expr SEMI			#declAssignStat
	| ASM STRING SEMI					#asmStat
	| IF LB expr RB stat  (ELSE stat)?	#ifStat
	| WHILE LB expr RB stat				#whileStat
	| block								#blockStat
	;

type: INT | BOOL;

/** Expression. */
expr: prefixOp expr			#prefixExpr
	| expr addSubOp expr	#addSubExpr
	| expr multDivOp expr	#multDivExpr
	| expr compOp expr		#compExpr
	| expr boolOp expr		#boolExpr
	| LB expr RB			#parExpr
	| ID					#idExpr
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
