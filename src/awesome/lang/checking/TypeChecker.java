package awesome.lang.checking;

import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarBaseListener;
import awesome.lang.GrammarParser;
import awesome.lang.GrammarParser.*;
import awesome.lang.model.Type;

public class TypeChecker extends GrammarBaseListener {
	
	private ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
	private ArrayList<String> errors 	  = new ArrayList<String>();
	private SymbolTable variables		  = new SymbolTable();


	private void addError(String string, ParserRuleContext ctx) {
		
		Token token = ctx.getStart();
		String error = string.replace("{expr}", ctx.getText());
		this.errors.add(error + " (line +"+token.getLine() + ":" + token.getCharPositionInLine() + ")");
		
	}

	@Override
	public void enterBlock(BlockContext ctx) {
		
		if (ctx.parent.getClass() != ProgramContext.class)
			this.variables.openScope();
		
	}
	
	@Override
	public void exitBlock(BlockContext ctx) {
		this.variables.closeScope();
	}

	@Override
	public void exitDeclStat(DeclStatContext ctx) {
		
	}

	@Override
	public void exitDeclAssignStat(DeclAssignStatContext ctx) {
		
	}

	@Override
	public void exitBlockStat(BlockStatContext ctx) {
		
	}

	@Override
	public void exitAssignStat(AssignStatContext ctx) {
		
	}
	
	@Override
	public void exitWhileStat(WhileStatContext ctx) {
		
	}

	@Override
	public void exitAsmStat(AsmStatContext ctx) {
		
	}

	@Override
	public void exitIfStat(IfStatContext ctx) {
		
	}
	
	@Override
	public void exitType(TypeContext ctx) {
		
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
		}
		else if (this.types.get(child1) == Type.Int) {
			this.types.put(ctx, Type.Bool);
		}
		else if (ctx.compOp().EQ() == null) {// bool comparison with a wrong operand
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
		// TODO: Put variable type
	}
	
}
