package awesome.lang.checking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import awesome.lang.model.Type;
import awesome.lang.model.Type.FunctionType;

public class FunctionTable {
	
	private HashMap<String, ArrayList<Type.FunctionType>> types = new HashMap<String, ArrayList<Type.FunctionType>>();
	
	public boolean addFunction(String name, Type.FunctionType type) {
		
		if (this.types.containsKey(name) == false)
			this.types.put(name, new ArrayList<FunctionType>());
		
		if (this.contains(name, type))
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
	
	public boolean contains(String name, Type.FunctionType type) {
		List<FunctionType> types = this.getTypes(name);
		for (FunctionType tCheck : types) {
			if (tCheck.equals(type))
				return true;
		}
		
		return false;
	}
	
}
