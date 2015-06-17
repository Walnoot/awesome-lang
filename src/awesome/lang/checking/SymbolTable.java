package awesome.lang.checking;

import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;

import awesome.lang.model.Type;

public class SymbolTable{

	private final ArrayList<Scope> declarations = new ArrayList<Scope>();
	
	public SymbolTable() {
		// outer scope
		this.declarations.add(new Scope(null, null));
	}

	public Scope getCurrentScope() {
		return this.declarations.get(this.declarations.size()-1);
	}
	
	/** Adds a next deeper scope level. */
	public void openScope(ParserRuleContext ctx) {
		declarations.add(new Scope(ctx, this.getCurrentScope()));
	}

	/** Removes the deepest scope level.
	 * @throws RuntimeException if the table only contains the outer scope.
	 */
	public void closeScope() {
		
		if (this.declarations.size() == 1)
			throw new RuntimeException("Table only contains outer scope");
		
		this.declarations.remove(this.getCurrentScope());
		
	}
	
	/** Tries to declare a given identifier in the deepest scope level.
	 * @return <code>true</code> if the identifier was added,
	 * <code>false</code> if it was already declared in this scope.
	 */
	public boolean add(String id, Type type) {
		
		return this.getCurrentScope().add(id, type);
		
	}

	/** Tests if a given identifier is in the scope of any declaration.
	 * @return <code>true</code> if there is any enclosing scope in which
	 * the identifier is declared; <code>false</code> otherwise.
	 */
	public boolean contains(String id) {
		
		for (int i = declarations.size()-1; i >= 0; i--) {
			if (declarations.get(i).containsKey(id)) {
				return true;
			}
		}
		
		return false;
	}
	
	public Type getType(String id) {
		
		for (int i = declarations.size()-1; i >= 0; i--) {
			if (declarations.get(i).containsKey(id)) {
				return declarations.get(i).getType(id);
			}
		}
		
		throw new RuntimeException("Variable not found!");
		
	}
	
	public int getOffset(String id){
		return 0;
	}
}
