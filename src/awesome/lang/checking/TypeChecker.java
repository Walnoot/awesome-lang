package awesome.lang.checking;

import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.*;
import awesome.lang.GrammarParser.AddSubExprContext;
import awesome.lang.GrammarParser.ArgumentContext;
import awesome.lang.GrammarParser.ArrayExprContext;
import awesome.lang.GrammarParser.ArrayTargetContext;
import awesome.lang.GrammarParser.ArrayTypeContext;
import awesome.lang.GrammarParser.AssignStatContext;
import awesome.lang.GrammarParser.BlockContext;
import awesome.lang.GrammarParser.BoolExprContext;
import awesome.lang.GrammarParser.BoolTypeContext;
import awesome.lang.GrammarParser.CompExprContext;
import awesome.lang.GrammarParser.DeclAssignStatContext;
import awesome.lang.GrammarParser.DeclStatContext;
import awesome.lang.GrammarParser.DoStatContext;
import awesome.lang.GrammarParser.ExprContext;
import awesome.lang.GrammarParser.FalseExprContext;
import awesome.lang.GrammarParser.ForStatContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.IdExprContext;
import awesome.lang.GrammarParser.IdTargetContext;
import awesome.lang.GrammarParser.IfStatContext;
import awesome.lang.GrammarParser.ImprtContext;
import awesome.lang.GrammarParser.IntTypeContext;
import awesome.lang.GrammarParser.MultDivExprContext;
import awesome.lang.GrammarParser.NumExprContext;
import awesome.lang.GrammarParser.ParExprContext;
import awesome.lang.GrammarParser.PrefixExprContext;
import awesome.lang.GrammarParser.ProgramContext;
import awesome.lang.GrammarParser.StatContext;
import awesome.lang.GrammarParser.TargetContext;
import awesome.lang.GrammarParser.TrueExprContext;
import awesome.lang.GrammarParser.WhileStatContext;
import awesome.lang.model.Type;
import awesome.lang.model.Type.ArrayType;
import awesome.lang.model.Type.FunctionType;

public class TypeChecker extends GrammarBaseVisitor<Void> {
	private ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
	private ParseTreeProperty<Boolean> blockNewScope = new ParseTreeProperty<Boolean>();
	private ArrayList<String> errors 	  = new ArrayList<String>();
	private SymbolTable variables		  = new SymbolTable();
	private FunctionTable functions		  = new FunctionTable();

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

	@Override 
	public Void visitProgram(ProgramContext ctx) {
		
		// fix imports
		for(ImprtContext child : ctx.imprt()) {
			visit(child);
		}
		// function definitions
		for(FunctionContext child : ctx.function()) {
			visit(child);
		}
		//  stats
		for(ParseTree child : ctx.children) {
			if (child instanceof FunctionContext) {
				this.variables.openScope(((FunctionContext) child), true);
				for(ArgumentContext arg : ((FunctionContext) child).argument()) {
					// arg is already visitited in visitFunction
					this.variables.add(arg, this.types.get(arg));
				}
				visit(((FunctionContext) child).stat());
				this.variables.closeScope();
			} else if(child instanceof StatContext) {
				visit(child);
			}
			
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
		visit(ctx.type());
		for (ParserRuleContext child : ctx.argument()) {
			visit(child);
		}
		
		Type retType = this.types.get(ctx.type());
		Type[] argTypes = new Type[ctx.argument().size()];
		for (int i = 0; i < ctx.argument().size(); i++) {
			argTypes[i] = this.types.get(ctx.argument(i));
		}
		
		FunctionType fType = Type.function(retType, argTypes); 
		String name  	   = ctx.ID().getText(); 
		if (this.functions.contains(name, fType)) {
			this.addError("Double function definition with the same arguments in expression: {expr}", ctx);
		} else {
			this.functions.addFunction(name, fType);
		}
		
		return null;
	}

	//@Override
	//public Void visitFunctionCall(FunctionCallContext ctx) {	
	//}	
	
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
		// check assign type to variable type
		String name = getID(ctx);
		
		if (this.variables.contains(name) == false) {
			this.addError("Assignment of undeclared variable "+name+" in expression: {expr}", ctx);
		} else {
			Type exptype = this.types.get(ctx.expr());
			Type vartype = this.variables.getType(name);
			if(vartype instanceof ArrayType){
				//assign to array position
				vartype = ((ArrayType) vartype).getType();
				
				if(!exptype.equals(vartype)){
					addError("Cannot assign " + exptype + " to " + vartype + " in {expr}", ctx);
				}
			}
			
			if (!vartype.equals(exptype)) {
				this.addError("Assignment of type "+exptype.toString()+" to variable of type "+vartype.toString()+" in expression {expr}", ctx);
			} else {
				this.variables.assign(ctx);
			}
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
	public Void visitIdExpr(IdExprContext ctx) {
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
	
	@Override
	public Void visitArrayExpr(ArrayExprContext ctx) {
		visit(ctx.expr());
		
		Type type = variables.getType(ctx.ID().getText());
		if(!type.isArray()) {
			addError("Not an array in expression: {expr}", ctx);
		}
		
		if(types.get(ctx.expr()) != Type.INT) {
			addError("No instance of int: ", ctx.expr());
		}
		
		types.put(ctx, ((ArrayType) type).getType());
		variables.assign(ctx);
		return null;
	}
	
	public static String getID(AssignStatContext ctx){
		return getID(ctx.target());
	}
	
	private static String getID(TargetContext ctx){
		if(ctx instanceof IdTargetContext) {
			return ((IdTargetContext) ctx).ID().getText();
		} else if (ctx instanceof ArrayTargetContext) {
			return getID(((ArrayTargetContext) ctx).target());
		} else {
			throw new UnsupportedOperationException("Unknown target class " + ctx.getClass());
		}
	}
	
}

