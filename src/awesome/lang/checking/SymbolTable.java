package awesome.lang.checking;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable{

	private ArrayList<HashMap<String, String>> declarations = new ArrayList<HashMap<String, String>>();
	
	public SymbolTable() {
		// outer scope
		declarations.add(new HashMap<String, String>());
	}
	
	/** Adds a next deeper scope level. */
	public void openScope() {
		declarations.add(new HashMap<String, String>());
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
	public boolean add(String id, String type) {
		
		HashMap<String, String> scope = declarations.get(declarations.size()-1);
		
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
	
	public String getType(String id) {
		
		for (int i = declarations.size()-1; i >= 0; i--) {
			if (declarations.get(i).containsKey(id)) {
				return declarations.get(i).get(id);
			}
		}
		
		throw new RuntimeException("Variable not found!");
		
	}

}
