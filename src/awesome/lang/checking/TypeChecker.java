package awesome.lang.checking;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.*;
import awesome.lang.GrammarParser.AcquireStatContext;
import awesome.lang.GrammarParser.AddSubExprContext;
import awesome.lang.GrammarParser.ArgumentContext;
import awesome.lang.GrammarParser.ArrayLengthExprContext;
import awesome.lang.GrammarParser.ArrayTargetContext;
import awesome.lang.GrammarParser.ArrayTypeContext;
import awesome.lang.GrammarParser.ArrayValueExprContext;
import awesome.lang.GrammarParser.AssignStatContext;
import awesome.lang.GrammarParser.BlockContext;
import awesome.lang.GrammarParser.BlockStatContext;
import awesome.lang.GrammarParser.BoolExprContext;
import awesome.lang.GrammarParser.BoolTypeContext;
import awesome.lang.GrammarParser.CharTypeContext;
import awesome.lang.GrammarParser.ClassDefContext;
import awesome.lang.GrammarParser.ClassTargetContext;
import awesome.lang.GrammarParser.CompExprContext;
import awesome.lang.GrammarParser.DeclAssignStatContext;
import awesome.lang.GrammarParser.DeclStatContext;
import awesome.lang.GrammarParser.DoStatContext;
import awesome.lang.GrammarParser.EnumDefContext;
import awesome.lang.GrammarParser.EnumExprContext;
import awesome.lang.GrammarParser.EnumOrClassTypeContext;
import awesome.lang.GrammarParser.ExprContext;
import awesome.lang.GrammarParser.FalseExprContext;
import awesome.lang.GrammarParser.ForStatContext;
import awesome.lang.GrammarParser.FuncExprContext;
import awesome.lang.GrammarParser.FunctionCallContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.IdTargetContext;
import awesome.lang.GrammarParser.IfStatContext;
import awesome.lang.GrammarParser.IntTypeContext;
import awesome.lang.GrammarParser.LockTypeContext;
import awesome.lang.GrammarParser.ModExprContext;
import awesome.lang.GrammarParser.MultDivExprContext;
import awesome.lang.GrammarParser.NewClassExprContext;
import awesome.lang.GrammarParser.NextStatContext;
import awesome.lang.GrammarParser.NumExprContext;
import awesome.lang.GrammarParser.ParExprContext;
import awesome.lang.GrammarParser.PrefixExprContext;
import awesome.lang.GrammarParser.ReadExprContext;
import awesome.lang.GrammarParser.ReleaseStatContext;
import awesome.lang.GrammarParser.ReturnStatContext;
import awesome.lang.GrammarParser.StatContext;
import awesome.lang.GrammarParser.StringExprContext;
import awesome.lang.GrammarParser.SwitchStatContext;
import awesome.lang.GrammarParser.TargetExprContext;
import awesome.lang.GrammarParser.TrueExprContext;
import awesome.lang.GrammarParser.TypeContext;
import awesome.lang.GrammarParser.WhileStatContext;
import awesome.lang.GrammarParser.WriteStatContext;
import awesome.lang.checking.FunctionTable.Function;
import awesome.lang.model.Scope;
import awesome.lang.model.Type;
import awesome.lang.model.Type.ArrayType;
import awesome.lang.model.Type.ClassType;
import awesome.lang.model.Type.EnumType;
import awesome.lang.model.Type.FunctionType;

public class TypeChecker extends GrammarBaseVisitor<Void> {
	private ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
	private ParseTreeProperty<Boolean> blockNewScope = new ParseTreeProperty<Boolean>();
	private ArrayList<String> errors 	  = new ArrayList<String>();
	private SymbolTable variables		  = new SymbolTable();
	private FunctionTable functions		  = new FunctionTable();
	private Type returnType				  = null;
	private Boolean inSwitch			  = Boolean.FALSE;
	private CompilationUnit cUnit;

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
	
	// execute source elements in the order: 
	public void checkProgram(CompilationUnit cUnit) {
		this.cUnit = cUnit;
		// enum definitions
		for(EnumDefContext child : cUnit.getEnumlist()) {
			visit(child);
		}
		// class definitions 
		for(ClassDefContext child : cUnit.getClasslist()) {
			visit(child);
		}
		// function definitions (visitFunction does not check its body, that task is left for the end of this function(checkProgram)
		for(FunctionContext child : cUnit.getFunclist()) {
			visit(child);
		}
		// execute all bodies
		for(StatContext stat : cUnit.getStatlist()){
			visit(stat);
		}
		
		for(FunctionContext child : cUnit.getFunclist()) {
			
			// restore class scope, if neccesary
			boolean isMethod = child.parent instanceof ClassDefContext;
			Scope restore = null;
			ClassType classType = null;
			if (isMethod) {
				classType = Type.getClass(((ClassDefContext) child.parent).ID().getText());
				restore = this.variables.swapScopes(classType.getScope());
			}
			
			// open scope (add local variables)
			this.variables.openScope(((FunctionContext) child), true);

			// add 'this' if the function is a method
			if (isMethod && classType != null) {
				this.variables.addMethodThis(classType);
			}
			
			for(ArgumentContext arg : ((FunctionContext) child).argument()) {
				// arg is already visitited in visitFunction
				this.variables.add(arg, this.types.get(arg));
			}
			// add scope to function
			Function func = this.functions.getFunction((FunctionContext) child);
			func.setScope(this.variables.getCurrentScope());
			// set return type, for return statements
			this.returnType = func.getFunctionType().getReturnType();
			
			// execute stat/subscopes or return expression
			if (child.stat() != null)
				visit(child.stat());
			else {
				visit(child.expr());
				if (this.returnType.equals(this.types.get(child.expr())) == false) {
					this.addError("Return type does not match function definition in expression: {expr}", child);
				}
			}
			
			// reset return type and finalize
			this.returnType = null;
			this.variables.closeScope();
			
			// restore scope? (If a class was visited)
			if (restore != null)
				this.variables.swapScopes(restore);
			
		}
	}
	
	@Override
	public Void visitAcquireStat(AcquireStatContext ctx) {
		
		visit(ctx.target());
		if(Type.LOCK.equals(this.types.get(ctx.target())) == false) {
			this.addError("Trying to lock an " + this.types.get(ctx.target()) + " in expression: {expr}", ctx);
		}
		return null;
	}
	
	@Override
	public Void visitReleaseStat(ReleaseStatContext ctx) {
		
		visit(ctx.target());
		if(Type.LOCK.equals(this.types.get(ctx.target())) == false) {
			this.addError("Trying to lock an " + this.types.get(ctx.target()) + " in expression: {expr}", ctx);
		}
		return null;
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
	public Void visitClassDef(ClassDefContext ctx) {
		String name = ctx.ID().getText();
		if (Type.classExists(name)) {
			this.addError("Redefined class with name "+name+" in expression: {expr}", ctx);
		} else if(Type.enumExists(name)) {
			this.addError("Cannot define a class with the same identifier as an enum, in expression: {expr}", ctx);
		} else {
			// create class
			ClassType cls = Type.newClass(name);
			this.variables.openScope(ctx, true);
			cls.setScope(this.variables.getCurrentScope());
			
			// define all properties
			for (int i = 0; i < ctx.declStat().size(); i++) {
				visit(ctx.declStat(i));
			}
			
			// delay function definition and execution
			for (FunctionContext child : ctx.function()) {
				this.cUnit.add(child);
			}
			
			// close scope again (can still be accessed through Type.getClass(name).getScope()
			this.variables.closeScope();
		}
		return null;
	}
	
	@Override
	public Void visitEnumDef(EnumDefContext ctx) {
		// get name+values
		String name = ctx.ID(0).getText();
		ArrayList<String> values = new ArrayList<String>();
		if (ctx.ID().size() > 1) {
			for (int i = 1; i < ctx.ID().size(); i++) {
				values.add(ctx.ID(i).getText());
			}
		}
		// create new ENUM-type
		if (Type.enumExists(name)) {
			this.addError("Redefined enum\""+name+"\" in expression: {expr}", ctx);
		} else if(Type.classExists(name)) {
			this.addError("Cannot define a enum with the same identifier as a class, in expression: {expr}", ctx);
		} else {
			Type.newEnum(name, values);
		}
		return null;
	}
	
	@Override 
	public Void visitFunction(FunctionContext ctx) {
		
		boolean isClassMethod = ctx.parent instanceof ClassDefContext;
		
		// swap scope?
		Scope restore = null;
		if (isClassMethod) {
			ClassType classType = Type.getClass(((ClassDefContext) ctx.parent).ID().getText());
			restore = this.variables.swapScopes(classType.getScope());
		}
		
		// only function definition, contents are evaluated in visitProgram.
		TypeContext typeExpr = ctx.type();
		if(typeExpr != null) visit(typeExpr);
		for (ArgumentContext child : ctx.argument()) {
			visit(child);
		}
		
		Type retType = typeExpr == null ? Type.VOID : this.types.get(typeExpr);
		
		if(retType != Type.VOID && ctx.expr() == null && !hasReturn(ctx.stat())){
			addError("Function " + ctx.ID().getText() + " does not return properly", ctx);
		}
		
		int difference = (isClassMethod ? 1 : 0);
		Type[] argTypes = new Type[ctx.argument().size()+difference];
		for (int i = 0; i < ctx.argument().size(); i++) {
			argTypes[i+difference] = this.types.get(ctx.argument(i));
		}
		// class method, first argument becomes the object reference
		if (isClassMethod)
			argTypes[0] = Type.getClass(((ClassDefContext) ctx.parent).ID().getText());
		
					
		boolean thread = (ctx.THREAD() != null);
		if (thread && ctx.argument().size() > 0) {
			this.addError("Thread definition contains arguments in expression: {expr}", ctx);
		}
		if (thread && isClassMethod) {
			this.addError("You cannot use a class method as a thread, in expression: {expr}", ctx);
		}
		
		FunctionType fType = Type.function(retType, argTypes);
		fType.setMethod(isClassMethod);
		String name  	   = ctx.ID().getText(); 
		if (this.functions.containsWithArgs(name, fType)) {
			this.addError("Double function definition with the same arguments in expression: {expr}", ctx);
		} else {
			this.functions.addFunction(name, fType, thread);
		}
		
		this.functions.addContextToFunction(ctx, fType);
		
		// restore scope (only if swapped?)
		if (restore != null) {
			this.variables.swapScopes(restore);
		}
		
		return null;
	}

	@Override
	public Void visitFunctionCall(FunctionCallContext ctx) {
		
		// get argument types
		int difference = (ctx.ON() != null ? 1 : 0);
		Type[] args = new Type[ctx.expr().size()];
		for(int i = 0; i < ctx.expr().size()-difference; i++) {
			visit(ctx.expr(i));
			args[i+difference] = this.types.get(ctx.expr(i));
		}
		if (difference > 0) {
			ExprContext objCtx = ctx.expr(ctx.expr().size()-1);
			visit(objCtx);
			args[0] = this.types.get(objCtx);
		}
		
		String name = ctx.ID().getText();
		
		FunctionType ftype = this.functions.getFunctionTypeByArgs(name, args, ctx.ON() != null);
		if (ftype == null) {
			this.addError("Function call to unknown " + (ctx.ON() == null ? "function " : "method") + "in expression: {expr}", ctx);
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
	public Void visitCharType(CharTypeContext ctx) {
		types.put(ctx, Type.CHAR);
		return null;
	}
	
	@Override
	public Void visitLockType(LockTypeContext ctx) {
		types.put(ctx, Type.LOCK);
		// this type can only be defined in the global scope
		if (this.variables.getCurrentScope().isGlobal() == false) {
			this.addError("Locks can only be defined in the global scope, in expression: {expr}", ctx);
		}
		return null;
	}
	
	@Override
	public Void visitEnumOrClassType(EnumOrClassTypeContext ctx) {
		String name = ctx.ID().getText();
		if (Type.enumExists(name) == false && Type.classExists(name) == false) {
			this.addError("Using an undefined enum or class type in expression: {expr}", ctx);
			this.types.put(ctx, Type.BOOL); // default type is boolean, this is for not requiring the typechecker to check for failures in every expression.
		} else {
			if (Type.enumExists(name))
				this.types.put(ctx, Type.getEnum(name));
			else 
				this.types.put(ctx, Type.getClass(name));
		}
		return null;
	}
	
	@Override
	public Void visitArrayType(ArrayTypeContext ctx) {
		visit(ctx.type());
		Type type = types.get(ctx.type());
		
		types.put(ctx, Type.array(type));
		return null;
	}

	@Override // Lock support is not required here, because constraints on the equality of the type and the type of the expression. (Expression cannot become of type Lock).
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
		
		if(types.get(ctx.expr(1)) != Type.INT) {
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
		if (this.types.get(child1).equals(this.types.get(child2)) == false) {
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
	public Void visitModExpr(ModExprContext ctx) {
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
	public Void visitNewClassExpr(NewClassExprContext ctx) {
		String name = ctx.ID().getText();
		if (Type.classExists(name) == false) {
			 this.addError("Using an undefined class in expression: {expr}", ctx);
			 this.types.put(ctx, Type.BOOL); // default value
		}
		else {
			this.types.put(ctx, Type.getClass(name));
		}
		return null;
	}
	
	
	@Override
	public Void visitEnumExpr(EnumExprContext ctx) {
		String name  = ctx.ID(0).getText();
		String value = ctx.ID(1).getText();
		if (Type.enumExists(name) == false) {
			this.addError("Using an undefined enum in expression: {expr}", ctx);
			this.types.put(ctx, Type.BOOL); // default type
		}
		else {
			EnumType type = Type.getEnum(name);
			if (type.contains(value) == false) {
				this.addError("Using an undefined value in enum \""+name+"\" in expression: {expr}", ctx);
			}
			this.types.put(ctx, type);
		}
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
	public Void visitArrayValueExpr(ArrayValueExprContext ctx) {
		visit(ctx.expr(0));
		Type cmp = this.types.get(ctx.expr(0));
		for (int i = 1; i < ctx.expr().size(); i++) {
			visit(ctx.expr(i));
			Type type = this.types.get(ctx.expr(i));
			if (cmp.equals(type) == false) {
				this.addError("Invalid value of type "+type+" in expression: {expr}", ctx);
			}
		}
		this.types.put(ctx, Type.array(cmp));
		return null;
	}
	
	@Override
	public Void visitArrayLengthExpr(ArrayLengthExprContext ctx) {
		visit(ctx.type());
		visit(ctx.expr());
		
		if(types.get(ctx.expr()).equals(Type.INT) == false) {
			addError("Array length expression must be of type int in {expr}", ctx.expr());
		}
		
		types.put(ctx, Type.array(types.get(ctx.type())));
		
		return null;
	}
	
	@Override
	public Void visitStringExpr(StringExprContext ctx) {
		types.put(ctx, Type.array(Type.CHAR));
		
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
		if (this.variables.add(ctx, aType) == false) {
			this.variables.assign(ctx);
		}
		this.types.put(ctx, aType);
		return null;
	}
	
	@Override
	public Void visitClassTarget(ClassTargetContext ctx) {
		visit(ctx.target());
		Type type = this.types.get(ctx.target());
		if (type instanceof ClassType == false) {
			this.addError("Cannot get a field of a non-class type in expression: {expr}", ctx);
			this.types.put(ctx, Type.BOOL); // default type
		}
		else {
			Scope scope = ((ClassType) type).getScope();
			String property = ctx.ID().getText();
			if (scope.containsKey(property) == false) {
				this.addError("Field does not exist in the given class in expression: {expr}", ctx);
				this.types.put(ctx, Type.BOOL); // default type
			} else {
				Type newType = scope.getType(property);
				this.types.put(ctx, newType);
				this.variables.add(ctx, newType, scope);
			}
		}
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

