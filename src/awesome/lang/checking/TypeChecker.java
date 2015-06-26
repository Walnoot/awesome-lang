package awesome.lang.checking;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.*;
import awesome.lang.GrammarParser.TypeContext;
import awesome.lang.GrammarParser.*;
import awesome.lang.checking.FunctionTable.Function;
import awesome.lang.model.Type;
import awesome.lang.model.Type.ArrayType;
import awesome.lang.model.Type.FunctionType;

public class TypeChecker extends GrammarBaseVisitor<Void> {
	private ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
	private ParseTreeProperty<Boolean> blockNewScope = new ParseTreeProperty<Boolean>();
	private ArrayList<String> errors 	  = new ArrayList<String>();
	private SymbolTable variables		  = new SymbolTable();
	private FunctionTable functions		  = new FunctionTable();
	private Type returnType				  = null;
	private Boolean inSwitch			  = Boolean.FALSE;

	private void addError(String string, ParserRuleContext ctx) {
		
		Token token = ctx.getStart();
		String error = string.replace("{expr}", "\"" + ctx.getText() + "\"");
		this.errors.add(error + " (line "+token.getLine() + ":" + token.getCharPositionInLine() + ")");
		
	}

	public ArrayList<String> getErrors() {
		return new ArrayList<String> (this.errors);
	}

	public SymbolTable getSymbolTable() {
		return variables;
	}
	public FunctionTable getFunctionTable() {
		return functions;
	}
	
	public void checkProgram(ArrayList<FunctionContext> functions, ArrayList<StatContext> statements) {
		// function definitions
		for(FunctionContext child : functions) {
			visit(child);
		}
		
		for(StatContext stat : statements){
			visit(stat);
		}
		
		for(FunctionContext child : functions) {
			// open scope (add local variables)
			this.variables.openScope(((FunctionContext) child), true);
			for(ArgumentContext arg : ((FunctionContext) child).argument()) {
				// arg is already visitited in visitFunction
				this.variables.add(arg, this.types.get(arg));
			}
			// add scope to function
			Function func = this.functions.getFunction((FunctionContext) child);
			func.setScope(this.variables.getCurrentScope());
			// set return type, for return statements
			this.returnType = func.getFunctionType().getReturnType();
			
			// execute stat/subscopes
			visit(((FunctionContext) child).stat());
			
			// reset return type and finalize
			this.returnType = null;
			this.variables.closeScope();
		}
	}
	
	@Override
	public Void visitReturnStat(ReturnStatContext ctx) {
		
		if (this.returnType == null) {
			this.addError("Returning outside a function in expression: {expr}", ctx);
			return null;
		}
		
		visit(ctx.expr());
		if (this.returnType.equals(this.types.get(ctx.expr())) == false) {
				this.addError("Type of return statement is different from function definition in expression: {expr}", ctx);
		}
		
		return null;
	}

	@Override
	public Void visitBlock(BlockContext ctx) {
		//handles null as well
		if (Boolean.TRUE.equals(this.blockNewScope.get(ctx)))
			return null;
		
		this.variables.openScope(ctx);
		
		for(StatContext stat : ctx.stat()) {
			visit(stat);
		}
		
		this.variables.closeScope();
		
		return null;
	}
	
	@Override 
	public Void visitFunction(FunctionContext ctx) {
		// only function definition, contents are evaluated in visitProgram.
		TypeContext typeExpr = ctx.type();
		if(typeExpr != null) visit(typeExpr);
		for (ParserRuleContext child : ctx.argument()) {
			visit(child);
		}
		
		Type retType = typeExpr == null ? Type.VOID : this.types.get(typeExpr);
		
		if(retType != Type.VOID && !hasReturn(ctx.stat())){
			addError("Function " + ctx.ID().getText() + " does not return properly", ctx);
		}
		
		Type[] argTypes = new Type[ctx.argument().size()];
		for (int i = 0; i < ctx.argument().size(); i++) {
			argTypes[i] = this.types.get(ctx.argument(i));
		}
		
		FunctionType fType = Type.function(retType, argTypes); 
		String name  	   = ctx.ID().getText(); 
		if (this.functions.containsWithArgs(name, fType)) {
			this.addError("Double function definition with the same arguments in expression: {expr}", ctx);
		} else {
			this.functions.addFunction(name, fType);
		}
		
		this.functions.addContextToFunction(ctx, fType);
		
		return null;
	}

	@Override
	public Void visitFunctionCall(FunctionCallContext ctx) {
		
		// get argument types
		Type[] args = new Type[ctx.expr().size()];
		for(int i = 0; i < ctx.expr().size(); i++) {
			visit(ctx.expr(i));
			args[i] = this.types.get(ctx.expr(i));
		}
		String name = ctx.ID().getText();
		
		FunctionType ftype = this.functions.getFunctionTypeByArgs(name, args);
		if (ftype == null) {
			this.addError("Function call to unknown function in expression: {expr}", ctx);
		} else {
			this.types.put(ctx, ftype.getReturnType());
			this.functions.addContextToFunction(ctx, ftype);
		}
		
		return null;
	}
	
	@Override 
	public Void visitArgument(ArgumentContext ctx) {
		visit(ctx.type());
		this.types.put(ctx, this.types.get(ctx.type()));
		return null;
	}
	
	@Override
	public Void visitIntType(IntTypeContext ctx) {
		types.put(ctx, Type.INT);
		return null;
	}
	
	@Override
	public Void visitBoolType(BoolTypeContext ctx) {
		types.put(ctx, Type.BOOL);
		return null;
	}
	
	@Override
	public Void visitArrayType(ArrayTypeContext ctx) {
		visit(ctx.type());
		Type type = types.get(ctx.type());
		int size = Integer.parseInt(ctx.NUM().getText());
		
		types.put(ctx, Type.array(type, size));
		return null;
	}
	@Override
	public Void visitDeclAssignStat(DeclAssignStatContext ctx) {
		// add new variable to scope (with value)
		visit(ctx.type());
		visit(ctx.expr());
		Type type = types.get(ctx.type());
		Type exprtype = this.types.get(ctx.expr());
		
		if (this.variables.add(ctx, type) == false) {
			this.addError("Redeclaration of variable "+ctx.ID().getText()+" in expression: {expr}", ctx);
		}
		if (type.equals(exprtype) == false) {
			this.addError("type of variable "+ctx.ID().getText()+" is not equal to type of expression: {expr}", ctx);
		}
		return null;
	}
	
	@Override
	public Void visitDeclStat(DeclStatContext ctx) {
		// add new variable to scope (uninitialized)
		visit(ctx.type());
		Type type = types.get(ctx.type());
		if (this.variables.add(ctx, type) == false) {
			this.addError("Redeclaration of variable "+ctx.ID().getText()+" in expression: {expr}", ctx);
		}
		return null;
	}

	@Override
	public Void visitAssignStat(AssignStatContext ctx) {
		visit(ctx.expr());
		visit(ctx.target());
		
		Type exptype = this.types.get(ctx.expr());
		Type vartype = this.types.get(ctx.target());

		if (!vartype.equals(exptype)) {
			this.addError("Assignment of type "+exptype.toString()+" to variable of type "+vartype.toString()+" in expression {expr}", ctx);
		} else {
			this.variables.assign(ctx);
		}
		return null;
	}

	@Override
	public Void visitForStat(ForStatContext ctx) {
		visit(ctx.varSubStat(0));
		visit(ctx.expr());
		if (this.types.get(ctx.expr()) != Type.BOOL) {
			this.addError("Expression in for-statement does not return boolean: {expr}", ctx);
		}
		visit(ctx.varSubStat(1));
		visit(ctx.stat());
		return null;
	}

	@Override
	public Void visitDoStat(DoStatContext ctx) {
		visit(ctx.expr());
		if (this.types.get(ctx.expr()) != Type.BOOL) {
			this.addError("Expression in do-until-statement does not return boolean: {expr}", ctx);
		}
		visit(ctx.stat());
		return null;
	}
	
	@Override
	public Void visitWhileStat(WhileStatContext ctx) {
		visit(ctx.expr());
		if (this.types.get(ctx.expr()) != Type.BOOL) {
			this.addError("Expression in while-statement does not return boolean: {expr}", ctx);
		}
		visit(ctx.stat());
		return null;
	}
	
	@Override
	public Void visitSwitchStat(SwitchStatContext ctx) {
		visit(ctx.expr(0));
		Type resType = this.types.get(ctx.expr(0)); 
		for (int i = 1; i < ctx.expr().size(); i++) {
			visit(ctx.expr(i));
			if (resType.equals(this.types.get(ctx.expr(i))) == false)
				this.addError("Type of case does not match the switch-type in expression: {expr}", ctx);
		}
		
		this.inSwitch = Boolean.TRUE;
		for (int i = 0; i < ctx.block().size()-1; i++) {
			visit(ctx.block(i));
		}
		this.inSwitch = null;
		if (ctx.block().size() > 0)
			visit(ctx.block(ctx.block().size()-1));
		
		this.inSwitch = Boolean.FALSE;
		return null;
	}
	
	@Override
	public Void visitNextStat(NextStatContext ctx) {
		if (this.inSwitch == null) {
			this.addError("Next statement in last switch-block, there is no next? In expression: {expr}", ctx);
		}
		else if (Boolean.FALSE.equals(this.inSwitch)) {
			this.addError("Next statement outside a switch-block in expression: {expr}", ctx); 
		}
		return null;
	}

	@Override
	public Void visitIfStat(IfStatContext ctx) {
		visit(ctx.expr());
		if (this.types.get(ctx.expr()) != Type.BOOL) {
			this.addError("Expression in if-statement does not return boolean: {expr}", ctx);
		}
		visit(ctx.stat(0));
		if (ctx.stat().size() > 1)
			visit(ctx.stat(1));
		return null;
	}
	
	@Override
	public Void visitWriteStat(WriteStatContext ctx) {
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		
		if(types.get(ctx.expr(0)) != Type.INT || types.get(ctx.expr(1)) != Type.INT) {
			addError("Expressions in write statement must be of type int: {expr}", ctx);
		}
		
		return null;
	}
	
	@Override
	public Void visitReadExpr(ReadExprContext ctx) {
		visit(ctx.expr());
		
		if(types.get(ctx.expr()) != Type.INT) {
			addError("Expressions in read expression must be of type int: {expr}", ctx);
		}
		
		types.put(ctx, Type.INT);
		
		return null;
	}
	
	@Override
	public Void visitPrefixExpr(PrefixExprContext ctx) {
		visit(ctx.expr());
		// not statement
		if (ctx.prefixOp().NOT() != null) {
			if (this.types.get(ctx.expr()) != Type.BOOL){
				this.addError("Subexpression is not of type boolean in {expr}", ctx);
			}
			this.types.put(ctx, Type.BOOL);
			
		// unary minus
		} else {
			if(this.types.get(ctx.expr()) != Type.INT){
				this.addError("Subexpression is not of type integer in {expr}", ctx);
			}
			this.types.put(ctx, Type.INT);
		}
		return null;
		
		
	}

	@Override
	public Void visitBoolExpr(BoolExprContext ctx) {
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		// valid types?
		if (this.types.get(ctx.expr(0)) != Type.BOOL){
			this.addError("First child of expression: {expr} is no boolean", ctx);
		} else if(this.types.get(ctx.expr(1)) != Type.BOOL) {
			this.addError("Second child of expression: {expr} is no boolean", ctx);
		}
		
		this.types.put(ctx, Type.BOOL);
		return null;
		
	}

	@Override
	public Void visitCompExpr(CompExprContext ctx) {
		ExprContext child1 = ctx.expr(0);
		ExprContext child2 = ctx.expr(1);
		visit(child1);
		visit(child2);
		// valid types?
		if (this.types.get(child1) != this.types.get(child2)) {
			this.addError("Comparing "+this.types.get(child1)+" with "+this.types.get(child2) + " in {expr}", ctx);
		} else if (this.types.get(child1) == Type.INT) {
			this.types.put(ctx, Type.BOOL);
		} else if (ctx.compOp().EQ() == null && ctx.compOp().NE() == null) {// bool comparison with a wrong operand
			this.addError("Doing an impossible comparison on two booleans: {expr}", ctx);
		}
		
		this.types.put(ctx, Type.BOOL);
		return null;
		
	}

	@Override
	public Void visitMultDivExpr(MultDivExprContext ctx) {
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		// valid types? 
		if (this.types.get(ctx.expr(0)) != Type.INT){
			this.addError("First child of expression: {expr} is no integer", ctx);
		} else if (this.types.get(ctx.expr(1)) != Type.INT) {
			this.addError("Second child of expression: {expr} is no integer", ctx);
		}
		
		this.types.put(ctx, Type.INT);
		return null;
		
	}

	@Override
	public Void visitAddSubExpr(AddSubExprContext ctx) {
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		// valid types?
		if (this.types.get(ctx.expr(0)) != Type.INT){
			this.addError("First child of expression: {expr} is no integer", ctx);
		} else if (this.types.get(ctx.expr(1)) != Type.INT) {
			this.addError("Second child of expression: {expr} is no integer", ctx);
		}
		
		this.types.put(ctx, Type.INT);
		return null;
		
	}

	@Override
	public Void visitFuncExpr(FuncExprContext ctx) {
		visit(ctx.functionCall());
		this.types.put(ctx, this.types.get(ctx.functionCall()));
		return null;
	}
	

	@Override
	public Void visitParExpr(ParExprContext ctx) {
		visit(ctx.expr());
		this.types.put(ctx, this.types.get(ctx.expr()));
		return null;
	}

	@Override
	public Void visitNumExpr(NumExprContext ctx) {
		this.types.put(ctx, Type.INT);
		return null;
	}

	@Override
	public Void visitFalseExpr(FalseExprContext ctx) {
		this.types.put(ctx, Type.BOOL);
		return null;
	}
	
	@Override
	public Void visitTrueExpr(TrueExprContext ctx) {
		this.types.put(ctx, Type.BOOL);
		return null;
	}
	
	@Override
	public Void visitTargetExpr(TargetExprContext ctx) {
		visit(ctx.target());
		this.types.put(ctx, this.types.get(ctx.target()));
		return null;
	}
	
	@Override
	public Void visitArrayTarget(ArrayTargetContext ctx) {
		visit(ctx.target());
		visit(ctx.expr());
		
		if (this.types.get(ctx.expr()) != Type.INT) {
			this.addError("Using a non-integer index in expression: {expr}", ctx);
		}
		
		Type aType = this.types.get(ctx.target());
		if (aType instanceof ArrayType) {
			aType = ((ArrayType) aType).getType();
		} else {
			this.addError("Taking index of a non-array variable in expression: {expr}", ctx);
		}
		
		this.types.put(ctx, aType);
		return null;
	}
	
	@Override 
	public Void visitIdTarget(IdTargetContext ctx) {
		String name = ctx.ID().getText();
		if (this.variables.contains(name) == false) {
			this.addError("use of undeclared variable "+name+" in expression: {expr}", ctx);
			this.types.put(ctx, Type.BOOL); // default type to prevent lookup errors
		} else {
			this.variables.assign(ctx);
			this.types.put(ctx, this.variables.getType(name));
		}
		return null;
	}
	
	/**
	 * Used to check if a function always reaches a return statement.
	 */
	private boolean hasReturn(StatContext ctx) {
		if (ctx instanceof ReturnStatContext) {
			return true;
		} else if (ctx instanceof BlockStatContext) {
			List<StatContext> stats = ((BlockStatContext) ctx).block().stat();
			if (stats.size() > 0) {
				return hasReturn(stats.get(stats.size() - 1));
			} else {
				return false;
			}
		} else if (ctx instanceof IfStatContext) {
			IfStatContext ifStat = (IfStatContext) ctx;
			if(ifStat.stat().size() > 1) {
				return hasReturn(ifStat.stat(0)) && hasReturn(ifStat.stat(1));
			} else {
				return hasReturn(ifStat.stat(0));
			}
		} else {
			return false;
		}
	}
}

