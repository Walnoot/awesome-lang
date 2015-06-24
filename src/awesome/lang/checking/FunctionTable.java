package awesome.lang.checking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarParser.FunctionCallContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.checking.FunctionTable.Function;
import awesome.lang.model.Scope;
import awesome.lang.model.Type;
import awesome.lang.model.Type.FunctionType;

public class FunctionTable {
	
	private HashMap<String, ArrayList<Function>> types = new HashMap<String, ArrayList<Function>>();
	private ParseTreeProperty<Function> contextTypes   = new ParseTreeProperty<Function>();
	
	public boolean addFunction(String name, Type.FunctionType type) {
		
		if (this.types.containsKey(name) == false)
			this.types.put(name, new ArrayList<Function>());
		
		if (this.containsWithArgs(name, type))
			return false;
		
		this.types.get(name).add(new Function(name, type));
		return true;
	}

	public List<FunctionType> getTypes(String name) {
		if (this.types.containsKey(name) == false)
			return null;
		
		ArrayList<FunctionType> list = new ArrayList<FunctionType>();
		for (Function func : this.types.get(name))
			list.add(func.getFunctionType());
		
		return list;
	}

	public List<Function> getFunctions(String name) {
		if (this.types.containsKey(name) == false)
			return null;
		
		return this.types.get(name);
	}
	
	public FunctionType getFunctionTypeByArgs(String name, Type[] arguments) {
		List<FunctionType> types = this.getTypes(name);
		if(types != null) {
			for (FunctionType tCheck : types) {
				if (Arrays.equals(tCheck.getArguments(), arguments))
					return tCheck;
			}
		}
			
		return null;
	}

	public void addContextToFunction(FunctionCallContext ctx, FunctionType type) {
		this.contextTypes.put(ctx, this.getFunction(ctx.ID().getText(), type.getArguments()));
	}

	public void addContextToFunction(FunctionContext ctx, FunctionType type) {
		this.contextTypes.put(ctx, this.getFunction(ctx.ID().getText(), type.getArguments()));
	}
	
	public Function getFunction(FunctionCallContext ctx) {
		return this.contextTypes.get(ctx);
	}

	public Function getFunction(FunctionContext ctx) {
		return this.contextTypes.get(ctx);
	}

	
	public Function getFunction(String name, Type[] arguments) {
		List<Function> types = this.getFunctions(name);
		
		if(types != null) {
			for (Function tCheck : types) {
				if (Arrays.equals(tCheck.getFunctionType().getArguments(), arguments))
					return tCheck;
			}
		}
			
		return null;
	}
	
	public boolean contains(String name, Type.FunctionType type) {
		List<FunctionType> types = this.getTypes(name);
		if(types != null) {
			for (FunctionType tCheck : types) {
				if (tCheck.equals(type))
					return true;
			}
		}
			
		return false;
	}

	public boolean containsWithArgs(String name, FunctionType type) {
		return this.containsWithArgs(name, type.getArguments());
	}
	
	public boolean containsWithArgs(String name, Type[] arguments) {
		List<FunctionType> types = this.getTypes(name);
		if(types != null) {
			for (FunctionType tCheck : types) {
				if (Arrays.equals(tCheck.getArguments(), arguments))
					return true;
			}
		}
			
		return false;
	}
	
	public class Function {
		private final String name;
		private final FunctionType type;
		private Scope scope;

		private Function(String name, FunctionType type){
			this.name = name;
			this.type = type;
			
		}
		
		public String getName() {
			return name;
		}
		
		public FunctionType getFunctionType() {
			return type;
		}

		public Scope getScope() {
			return scope;
		}

		public void setScope(Scope scope) {
			this.scope = scope;
		}
		
	}
	
}
