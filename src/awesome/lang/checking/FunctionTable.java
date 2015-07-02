package awesome.lang.checking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarParser.FunctionCallContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.NewObjectContext;
import awesome.lang.checking.FunctionTable.Function;
import awesome.lang.model.Scope;
import awesome.lang.model.Type;
import awesome.lang.model.Type.FunctionType;

public class FunctionTable {
	
	private HashMap<String, ArrayList<Function>> types = new HashMap<String, ArrayList<Function>>();
	private ParseTreeProperty<Function> contextTypes   = new ParseTreeProperty<Function>();
	
	/** 
	 * Creates a new fuction, if it does not already exists, and saves it in this class.
	 */
	public boolean addFunction(String name, Type.FunctionType type, boolean isThread) {
		
		if (this.types.containsKey(name) == false)
			this.types.put(name, new ArrayList<Function>());
		
		if (this.containsWithArgs(name, type))
			return false;
		
		this.types.get(name).add(new Function(name, type, isThread));
		return true;
	}

	/**
	 * Retrieves all functiontypes, based on all funtion overloads of the provided name
	 */
	public List<FunctionType> getTypes(String name) {
		if (this.types.containsKey(name) == false)
			return null;
		
		ArrayList<FunctionType> list = new ArrayList<FunctionType>();
		for (Function func : this.types.get(name))
			list.add(func.getFunctionType());
		
		return list;
	}
	
	/**
	 * Get all functions, based on all overloads of the provided name
	 */
	public List<Function> getFunctions(String name) {
		if (this.types.containsKey(name) == false)
			return null;
		
		return this.types.get(name);
	}
	
	/**
	 * Get a functiontype of a function with provided name, arguments and the isMethod of that class. Returns null if not found
	 */
	public FunctionType getFunctionTypeByArgs(String name, Type[] arguments, boolean isMethod) {
		List<FunctionType> types = this.getTypes(name);
		if(types != null) {
			for (FunctionType tCheck : types) {
				if (Arrays.equals(tCheck.getArguments(), arguments) && tCheck.isMethod() == isMethod)
					return tCheck;
			}
		}
			
		return null;
	}

	/**
	 * Bind the provided context to a functiontype, this method is called from the typechecker and used to determine the parameters of a functioncall in the generator 
	 */
	public void addContextToFunction(FunctionCallContext ctx, FunctionType type) {
		this.contextTypes.put(ctx, this.getFunction(ctx.ID().getText(), type.getArguments(), type.isMethod()));
	}

	/**
	 * Bind the provided context to a functiontype, this method is called from the typechecker and used to determine the parameters of a functioncall in the generator
	 */
	public void addContextToFunction(FunctionContext ctx, FunctionType type) {
		this.contextTypes.put(ctx, this.getFunction(ctx.ID().getText(), type.getArguments(), type.isMethod()));
	}

	/**
	 * Bind the provided context to a functiontype, this method is called from the typechecker and used to determine the parameters of a functioncall in the generator
	 */
	public void addContextToFunction(NewObjectContext ctx, FunctionType type) {
		Type[] arguments = type.getArguments();
		boolean method = type.isMethod();
		Function function = this.getFunction("init", arguments, method);
		this.contextTypes.put(ctx, function);
	}

	/**
	 * Gets the functiontype of a context, set in the method addConetextToFunction(XXXContext, FunctionType);
	 */
	public Function getFunction(FunctionCallContext ctx) {
		return this.contextTypes.get(ctx);
	}
	
	/**
	 * Gets the functiontype of a context, set in the method addConetextToFunction(XXXContext, FunctionType);
	 */
	public Function getFunction(FunctionContext ctx) {
		return this.contextTypes.get(ctx);
	}
	
	/**
	 * Gets the functiontype of a context, set in the method addConetextToFunction(XXXContext, FunctionType);
	 */
	public Function getFunction(NewObjectContext ctx) {
		return this.contextTypes.get(ctx);
	}
	
	/**
	 * Find a function, based on its name, arguments and the isMethod() fo the type. Returns null if not found
	 */
	public Function getFunction(String name, Type[] arguments, boolean isMethod) {
		List<Function> types = this.getFunctions(name);
		
		if(types != null) {
			for (Function tCheck : types) {
				if (Arrays.equals(tCheck.getFunctionType().getArguments(), arguments) && tCheck.getFunctionType().isMethod() == isMethod)
					return tCheck;
			}
		}
			
		return null;
	}
	
	/**
	 * Checkers whether a specific function-overload is registered in this functiontable.
	 */
	public boolean contains(String name, Type.FunctionType type) {
		List<FunctionType> types = this.getTypes(name);
		if(types != null) {
			for (FunctionType tCheck : types) {
				if (tCheck.equals(type) && tCheck.isMethod() == type.isMethod())
					return true;
			}
		}
			
		return false;
	}
	
	/** 
	 * Checks whether a specific function-overload is registered in this functiontable, not taking the return-type into account
	 */
	public boolean containsWithArgs(String name, FunctionType type) {
		return this.containsWithArgs(name, type.getArguments(), type.isMethod());
	}
	
	/**
	 * Checks whether a specific function-overload is registered in this functiontable, not taking the return-type into account
	 */
	public boolean containsWithArgs(String name, Type[] arguments, boolean isMethod) {
		List<FunctionType> types = this.getTypes(name);
		if(types != null) {
			for (FunctionType tCheck : types) {
				if (Arrays.equals(tCheck.getArguments(), arguments) && tCheck.isMethod() == isMethod)
					return true;
			}
		}
			
		return false;
	}
	
	/**
	 * Function description
	 */
	public class Function {
		private final String name;
		private final FunctionType type;
		private Scope scope;
		private boolean isThread;

		private Function(String name, FunctionType type) {
			this(name, type, false);
		}
		
		private Function(String name, FunctionType type, boolean isThread){
			this.name = name;
			this.type = type;
			this.isThread = isThread;
		}
		
		/**
		 * name getter
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * type getter
		 */
		public FunctionType getFunctionType() {
			return type;
		}
		
		/**
		 * scope getter
		 */
		public Scope getScope() {
			return scope;
		}
		
		/**
		 * scope setter
		 */
		public void setScope(Scope scope) {
			this.scope = scope;
		}
		
		/**
		 * return whether this function is a new thread definition 
		 */
		public boolean isThreadFunction(){
			return this.isThread;
		}
		
		/**
		 * returns whether this function is a class-method.
		 * @return
		 */
		public boolean isMethod() {
			return this.type.isMethod();
		}
	}
	
}
