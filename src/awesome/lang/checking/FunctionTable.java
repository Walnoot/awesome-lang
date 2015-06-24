package awesome.lang.checking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTreeProperty;

import awesome.lang.GrammarParser.FunctionCallContext;
import awesome.lang.model.Type;
import awesome.lang.model.Type.FunctionType;

public class FunctionTable {
	
	private HashMap<String, ArrayList<Type.FunctionType>> types = new HashMap<String, ArrayList<Type.FunctionType>>();
	private ParseTreeProperty<FunctionType> contextTypes	    = new ParseTreeProperty<FunctionType>();
	
	public boolean addFunction(String name, Type.FunctionType type) {
		
		if (this.types.containsKey(name) == false)
			this.types.put(name, new ArrayList<FunctionType>());
		
		if (this.containsWithArgs(name, type))
			return false;
		
		this.types.get(name).add(type);
		return true;
	}
	
	public List<FunctionType> getTypes(String name) {
		if (this.types.containsKey(name) == false)
			return null;
		
		ArrayList<FunctionType> list = this.types.get(name);
		return Collections.unmodifiableList(list);
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
	
	public void addContextToFunctionType(FunctionCallContext ctx, FunctionType type) {
		this.contextTypes.put(ctx, type);
	}
	
	public FunctionType getFunctionType(FunctionCallContext ctx) {
		return this.contextTypes.get(ctx);
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
	
}
