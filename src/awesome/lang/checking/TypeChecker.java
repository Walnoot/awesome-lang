package awesome.lang.checking;

import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarBaseListener;
import awesome.lang.GrammarParser.AddSubExprContext;
import awesome.lang.GrammarParser.AssignStatContext;
import awesome.lang.GrammarParser.BlockContext;
import awesome.lang.GrammarParser.BoolExprContext;
import awesome.lang.GrammarParser.CompExprContext;
import awesome.lang.GrammarParser.DeclAssignStatContext;
import awesome.lang.GrammarParser.DeclStatContext;
import awesome.lang.GrammarParser.ExprContext;
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
import awesome.lang.model.Type;

public class TypeChecker extends GrammarBaseListener {
	
	private ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
	private ArrayList<String> errors 	  = new ArrayList<String>();
	private SymbolTable variables		  = new SymbolTable();


	private void addError(String string, ParserRuleContext ctx) {
		
		Token token = ctx.getStart();
		String error = string.replace("{expr}", ctx.getText());
		this.errors.add(error + " (line "+token.getLine() + ":" + token.getCharPositionInLine() + ")");
		
	}

	public ArrayList<String> getErrors() {
		return new ArrayList<String> (this.errors);
	}

	@Override
	public void enterBlock(BlockContext ctx) {
		
		if (ctx.parent.getClass() != ProgramContext.class)
			this.variables.openScope(ctx);
		
	}
	
	@Override
	public void exitBlock(BlockContext ctx) {
		
		if (ctx.parent.getClass() != ProgramContext.class)
			this.variables.closeScope();
		
	}

	@Override
	public void exitDeclStat(DeclStatContext ctx) {
		// add new variable to scope (uninitialized)
		Type type = ctx.type().INT() != null ? Type.Int : Type.Bool;
		if (this.variables.add(ctx, type) == false) {
			this.addError("Redeclaration of variable "+ctx.ID().getText()+" in expression: \"{expr}\"", ctx);
		}
		
	}

	@Override
	public void exitDeclAssignStat(DeclAssignStatContext ctx) {
		// add new variable to scope (with value)
		Type type = this.types.get(ctx.expr());
		if (this.variables.add(ctx, type) == false) {
			this.addError("Redeclaration of variable "+ctx.ID().getText()+" in expression: \"{expr}\"", ctx);
		}
	}

	@Override
	public void exitAssignStat(AssignStatContext ctx) {
		// check assign type to variable type
		String name = ctx.ID().getText();
		if (this.variables.contains(name) == false) {
			this.addError("Assignment of undeclared variable "+name+" in expression: \"{expr}\"", ctx);
		} else {
			Type vartype = this.variables.getType(name);
			Type exptype = this.types.get(ctx.expr());
			if (vartype != exptype) {
				this.addError("Assignment of type "+exptype.toString()+" to variable of type "+vartype.toString()+" in expression \"{expr}\"", ctx);
			} else {
				this.variables.assign(ctx);
			}
		}
		
	}
	
	@Override
	public void exitWhileStat(WhileStatContext ctx) {
		if (this.types.get(ctx.expr()) != Type.Bool) {
			this.addError("Expression in while-statement does not return boolean: \"{expr}\"", ctx);
		}
	}

	@Override
	public void exitIfStat(IfStatContext ctx) {
		if (this.types.get(ctx.expr()) != Type.Bool) {
			this.addError("Expression in if-statement does not return boolean: \"{expr}\"", ctx);
		}
	}

	@Override
	public void exitPrefixExpr(PrefixExprContext ctx) {
		
		// not statement
		if (ctx.prefixOp().NOT() != null) {
			if (this.types.get(ctx.expr()) != Type.Bool){
				this.addError("Subexpression is not of type boolean in \"{expr}\"", ctx);
			}
			this.types.put(ctx, Type.Bool);
			
		// unary minus
		} else {
			if(this.types.get(ctx.expr()) != Type.Int){
				this.addError("Subexpression is not of type integer in \"{expr}\"", ctx);
			}
			this.types.put(ctx, Type.Int);
		}
		
		
	}

	@Override
	public void exitBoolExpr(BoolExprContext ctx) {
		
		// valid types?
		if (this.types.get(ctx.expr(0)) != Type.Bool){
			this.addError("First child of expression: \"{expr}\" is no boolean", ctx);
		} else if(this.types.get(ctx.expr(1)) != Type.Bool) {
			this.addError("Second child of expression: \"{expr}\" is no boolean", ctx);
		}
		
		this.types.put(ctx, Type.Bool);
		
	}

	@Override
	public void exitCompExpr(CompExprContext ctx) {
		ExprContext child1 = ctx.expr(0);
		ExprContext child2 = ctx.expr(1);
		
		// valid types?
		if (this.types.get(child1) != this.types.get(child2)) {
			this.addError("Comparing "+this.types.get(child1)+" with "+this.types.get(child2) + " in \"{expr}\"", ctx);
		} else if (this.types.get(child1) == Type.Int) {
			this.types.put(ctx, Type.Bool);
		} else if (ctx.compOp().EQ() == null && ctx.compOp().NE() == null) {// bool comparison with a wrong operand
			this.addError("Doing an impossible comparison on two booleans: \"{expr}\"", ctx);
		}
		
		this.types.put(ctx, Type.Bool);
		
	}

	@Override
	public void exitMultDivExpr(MultDivExprContext ctx) {
		
		// valid types? 
		if (this.types.get(ctx.expr(0)) != Type.Int){
			this.addError("First child of expression: \"{expr}\" is no integer", ctx);
		} else if (this.types.get(ctx.expr(1)) != Type.Int) {
			this.addError("Second child of expression: \"{expr}\" is no integer", ctx);
		}
		
		this.types.put(ctx, Type.Int);
		
	}

	@Override
	public void exitAddSubExpr(AddSubExprContext ctx) {
		
		// valid types?
		if (this.types.get(ctx.expr(0)) != Type.Int){
			this.addError("First child of expression: \"{expr}\" is no integer", ctx);
		} else if (this.types.get(ctx.expr(1)) != Type.Int) {
			this.addError("Second child of expression: \"{expr}\" is no integer", ctx);
		}
		
		this.types.put(ctx, Type.Int);
		
	}

	@Override
	public void exitParExpr(ParExprContext ctx) {
		this.types.put(ctx, this.types.get(ctx.expr()));
	}

	@Override
	public void exitNumExpr(NumExprContext ctx) {
		this.types.put(ctx, Type.Int);
	}

	@Override
	public void exitFalseExpr(FalseExprContext ctx) {
		this.types.put(ctx, Type.Bool);
	}
	
	@Override
	public void exitTrueExpr(TrueExprContext ctx) {
		this.types.put(ctx, Type.Bool);
	}
	
	@Override
	public void exitIdExpr(IdExprContext ctx) {
		String name = ctx.ID().getText();
		if (this.variables.contains(name) == false) {
			this.addError("use of undeclared variable "+name+" in expression: \"{expr}\"", ctx);
			this.types.put(ctx, Type.Bool); // default type to prevent lookup errors
		} else {
			this.types.put(ctx, this.variables.getType(name));
		}
		
	}
	
}
