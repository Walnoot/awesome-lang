package awesome.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import awesome.lang.GrammarParser.DeclStatContext;
import awesome.lang.GrammarParser.DoStatContext;
import awesome.lang.GrammarParser.EnumExprContext;
import awesome.lang.GrammarParser.ExprContext;
import awesome.lang.GrammarParser.FalseExprContext;
import awesome.lang.GrammarParser.ForStatContext;
import awesome.lang.GrammarParser.FuncExprContext;
import awesome.lang.GrammarParser.FuncStatContext;
import awesome.lang.GrammarParser.FunctionCallContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.IdTargetContext;
import awesome.lang.GrammarParser.IfStatContext;
import awesome.lang.GrammarParser.ModExprContext;
import awesome.lang.GrammarParser.MultDivExprContext;
import awesome.lang.GrammarParser.NextStatContext;
import awesome.lang.GrammarParser.NumExprContext;
import awesome.lang.GrammarParser.ParExprContext;
import awesome.lang.GrammarParser.PrefixExprContext;
import awesome.lang.GrammarParser.ReadExprContext;
import awesome.lang.GrammarParser.ReturnStatContext;
import awesome.lang.GrammarParser.StatContext;
import awesome.lang.GrammarParser.SwitchStatContext;
import awesome.lang.GrammarParser.TargetContext;
import awesome.lang.GrammarParser.TargetExprContext;
import awesome.lang.GrammarParser.TrueExprContext;
import awesome.lang.GrammarParser.VarStatContext;
import awesome.lang.GrammarParser.WhileStatContext;
import awesome.lang.GrammarParser.WriteStatContext;
import awesome.lang.checking.FunctionTable;
import awesome.lang.checking.FunctionTable.Function;
import awesome.lang.checking.SymbolTable;
import awesome.lang.model.*;

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
	private HashMap<Function, Integer> threadIdMap;//assigns an unique int to every thread function
	private Program prog;

	private SymbolTable symboltable;
	private FunctionTable funcTable;

	private Label nextSwitchLabel = null;

	private int staticBlockStart;
	
	public Generator(SymbolTable symboltable, FunctionTable funcTable) {
		this.symboltable = symboltable;
		this.funcTable = funcTable;
	}
	
	public Program genProgram(ArrayList<FunctionContext> functions, ArrayList<StatContext> statements) {
		prog = new Program(1);
		freeRegs = new ArrayList<Reg>(Arrays.asList(REGISTERS));
		regs = new ParseTreeProperty<Reg>();
		functionLabels = new HashMap<Function, Label>();
		
		staticBlockStart = 0xFFFFFF - symboltable.getCurrentScope().getOffset();
		
		visitProgram(functions, statements);
		
		if(freeRegs.size() != REGISTERS.length) {
			//some function did not free a register it used, but no errors were encountered.
			System.err.println("Non-fatal register leak encountered.");
		}
		
		return prog;
	}
	
	public void visitProgram(ArrayList<FunctionContext> functions, ArrayList<StatContext> statements) {
		Label start = new Label("program-begin");
		
//		for(int i = 0; i < symboltable.getCurrentScope().getOffset(); i++){
//			prog.addInstr(OpCode.Push, Reg.Zero);
//		}
		
		//set initial ARP
		prog.addInstr(OpCode.Compute, Operator.Add, Reg.Zero, Reg.SP, ARP);
		
		ArrayList<Function> threads = new ArrayList<Function>();
		int threadCounter = 1;
		
		//prepare function labels
		for(FunctionContext f : functions){
			Function func = funcTable.getFunction(f);
			Label label = new Label("func " + func.getName());
			
			functionLabels.put(func, label);
			
			if(func.isThreadFunction()){
				threads.add(func);
				threadIdMap.put(func, staticBlockStart - threadCounter);
				threadCounter++;
			}
		}
		
		Reg reg = newReg();
		for(int i = 0; i <= threads.size(); i++){
			prog.addInstr(OpCode.Const, i, reg);
			prog.addInstr(OpCode.Compute, Operator.Equal, reg, Reg.SPID, reg);
			if(i == 0){
				//main thread
				prog.addInstr(OpCode.Branch, reg, Target.abs(start));
			}else{
				//'sub' thread
				Function function = threads.get(i-1);
				prog.addInstr(OpCode.Branch, reg, Target.abs(functionLabels.get(function)));
			}
		}
		freeReg(reg);
		
		for(FunctionContext func : functions){
			visit(func);
		}
		
		prog.addInstr(start, OpCode.Nop);
		
		for(StatContext stat : statements){
			visit(stat);
		}
		
		//bunch of nops to flush stdio :( :( :( :(
		for(int j = 0; j < 5; j++){
			prog.addInstr(OpCode.Nop);
		}
		
		prog.addInstr(OpCode.EndProg);
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
		assign(ctx.expr(), MemAddr.deref(reg), isGlobal(ctx.target()));
		freeReg(reg);
		
		return targetI;
	}
	
	@Override
	public Instruction visitDeclAssignStat(DeclAssignStatContext ctx) {
		Reg reg = newReg();
		Instruction instr = genAddr(symboltable.isGlobal(ctx), symboltable.getOffset(ctx), reg);
//		Instruction instr = prog.addInstr(OpCode.Const, symboltable.getOffset(ctx) + 1, reg);
		instr.setComment("var " + ctx.ID().getText());
//		prog.addInstr(OpCode.Compute, Operator.Add, ARP, reg, reg);
		
		assign(ctx.expr(), MemAddr.deref(reg), symboltable.isGlobal(ctx));
		freeReg(reg);
		
		return instr;
	}
	
	@Override
	public Instruction visitDeclStat(DeclStatContext ctx) {
		return prog.addInstr(OpCode.Nop);
	}
	
	private Instruction assign(ExprContext exprContext, MemAddr addr, boolean global) {
		Instruction i = visit(exprContext);
		Reg reg = regs.get(exprContext);
		
		if(global) {
			prog.addInstr(OpCode.Write, reg, addr);
		} else {
			prog.addInstr(OpCode.Store, reg, addr);
		}
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
			freeReg(ctx.expr());

			visit(ctx.stat(1));
			prog.addInstr(OpCode.Jump, Target.abs(endLabel));
			visit(ctx.stat(0)).setLabel(thenLabel);
			prog.addInstr(endLabel, OpCode.Nop);
		} else {
			Label endLabel = new Label("endif");
			Reg reg = regs.get(ctx.expr());
			prog.addInstr(OpCode.Compute, Operator.Equal, Reg.Zero, reg, reg);//inverse
			prog.addInstr(OpCode.Branch, reg, Target.abs(endLabel));
			freeReg(ctx.expr());
			
			visit(ctx.stat(0));
			prog.addInstr(endLabel, OpCode.Nop);
		}
		
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
	
	//TODO: Push regCheck on stack when visiting a child or document otherwise
	@Override
	public Instruction visitSwitchStat(SwitchStatContext ctx) {
		Instruction res = visit(ctx.expr(0));
		Reg regCheck = regs.get(ctx.expr(0));

		Label[] checkLabels = new Label[ctx.expr().size()+(ctx.DEFAULT() != null ? 1 : 0)]; // check if expression is valid (Not equal comparison) (including default and endlabel)
		Label[] blockLabels = new Label[ctx.block().size()];  // execute next block (next) 
		for (int i = 0; i < checkLabels.length; i++)
			checkLabels[i] = new Label("Switch-statement expr: " + i);
		for (int i = 0; i < blockLabels.length-1; i++)
			blockLabels[i] = new Label("Switch-statement block: " + i);
		blockLabels[blockLabels.length-1] = new Label("Switch-statement end");
		
		for (int i = 1; i < ctx.expr().size(); i++) {
			// fix label, execute expression, compute branching
			visit(ctx.expr(i)).setLabel(checkLabels[i-1]);
			Reg regCompare = regs.get(ctx.expr(i));
			prog.addInstr(OpCode.Compute, Operator.NEq, regCheck, regCompare, regCompare);
			prog.addInstr(OpCode.Branch, regCompare, Target.abs(checkLabels[i]));
			// free reg, set next label and visit children
			if (i < ctx.expr().size() - 1 || ctx.DEFAULT() != null)
				this.nextSwitchLabel = blockLabels[i];
			else
				this.nextSwitchLabel = null;
			freeReg(regCompare);
			visit(ctx.block(i-1)).setLabel(blockLabels[i-1]);
			// jump to end
			prog.addInstr(OpCode.Jump, Target.abs(checkLabels[checkLabels.length-1]));
		}
		// clear regcheck
		freeReg(regCheck);
		//default?
		if (ctx.DEFAULT() != null) {
			prog.addInstr(OpCode.Nop).setLabel(checkLabels[checkLabels.length - 2]);
			visit(ctx.block(ctx.block().size() - 1)).setLabel(blockLabels[blockLabels.length - 1]);
		}
		// label of end instruction
		prog.addInstr(checkLabels[checkLabels.length - 1], OpCode.Nop);
		this.nextSwitchLabel = null;
		return res;
	}
	
	@Override
	public Instruction visitNextStat(NextStatContext ctx) {
		return prog.addInstr(OpCode.Jump, Target.abs(this.nextSwitchLabel));
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
		freeReg(ctx.expr());
		
		visit(ctx.stat());
		prog.addInstr(OpCode.Jump, Target.abs(compLabel));
		prog.addInstr(endLabel, OpCode.Nop);
		
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
		instruction.setComment("return-expr");
		Reg exprReg = regs.get(ctx.expr());
		
		makeReturn(exprReg);
		
		freeReg(exprReg);
		
		return instruction;
	}
	
	private void makeReturn(Reg reg) {
		Reg temp = newReg();
		
		//set return value
		prog.addInstr(OpCode.Const, -2, temp);
		prog.addInstr(OpCode.Compute, Operator.Add, ARP, temp, temp);
		prog.addInstr(OpCode.Store, reg, MemAddr.deref(temp));
		
		//get return address
		prog.addInstr(OpCode.Const, -1, temp);
		prog.addInstr(OpCode.Compute, Operator.Add, ARP, temp, temp);
		prog.addInstr(OpCode.Load, MemAddr.deref(temp), temp);
		prog.addInstr(OpCode.Jump, Target.ind(temp));
		
		freeReg(temp);
	}
	
	@Override
	public Instruction visitWriteStat(WriteStatContext ctx) {
		Instruction i = visit(ctx.expr(0));
		visit(ctx.expr(1));
		
		prog.addInstr(OpCode.Write, regs.get(ctx.expr(0)), MemAddr.deref(regs.get(ctx.expr(1))));
		freeReg(ctx.expr(0));
		freeReg(ctx.expr(1));
		
		return i;
	}
	
	/**
	 * Generates code to put the address of a variable with given offset in the register
	 */
	private Instruction genAddr(boolean global, int offset, Reg reg){
		if(!global){
			Instruction instr = prog.addInstr(OpCode.Const, offset + 1, reg);
			prog.addInstr(OpCode.Compute, Operator.Add, ARP, reg, reg);
			return instr;
		} else {
			Instruction instr = prog.addInstr(OpCode.Const, offset + staticBlockStart, reg);
			return instr;
		}
	}
	
	//targets
	
	@Override
	public Instruction visitIdTarget(IdTargetContext ctx) {
		Reg reg = newReg(ctx);
		
		Instruction i = genAddr(symboltable.isGlobal(ctx), symboltable.getOffset(ctx), reg);
		i.setComment("var: " + ctx.getText());
		return i;
	}
	
	@Override
	public Instruction visitArrayTarget(ArrayTargetContext ctx) {
		Type type = symboltable.getType(ctx);
		
		Instruction i = visit(ctx.target());
		Reg reg = regs.get(ctx.target());
		regs.put(ctx, reg);
		visit(ctx.expr());
		Reg exprReg = regs.get(ctx.expr());
		
//		Type subType = ((ArrayType) type).getType();
		if(type.getSize() != 1) {//no need to multiply by one
			Reg multReg = newReg();
			prog.addInstr(OpCode.Const, type.getSize(), multReg);
			prog.addInstr(OpCode.Compute, Operator.Mul, exprReg, multReg, exprReg);
			freeReg(multReg);
		}

//		prog.addInstr(OpCode.Compute, isGlobal(ctx) ? Operator.Add : Operator.Sub, reg, exprReg, reg);
		prog.addInstr(OpCode.Compute, Operator.Add, reg, exprReg, reg);
		freeReg(exprReg);
		
		return i;
	}
	
	private boolean isGlobal(TargetContext ctx) {
		if(ctx instanceof IdTargetContext){
			return symboltable.isGlobal(ctx);
		} else if(ctx instanceof ArrayTargetContext) {
			return isGlobal(((ArrayTargetContext) ctx).target());
		}
		
		return false;
	}
	
	//functions
	
	@Override
	public Instruction visitFunction(FunctionContext ctx) {
		Function function = funcTable.getFunction(ctx);
		Label label = functionLabels.get(function);
		Instruction first;
		
		if (ctx.stat() != null) {
			first = visit(ctx.stat());
			if(function.getFunctionType().getReturnType() == Type.VOID){
				makeReturn(Reg.Zero);
			}
		} else {
			first = visit(ctx.expr());
			Reg retReg = this.regs.get(ctx.expr());
			makeReturn(retReg);
			freeReg(retReg);
		}
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
		
		//local vars + params + ARP + ret value + ret addr
		//register saves dont count
		int stackSize = func.getScope().getOffset() + 3;
		
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
			Instruction instr = visit(arg);
			
			Reg reg = regs.get(arg);
			prog.addInstr(OpCode.Push, reg);
			if(first == null) first = instr;
			freeReg(reg);
		}
		
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
		
		//register save area
		//we save every register that is currently used, i.e. every reg that's not on the free list
		ArrayList<Reg> savedRegs = new ArrayList<Reg>();
		for (Reg reg : REGISTERS) {
			if (!freeRegs.contains(reg)) {
				prog.addInstr(OpCode.Push, reg);
				savedRegs.add(reg);
			}
		}
		
		Label targetLabel = functionLabels.get(func);
		prog.addInstr(OpCode.Jump, Target.abs(targetLabel));
		
		prog.addInstr(returnLabel, OpCode.Nop);
		
		//get registers back
		Collections.reverse(savedRegs);
		for(Reg reg : savedRegs) {
			prog.addInstr(OpCode.Pop, reg);
		}
		
		Reg reg = newReg(ctx);
		
//		prog.addInstr(OpCode.Pop, reg);//return value
//		
//		prog.addInstr(OpCode.Pop, Reg.Zero);//return address
//		prog.addInstr(OpCode.Pop, Reg.Zero);//callers arp
//		for(int i = 0; i < func.getScope().getOffset(); i++) {
//			prog.addInstr(OpCode.Pop, Reg.Zero);
//		}
		
		//push stack back
		prog.addInstr(OpCode.Const, stackSize, reg);
		prog.addInstr(OpCode.Compute, Operator.Add, Reg.SP, reg, Reg.SP);
		
		//load return value
		prog.addInstr(OpCode.Const, -2, reg);
		prog.addInstr(OpCode.Compute, Operator.Add, ARP, reg, reg);
		prog.addInstr(OpCode.Load, MemAddr.deref(reg), reg);
		
		//set old ARP back
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
			prog.addInstr(OpCode.Compute, Operator.Sub, Reg.Zero, reg, reg);
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
	public Instruction visitModExpr(ModExprContext ctx) {
		return genBinaryInstr(Operator.Mod, ctx, ctx.expr(0), ctx.expr(1));
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
		Reg reg = regs.get(ctx.target());
		regs.put(ctx, reg);
		
		if(isGlobal(ctx.target())) {
			prog.addInstr(OpCode.Read, MemAddr.deref(reg));
			prog.addInstr(OpCode.Receive, reg);
		} else {
			prog.addInstr(OpCode.Load, MemAddr.deref(reg), reg);
		}
		
		return first;
	}
	
	@Override
	public Instruction visitNumExpr(NumExprContext ctx) {
		int num = Integer.parseInt(ctx.NUM().getText());
		
		return prog.addInstr(OpCode.Const, num, newReg(ctx));
	}

	@Override
	public Instruction visitEnumExpr(EnumExprContext ctx) {
		int num = Type.getEnum(ctx.ID(0).getText()).getValue(ctx.ID(1).getText());
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
	
	@Override
	public Instruction visitReadExpr(ReadExprContext ctx) {
		Instruction i = visit(ctx.expr());
		Reg reg = regs.get(ctx.expr());
		prog.addInstr(OpCode.Read, reg);
		prog.addInstr(OpCode.Receive, reg);
		regs.put(ctx, reg);
		
		return i;
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
		if(reg == null) throw new NullPointerException();
		
		if(freeRegs.contains(reg)){
			throw new IllegalArgumentException("Register already freed");
		}
		
		freeRegs.add(reg);
	}
}
