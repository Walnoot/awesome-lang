package awesome.lang.checking;

import java.util.ArrayList;
import java.util.HashMap;

import awesome.lang.model.Type;

public class Scope {
	
	private final Scope parent;
	private final ArrayList<Scope> children = new ArrayList<Scope>();
	private final HashMap<String, Type> declarations = new HashMap<String, Type>();
	private final HashMap<String, Integer> offsets = new HashMap<String, Integer>();
	private int offset = 0;
	
	public Scope(Scope parent) {
		this.parent = parent;
	}
	
	public void addChild(Scope child) {
		this.children.add(child);
	}

	public Scope getParent() {
		return this.parent;
	}
	
	public boolean add(String id, Type type) {
		
		if (this.declarations.containsKey(id))
			return false;
		
		this.declarations.put(id, type);
		this.offsets.put(id, this.offset++);
		return true;
		
	}

	public boolean containsKey(String id) {
		
		return this.declarations.containsKey(id);
		
	}

	public Type getType(String id) {
		
		if (this.containsKey(id) == false)
			return null;
		
		return this.declarations.get(id);
		
	}
	
	/**
	 * 
	 * @throws IllegalArgumentException If id has not been registered
	 * @param id
	 * @return
	 */
	public int getOffset(String id) {
		
		if (this.containsKey(id) == false)
			throw new IllegalArgumentException("Variable name has not been defined.");
		
		return this.offsets.get(id);
		
	}

	
}
