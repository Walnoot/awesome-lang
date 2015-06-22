package awesome.lang.checking;

import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.ParserRuleContext;

import awesome.lang.model.Type;

public class Scope {

	public  final ParserRuleContext identifier;
	public  final Scope parent;
	private final ArrayList<Scope> children = new ArrayList<Scope>();
	private final HashMap<String, Type> declarations = new HashMap<String, Type>();
	private final HashMap<String, Integer> offsets = new HashMap<String, Integer>();
	private int offset = 0;

	public Scope(ParserRuleContext ctx, Scope parent) {
		this.parent = parent;
		this.identifier = ctx;
	}

	public void addChild(Scope child) {
		this.children.add(child);
	}

	public boolean add(String id, Type type) {

		if (this.declarations.containsKey(id))
			return false;

		this.declarations.put(id, type);
		this.offsets.put(id, this.offset);
		this.offset += type.getSize();
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
	 * @throws IllegalArgumentException
	 *             If id has not been registered
	 * @param id
	 * @return
	 */
	public int getOffset(String id) {

		if (this.containsKey(id) == false)
			throw new IllegalArgumentException(
					"Variable name has not been defined.");

		return this.offsets.get(id);

	}

}
