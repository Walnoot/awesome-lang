package awesome.lang.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.antlr.v4.runtime.ParserRuleContext;

public class Scope {

	public  final ParserRuleContext identifier;
	public  final Scope parent;
	private final ArrayList<Scope> children = new ArrayList<Scope>();
	private final HashMap<String, Type> declarations = new HashMap<String, Type>();
	private final HashMap<String, Integer> offsets = new HashMap<String, Integer>();
	private AtomicInteger offset = new AtomicInteger(0);

	public Scope(ParserRuleContext ctx, Scope parent, boolean resetOffset) {
		this.parent = parent;
		this.identifier = ctx;
		if (resetOffset == false)
			this.offset = parent.getAtomicOffset();
		
	}

	public void addChild(Scope child) {
		this.children.add(child);
	}

	public boolean add(String id, Type type) {
		return this.add(id, type, true);
	}
	
	public boolean add(String id, Type type, boolean addOffset) {
		if (this.declarations.containsKey(id))
			return false;

		this.declarations.put(id, type);
		if (addOffset) {
			this.offsets.put(id, this.offset.get());
			this.offset.addAndGet(type.getSize());
		}
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
	
	public AtomicInteger getAtomicOffset() {
		return this.offset;
	}
	
	public int getOffset() {
		return this.offset.get();
	}
	
	public boolean isGlobal(){
		return parent == null;
	}
}
