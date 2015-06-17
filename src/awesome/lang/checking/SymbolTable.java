package awesome.lang.checking;

import java.util.ArrayList;
import java.util.HashMap;

import awesome.lang.model.Type;

public class SymbolTable{

	private ArrayList<HashMap<String, Type>> declarations = new ArrayList<HashMap<String, Type>>();
	
	public SymbolTable() {
		// outer scope
		this.openScope();
	}
	
	/** Adds a next deeper scope level. */
	public void openScope() {
		declarations.add(new HashMap<String, Type>());
	}

	/** Removes the deepest scope level.
	 * @throws RuntimeException if the table only contains the outer scope.
	 */
	public void closeScope() {
		
		if (declarations.size() == 1)
			throw new RuntimeException("Table only contains outer scope");
		
		declarations.remove(declarations.size()-1);
		
	}
	
	/** Tries to declare a given identifier in the deepest scope level.
	 * @return <code>true</code> if the identifier was added,
	 * <code>false</code> if it was already declared in this scope.
	 */
	public boolean add(String id, Type type) {
		
		HashMap<String, Type> scope = declarations.get(declarations.size()-1);
		
		if (scope.containsKey(id))
			return false;
		
		scope.put(id, type);
		return true;
		
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
				return declarations.get(i).get(id);
			}
		}
		
		throw new RuntimeException("Variable not found!");
		
	}
	
	public int getOffset(String id){
		return 0;
	}
}
