package awesome.lang;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarParser.AddSubExprContext;
import awesome.lang.GrammarParser.BoolExprContext;
import awesome.lang.GrammarParser.CompExprContext;
import awesome.lang.GrammarParser.FalseExprContext;
import awesome.lang.GrammarParser.MultDivExprContext;
import awesome.lang.GrammarParser.NumExprContext;
import awesome.lang.GrammarParser.ParExprContext;
import awesome.lang.GrammarParser.PrefixExprContext;
import awesome.lang.GrammarParser.TrueExprContext;
import awesome.lang.model.Instruction;
import awesome.lang.model.OpCode;
import awesome.lang.model.Operator;
import awesome.lang.model.Program;

/**
 * Generates sprockell code, visit methods return the first instruction of the block they generate
 */
public class Generator extends GrammarBaseVisitor<Instruction> {
	private static final int TRUE = -1, FALSE = 0;
	
	private ParseTreeProperty<VReg> regs = new ParseTreeProperty<VReg>();
	private int regCounter = 0;//virtual register counter
	private Program prog;
	
	public Generator() {
		prog = new Program(1);
	}
	
	//expressions
	
	@Override
	public Instruction visitPrefixExpr(PrefixExprContext ctx) {
		Instruction i = visit(ctx.expr());
		
		if(ctx.prefixOp().SUB() != null){
			//unary minus
			VReg reg = regs.get(ctx.expr());
			prog.addInstr(OpCode.Compute, Operator.Sub, 0, reg, newReg(ctx));
			
			return i;
		} else {
			//not
			VReg reg = regs.get(ctx.expr());
			prog.addInstr(OpCode.Compute, Operator.Xor, TRUE, reg, newReg(ctx));
			
			return i;
		}
	}
	
	@Override
	public Instruction visitAddSubExpr(AddSubExprContext ctx) {
		Operator op = ctx.addSubOp().ADD() != null ? Operator.Add : Operator.Sub;
		
		return genBinaryInstr(op, ctx, ctx.expr(0), ctx.expr(1));
	}
	
	@Override
	public Instruction visitMultDivExpr(MultDivExprContext ctx) {
		Operator op = ctx.multDivOp().MULT() != null ? Operator.Mul : Operator.Div;
		
		return genBinaryInstr(op, ctx, ctx.expr(0), ctx.expr(1));
	}
	
	@Override
	public Instruction visitCompExpr(CompExprContext ctx) {
		Operator op = null;
		
		//LE | LT | GE | GT | EQ | NE;
		if(ctx.compOp().LE() != null) op = Operator.LtE;
		if(ctx.compOp().LT() != null) op = Operator.Lt;
		if(ctx.compOp().GE() != null) op = Operator.GtE;
		if(ctx.compOp().GT() != null) op = Operator.Gt;
		if(ctx.compOp().EQ() != null) op = Operator.Equal;
		if(ctx.compOp().NE() != null) op = Operator.NEq;
		
		return genBinaryInstr(op, ctx, ctx.expr(0), ctx.expr(1));
	}
	
	@Override
	public Instruction visitBoolExpr(BoolExprContext ctx) {
		Operator op = ctx.boolOp().AND() != null ? Operator.And : Operator.Or;
		
		return genBinaryInstr(op, ctx, ctx.expr(0), ctx.expr(1));
	}
	
	/**
	 * Helper function for binary ops
	 */
	private Instruction genBinaryInstr(Operator op, ParserRuleContext expr, ParserRuleContext expr1, ParserRuleContext expr2){
		Instruction i = visit(expr1);
		visit(expr2);
		
		prog.addInstr(OpCode.Compute, op, regs.get(expr1), regs.get(expr2), newReg(expr));
		
		return i;
	}
	
	@Override
	public Instruction visitParExpr(ParExprContext ctx) {
		Instruction i = visit(ctx.expr());
		regs.put(ctx, regs.get(ctx.expr()));
		
		return i;
	}
	
	@Override
	public Instruction visitNumExpr(NumExprContext ctx) {
		int num = Integer.parseInt(ctx.NUM().getText());
		
		return prog.addInstr(OpCode.Const, num, newReg(ctx));
	}
	
	@Override
	public Instruction visitTrueExpr(TrueExprContext ctx) {
		return prog.addInstr(OpCode.Const, TRUE, newReg(ctx));
	}
	
	@Override
	public Instruction visitFalseExpr(FalseExprContext ctx) {
		return prog.addInstr(OpCode.Const, FALSE, newReg(ctx));
	}

	private VReg newReg(ParserRuleContext ctx) {
		VReg reg = new VReg("VReg" + regCounter++);
		regs.put(ctx, reg);
		return reg;
	}
	
	/**
	 * Represents a virtual register
	 */
	private class VReg {
		private String name;
		
		private VReg(String name){
			this.name = name;
		}
	}
}
