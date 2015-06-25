package awesome.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarParser.AddSubExprContext;
import awesome.lang.GrammarParser.ArrayTargetContext;
import awesome.lang.GrammarParser.AssignStatContext;
import awesome.lang.GrammarParser.BlockContext;
import awesome.lang.GrammarParser.BoolExprContext;
import awesome.lang.GrammarParser.CompExprContext;
import awesome.lang.GrammarParser.DeclAssignStatContext;
import awesome.lang.GrammarParser.DoStatContext;
import awesome.lang.GrammarParser.ExprContext;
import awesome.lang.GrammarParser.FalseExprContext;
import awesome.lang.GrammarParser.ForStatContext;
import awesome.lang.GrammarParser.FuncExprContext;
import awesome.lang.GrammarParser.FuncStatContext;
import awesome.lang.GrammarParser.FunctionCallContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.IdTargetContext;
import awesome.lang.GrammarParser.IfStatContext;
import awesome.lang.GrammarParser.MultDivExprContext;
import awesome.lang.GrammarParser.NumExprContext;
import awesome.lang.GrammarParser.ParExprContext;
import awesome.lang.GrammarParser.PrefixExprContext;
import awesome.lang.GrammarParser.PrintStatContext;
import awesome.lang.GrammarParser.ProgramContext;
import awesome.lang.GrammarParser.ReturnStatContext;
import awesome.lang.GrammarParser.StatContext;
import awesome.lang.GrammarParser.TargetExprContext;
import awesome.lang.GrammarParser.TrueExprContext;
import awesome.lang.GrammarParser.VarStatContext;
import awesome.lang.GrammarParser.WhileStatContext;
import awesome.lang.checking.FunctionTable;
import awesome.lang.checking.FunctionTable.Function;
import awesome.lang.checking.SymbolTable;
import awesome.lang.model.*;
import awesome.lang.model.Type.ArrayType;

/**
 * Generates sprockell code, visit methods return the first instruction of the
 * block they generate.
 * 
 * Roughly follows the structure of the grammar.
 */
public class Generator extends GrammarBaseVisitor<Instruction> {
	private static final int TRUE = 1, FALSE = 0;
	
	private static final Reg ARP = Reg.RegE;
	private static final Reg[] REGISTERS = {Reg.RegA, Reg.RegB, Reg.RegC, Reg.RegD};
	
	private ParseTreeProperty<Reg> regs;
	private ArrayList<Reg> freeRegs;
	private HashMap<Function, Label> functionLabels;
	private Program prog;

	private SymbolTable symboltable;
	private FunctionTable funcTable;
	
	public Generator(SymbolTable symboltable, FunctionTable funcTable) {
		this.symboltable = symboltable;
		this.funcTable = funcTable;
	}
	
	public Program genProgram(ParseTree tree) {
		prog = new Program(1);
		freeRegs = new ArrayList<Reg>(Arrays.asList(REGISTERS));
		regs = new ParseTreeProperty<Reg>();
		functionLabels = new HashMap<Function, Label>();
		
		for(int i = 0; i < symboltable.getCurrentScope().getOffset(); i++){
			prog.addInstr(OpCode.Push, Reg.Zero);
		}
		
		tree.accept(this);
		
		if(freeRegs.size() != REGISTERS.length){
			//some function did not free a register it used, but no errors were encountered.
			System.err.println("Non-fatal register leak encountered.");
		}
		
		return prog;
	}
	
	@Override
	public Instruction visitProgram(ProgramContext ctx) {
		Label start = new Label("program-begin");
		prog.addInstr(OpCode.Jump, Target.abs(start));
		
		for(FunctionContext func : ctx.function()){
			visit(func);
		}
		
		for(StatContext stat : ctx.stat()){
			Instruction instr = visit(stat);
			if(start.getInstr() == null) instr.setLabel(start);
		}
		
		//bunch of nops to flush stdio :( :( :( :(
		for(int j = 0; j < 5; j++){
			prog.addInstr(OpCode.Nop);
		}
		
		prog.addInstr(OpCode.EndProg);
		
		return null;
	}
	
	//statements
	
	@Override
	public Instruction visitBlock(BlockContext ctx) {
		Instruction first = null;
		
		for(StatContext stat : ctx.stat()){
			Instruction i = visit(stat);
			if(first == null) first = i;
		}
		
		if(first != null){
			return first;
		} else {
			return prog.addInstr(OpCode.Nop);
		}
	}
	
	@Override
	public Instruction visitVarStat(VarStatContext ctx) {
		return visit(ctx.varSubStat());
	}
	
	@Override
	public Instruction visitAssignStat(AssignStatContext ctx) {
		Instruction targetI = visit(ctx.target());
		
		Reg reg = regs.get(ctx.target());
		assign(ctx.expr(), MemAddr.deref(reg));
		freeReg(reg);
		
		return targetI;
	}
	
	@Override
	public Instruction visitDeclAssignStat(DeclAssignStatContext ctx) {
		return assign(ctx.expr(), MemAddr.direct(symboltable.getOffset(ctx)));
	}
	
	private Instruction assign(ExprContext exprContext, MemAddr addr) {
		Instruction i = visit(exprContext);
		Reg reg = regs.get(exprContext);
		prog.addInstr(OpCode.Store, reg, addr);
		freeReg(reg);
		
		return i;
	}
	
	@Override
	public Instruction visitIfStat(IfStatContext ctx) {
		Instruction i = visit(ctx.expr());
		
		if (ctx.stat().size() > 1) {
			//if-then-else
			
			Label thenLabel = new Label("then"), endLabel = new Label("endif");
			prog.addInstr(OpCode.Branch, regs.get(ctx.expr()), Target.abs(thenLabel));
			visit(ctx.stat(1));
			prog.addInstr(OpCode.Jump, Target.abs(endLabel));
			visit(ctx.stat(0)).setLabel(thenLabel);
			prog.addInstr(endLabel, OpCode.Nop);
		} else {
			Label endLabel = new Label("endif");
			Reg reg = regs.get(ctx.expr());
			prog.addInstr(OpCode.Compute, Operator.Equal, Reg.Zero, reg, reg);//inverse
			prog.addInstr(OpCode.Branch, reg, Target.abs(endLabel));
			visit(ctx.stat(0));
			prog.addInstr(endLabel, OpCode.Nop);
		}
		
		freeReg(ctx.expr());
		
		return i;
	}
	
	@Override
	public Instruction visitForStat(ForStatContext ctx) {
		// init
		Instruction i = visit(ctx.varSubStat(0));
		Instruction check = visit(ctx.expr());
		Label checkLabel  = new Label("for_check");
		Label endLabel    = new Label("for_end");
		
		// check
		check.setLabel(checkLabel);
		Reg reg = regs.get(ctx.expr());
		prog.addInstr(OpCode.Compute,Operator.Equal, Reg.Zero, reg, reg);
		prog.addInstr(OpCode.Branch, reg, Target.abs(endLabel));
		this.freeReg(reg);
		
		// body
		visit(ctx.stat());
		visit(ctx.varSubStat(1));
		
		// check again & endlabel
		prog.addInstr(OpCode.Jump, Target.abs(checkLabel));
		prog.addInstr(endLabel, OpCode.Nop);
		
		return i;
	}
	
	@Override
	public Instruction visitDoStat(DoStatContext ctx) {
		// body
		Instruction i = visit(ctx.stat());
		Label start   = new Label("do_start");
		i.setLabel(start);
		visit(ctx.expr());
		
		// jump back?
		Reg reg = regs.get(ctx.expr());
		prog.addInstr(OpCode.Branch, reg, Target.abs(start));
		this.freeReg(reg);
		
		return i;
	}
	 
	@Override
	public Instruction visitWhileStat(WhileStatContext ctx) {
		Instruction i = visit(ctx.expr());
		Label compLabel = new Label("while");
		i.setLabel(compLabel);
		
		Label endLabel = new Label("endwhile");
		Reg reg = regs.get(ctx.expr());
		prog.addInstr(OpCode.Compute, Operator.Equal, Reg.Zero, reg, reg);//inverse
		prog.addInstr(OpCode.Branch, reg, Target.abs(endLabel));
		visit(ctx.stat());
		prog.addInstr(OpCode.Jump, Target.abs(compLabel));
		prog.addInstr(endLabel, OpCode.Nop);
		
		freeReg(ctx.expr());
		
		return i;
	}
	
	@Override
	public Instruction visitPrintStat(PrintStatContext ctx) {
		//temp, will be a function later
		//only handles numbers between 0 and 9
		Instruction i = visit(ctx.expr());
		
		Reg value = regs.get(ctx.expr());
		Reg valChar = newReg(ctx);
		
		prog.addInstr(OpCode.Const, (int) '0', valChar);
		prog.addInstr(OpCode.Compute, Operator.Add, value, valChar, valChar);
		prog.addInstr(OpCode.Write, valChar, "stdio");

		freeReg(value);
		freeReg(valChar);
		
		return i;
	}
	
	@Override
	public Instruction visitFuncStat(FuncStatContext ctx) {
		Instruction instruction = visit(ctx.functionCall());
		freeReg(ctx.functionCall());
		
		return instruction;
	}
	
	@Override
	public Instruction visitReturnStat(ReturnStatContext ctx) {
		Instruction instruction = visit(ctx.expr());
		Reg exprReg = regs.get(ctx.expr());
		
		Reg temp = newReg();
		
		//set return value
		prog.addInstr(OpCode.Const, -2, temp);
		prog.addInstr(OpCode.Compute, Operator.Add, ARP, temp, temp);
		prog.addInstr(OpCode.Store, exprReg, MemAddr.deref(temp));
		
		//get return address
		prog.addInstr(OpCode.Const, -1, temp);
		prog.addInstr(OpCode.Compute, Operator.Add, ARP, temp, temp);
		prog.addInstr(OpCode.Load, MemAddr.deref(temp), temp);
		prog.addInstr(OpCode.Jump, Target.ind(temp));
		
		freeReg(exprReg);
		freeReg(temp);
		
		return instruction;
	}
	
	//targets
	
	@Override
	public Instruction visitIdTarget(IdTargetContext ctx) {
		//find the assignmentcontext to know the offset of this var
		ParserRuleContext current = ctx;
		do {
			current = current.getParent();
		} while(!(current instanceof AssignStatContext));
		
		int offset = symboltable.getOffset((AssignStatContext) current) + 1;//plus 1 to account for caller's arp
		
		Reg reg = newReg(ctx);
		Instruction first = prog.addInstr(OpCode.Const, offset, reg);
		first.setComment("var " + ctx.ID().getText());
		prog.addInstr(OpCode.Compute, Operator.Add, ARP, reg, reg);
		prog.addInstr(OpCode.Load, MemAddr.deref(reg), reg);
		
		return first;
	}
	
	@Override
	public Instruction visitArrayTarget(ArrayTargetContext ctx) {
		AssignStatContext parent = (AssignStatContext) ctx.getParent();
		Type type = symboltable.getType(parent);
		
		System.out.println(type);
		
		Instruction i = visit(ctx.target());
		Reg reg = regs.get(ctx.target());
		regs.put(ctx, reg);
		visit(ctx.expr());
		Reg exprReg = regs.get(ctx.expr());
		
		//prog.addInstr(OpCode.Const, offset, reg);
		
//		Type subType = type;
		Type subType = ((ArrayType) type).getType();
		if(subType.getSize() != 1) {//no need to multiply by one
			Reg multReg = newReg();
			prog.addInstr(OpCode.Const, subType.getSize(), multReg);
			prog.addInstr(OpCode.Compute, Operator.Mul, exprReg, multReg, exprReg);
			freeReg(multReg);
		}
		
		prog.addInstr(OpCode.Compute, Operator.Sub, reg, exprReg, reg);
		freeReg(exprReg);
		
//		addresses.put(ctx, MemAddr.deref(reg));
		
		return i;
	}
	
	//functions
	
	@Override
	public Instruction visitFunction(FunctionContext ctx) {
		Function func = funcTable.getFunction(ctx);
		Label label = new Label("func " + func.getName());
		
		functionLabels.put(func, label);
		
		Instruction first = visit(ctx.stat());
		first.setLabel(label);
		
		return first;
	}
	
	@Override()
	public Instruction visitFunctionCall(FunctionCallContext ctx) {
		//AR:
		//local var n
		//local var 0
		//param n
		//param 0
		//caller's ARP
		//return address
		//return value
		//RegA .. RegB
		
		Function func = funcTable.getFunction(ctx);
		
		Instruction first = null;
		int stackSize = func.getScope().getOffset();//local vars + params
		
		List<ExprContext> args = ctx.expr();
		
		//reserve space for local variables
		int localSize = func.getScope().getOffset() - args.size();
		for(int i = 0; i < localSize; i++) {
			Instruction instr = prog.addInstr(OpCode.Push, Reg.Zero);
			if(first == null) first = instr;
		}
		
		//parameters
		for(int i = args.size() - 1; i >= 0; i--) {
			ExprContext arg = args.get(i);
			visit(arg);
			
			Reg reg = regs.get(arg);
			Instruction instr = prog.addInstr(OpCode.Push, reg);
			if(first == null) first = instr;
			freeReg(reg);
		}
		
		stackSize += 3;//ARP + ret value + ret addr
		
		//caller's ARP
		Instruction instr = prog.addInstr(OpCode.Push, ARP);
		if(first == null) first = instr;
		
		//write value of stack-pointer to new ARP
		//uses an add for lack reg-to-reg instruction
		prog.addInstr(OpCode.Compute, Operator.Add, Reg.Zero, Reg.SP, ARP);
		
		//return address
		Label returnLabel = new Label("function-return");
		Reg temp = newReg();
		prog.addInstr(OpCode.Const, returnLabel, temp);
		prog.addInstr(OpCode.Push, temp);
		freeReg(temp);
		
		//return value
		prog.addInstr(OpCode.Push, Reg.Zero);
		
		stackSize += REGISTERS.length;
		
		//register save area
		for(Reg reg : REGISTERS) {
			prog.addInstr(OpCode.Push, reg);
		}
		
		Label targetLabel = functionLabels.get(func);
		prog.addInstr(OpCode.Jump, Target.abs(targetLabel));
		
		//get registers back
		for(int i = 0; i < REGISTERS.length; i++) {
			int index = REGISTERS.length - i - 1;
			
			if(i == 0) {
				prog.addInstr(returnLabel, OpCode.Pop, REGISTERS[index]);
			} else {
				prog.addInstr(OpCode.Pop, REGISTERS[index]);
			}
		}
		
		Reg reg = newReg(ctx);
		
		//temporarily use this reg to pop the stack back
		//later use it to get the return value
		prog.addInstr(OpCode.Const, stackSize, reg);
		prog.addInstr(OpCode.Compute, Operator.Sub, Reg.SP, reg, Reg.SP);
		
		prog.addInstr(OpCode.Const, -2, reg);
		prog.addInstr(OpCode.Compute, Operator.Add, ARP, reg, reg);
		prog.addInstr(OpCode.Load, MemAddr.deref(reg), reg);
		
		prog.addInstr(OpCode.Load, MemAddr.deref(ARP), ARP);
		
		return first;
	}
	
	//expressions
	
	@Override
	public Instruction visitPrefixExpr(PrefixExprContext ctx) {
		Instruction i = visit(ctx.expr());
		Reg reg = regs.get(ctx.expr());
		//store the result of this computation in the register of the previous expression
		regs.put(ctx, reg);
		
		if (ctx.prefixOp().SUB() != null) {
			//unary minus
			prog.addInstr(OpCode.Compute, Operator.Sub, 0, reg, reg);
		} else {
			//not
			prog.addInstr(OpCode.Compute, Operator.Equal, Reg.Zero, reg, reg);
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
		Reg reg = regs.get(expr2);
		prog.addInstr(OpCode.Compute, op, regs.get(expr1), regs.get(expr2), reg);
		regs.put(expr, reg);
		
		freeReg(regs.get(expr1));
		
		return i;
	}
	
	@Override
	public Instruction visitParExpr(ParExprContext ctx) {
		Instruction i = visit(ctx.expr());
		regs.put(ctx, regs.get(ctx.expr()));
		
		return i;
	}
	
	@Override
	public Instruction visitTargetExpr(TargetExprContext ctx) {
		Instruction first = visit(ctx.target());
		Reg reg = regs.get(ctx);
		regs.put(ctx, reg);
		prog.addInstr(OpCode.Load, MemAddr.deref(reg), reg);
		
		return first;
	}
	
//	@Override
//	public Instruction visitIdExpr(IdExprContext ctx) {
//		int offset = this.symboltable.getOffset(ctx);
//		return prog.addInstr(OpCode.Load, MemAddr.direct(offset), newReg(ctx));
//	}
//	
//	@Override
//	public Instruction visitArrayExpr(ArrayExprContext ctx) {
//		Instruction i = visit(ctx.expr());
//		
//		Reg reg = newReg(ctx);
//		prog.addInstr(OpCode.Const, symboltable.getOffset(ctx), reg);
//		
//		Reg exprReg = regs.get(ctx.expr());
//		prog.addInstr(OpCode.Compute, Operator.Add, reg, exprReg, reg);
//		prog.addInstr(OpCode.Load, MemAddr.deref(reg), reg);
//		freeReg(exprReg);
//		
//		return i;
//	}
	
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
	
	@Override
	public Instruction visitFuncExpr(FuncExprContext ctx) {
		Instruction instruction = visit(ctx.functionCall());
		regs.put(ctx, regs.get(ctx.functionCall()));
		
		return instruction;
	}
	
	private Reg newReg(ParserRuleContext ctx) {
		Reg reg = newReg();
		regs.put(ctx, reg);
		
		return reg;
	}
	
	private Reg newReg() {
		if(freeRegs.size() == 0){
			throw new IllegalStateException("Out of registers");
		}
		
		Reg reg = freeRegs.remove(0);
		
		return reg;
	}
	
	private void freeReg(ParseTree ctx){
		freeReg(regs.get(ctx));
	}
	
	private void freeReg(Reg reg) {
		if(freeRegs.contains(reg)){
			throw new IllegalArgumentException("Register already freed");
		}
		
		freeRegs.add(reg);
	}
}
