package awesome.lang;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarParser.AddSubExprContext;
import awesome.lang.GrammarParser.AssignStatContext;
import awesome.lang.GrammarParser.BoolExprContext;
import awesome.lang.GrammarParser.CompExprContext;
import awesome.lang.GrammarParser.DeclAssignStatContext;
import awesome.lang.GrammarParser.FalseExprContext;
import awesome.lang.GrammarParser.IdExprContext;
import awesome.lang.GrammarParser.IfStatContext;
import awesome.lang.GrammarParser.MultDivExprContext;
import awesome.lang.GrammarParser.NumExprContext;
import awesome.lang.GrammarParser.ParExprContext;
import awesome.lang.GrammarParser.PrefixExprContext;
import awesome.lang.GrammarParser.ProgramContext;
import awesome.lang.GrammarParser.TrueExprContext;
import awesome.lang.GrammarParser.WhileStatContext;
import awesome.lang.checking.SymbolTable;
import awesome.lang.model.Instruction;
import awesome.lang.model.Label;
import awesome.lang.model.MemAddr;
import awesome.lang.model.OpCode;
import awesome.lang.model.Operator;
import awesome.lang.model.Program;
import awesome.lang.model.Target;

/**
 * Generates sprockell code, visit methods return the first instruction of the
 * block they generate
 */
public class Generator extends GrammarBaseVisitor<Instruction> {
	private static final int TRUE = 1, FALSE = 0;
	
	private ParseTreeProperty<VReg> regs;
	private int regCounter = 0;//virtual register counter
	private Program prog;

	private SymbolTable symboltable;
	
	public Generator(SymbolTable symboltable) {
		this.symboltable = symboltable;
	}
	
	public Program genProgram(ParseTree tree) {
		prog = new Program(1);
		regCounter = 0;
		regs = new ParseTreeProperty<VReg>();
		
		tree.accept(this);
		
		return prog;
	}
	
	@Override
	public Instruction visitProgram(ProgramContext ctx) {
		Instruction i = visit(ctx.block());
		
		prog.addInstr(OpCode.EndProg);
		
		return i;
	}
	
	@Override
	public Instruction visitAssignStat(AssignStatContext ctx) {
		int offset = this.symboltable.getOffset(ctx);                   
		                                  
		Instruction i = visit(ctx.expr());
		prog.addInstr(OpCode.Store, regs.get(ctx.expr()), MemAddr.direct(offset));
		                                  
		return i;                         
	}                                     
	                                      
	@Override                             
	public Instruction visitDeclAssignStat(DeclAssignStatContext ctx) {
		int offset = this.symboltable.getOffset(ctx);                   
		                                  
		Instruction i = visit(ctx.expr());
		prog.addInstr(OpCode.Store, regs.get(ctx.expr()), MemAddr.direct(offset));
		
		return i;
	}
	
	@Override
	public Instruction visitIfStat(IfStatContext ctx) {
		Instruction i = visit(ctx.expr());
		
		if (ctx.stat().size() > 1) {
			//if-then-else
			
			Label thenLabel = new Label("then"), endLabel = new Label("endif");
			prog.addInstr(OpCode.Branch, regs.get(ctx.expr()), thenLabel);
			visit(ctx.stat(1));
			prog.addInstr(OpCode.Jump, endLabel);
			visit(ctx.stat(0)).setLabel(thenLabel);
			prog.addInstr(endLabel, OpCode.Nop);
		} else {
			Label endLabel = new Label("endif");
			prog.addInstr(OpCode.Branch, regs.get(ctx.expr()), endLabel);
			visit(ctx.stat(0));
			prog.addInstr(endLabel, OpCode.Nop);
		}
		
		return i;
	}
	
	@Override
	public Instruction visitWhileStat(WhileStatContext ctx) {
		Instruction i = visit(ctx.expr());
		Label compLabel = new Label("while");
		i.setLabel(compLabel);
		
		Label endLabel = new Label("endwhile");
		prog.addInstr(OpCode.Branch, regs.get(ctx.expr()), endLabel);
		visit(ctx.stat());
		prog.addInstr(OpCode.Jump, Target.abs(compLabel));
		prog.addInstr(endLabel, OpCode.Nop);
		
		return i;
	}
	
	//expressions
	
	@Override
	public Instruction visitPrefixExpr(PrefixExprContext ctx) {
		Instruction i = visit(ctx.expr());
		VReg reg = regs.get(ctx.expr());
		//store the result of this computation in the register of the previous expression
		regs.put(ctx, reg);
		
		if (ctx.prefixOp().SUB() != null) {
			//unary minus
			prog.addInstr(OpCode.Compute, Operator.Sub, 0, reg, reg);
		} else {
			//not
			prog.addInstr(OpCode.Compute, Operator.NEq, TRUE, reg, reg);
		}
		
		return i;
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
		if (ctx.compOp().LE() != null)
			op = Operator.LtE;
		if (ctx.compOp().LT() != null)
			op = Operator.Lt;
		if (ctx.compOp().GE() != null)
			op = Operator.GtE;
		if (ctx.compOp().GT() != null)
			op = Operator.Gt;
		if (ctx.compOp().EQ() != null)
			op = Operator.Equal;
		if (ctx.compOp().NE() != null)
			op = Operator.NEq;
		
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
	private Instruction genBinaryInstr(Operator op, ParserRuleContext expr, ParserRuleContext expr1,
			ParserRuleContext expr2) {
		Instruction i = visit(expr1);
		visit(expr2);
		
		//put the result of this binary operation in the reg of expr2
		//since it's not needed anymore
		VReg reg = regs.get(expr2);
		prog.addInstr(OpCode.Compute, op, regs.get(expr1), regs.get(expr2), reg);
		regs.put(expr, reg);
		
		return i;
	}
	
	@Override
	public Instruction visitParExpr(ParExprContext ctx) {
		Instruction i = visit(ctx.expr());
		regs.put(ctx, regs.get(ctx.expr()));
		
		return i;
	}
	
	@Override
	public Instruction visitIdExpr(IdExprContext ctx) {
		int offset = this.symboltable.getOffset(ctx);
		return prog.addInstr(OpCode.Load, MemAddr.direct(offset), newReg(ctx));
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
		
		private VReg(String name) {
			this.name = name;
		}
	}
}
