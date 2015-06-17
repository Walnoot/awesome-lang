package awesome.lang.checking;

import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.ParserRuleContext;

import awesome.lang.GrammarParser.AssignStatContext;
import awesome.lang.GrammarParser.DeclAssignStatContext;
import awesome.lang.GrammarParser.DeclStatContext;
import awesome.lang.GrammarParser.IdExprContext;
import awesome.lang.model.Type;

public class SymbolTable{

	private final ArrayList<Scope> declarations = new ArrayList<Scope>();
	private final HashMap<ParserRuleContext, Scope> contextmap = new HashMap<ParserRuleContext, Scope>();
	
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
	 * @return Scope removed scope
	 * @throws RuntimeException if the table only contains the outer scope.
	 */
	public Scope closeScope() {
		
		if (this.declarations.size() == 1)
			throw new RuntimeException("Table only contains outer scope");
		
		Scope scope = this.getCurrentScope();
		this.declarations.remove(scope);
		return scope;
		
	}

	/** Tries to declare a given identifier in the deepest scope level.
	 * @return <code>true</code> if the identifier was added,
	 * <code>false</code> if it was already declared in this scope.
	 */
	public boolean add(DeclAssignStatContext ctx, Type type) {
		
		boolean success = this.getCurrentScope().add(ctx.ID().getText(), type);
		if (success) {
			this.contextmap.put(ctx,  this.getCurrentScope());
		}
		
		return success;
		
	}
	public boolean add(DeclStatContext ctx, Type type) {
		
		boolean success = this.getCurrentScope().add(ctx.ID().getText(), type);
		if (success) {
			this.contextmap.put(ctx,  this.getCurrentScope());
		}
		
		return success;
		
	}
	
	/**
	 * binds an assignment context to the same scope as the variable definition, fails if variable has not yet been defined
	 * @param ctx
	 * @return
	 */
	public boolean assign(AssignStatContext ctx) {
		
		String id = ctx.ID().getText();
		for (int i = declarations.size()-1; i >= 0; i--) {
			if (declarations.get(i).containsKey(id)) {
				this.contextmap.put(ctx, declarations.get(i));
				return true;
			}
		}
		
		return false;
		
	}
	public boolean assign(IdExprContext ctx) {

		String id = ctx.ID().getText();
		for (int i = declarations.size()-1; i >= 0; i--) {
			if (declarations.get(i).containsKey(id)) {
				this.contextmap.put(ctx, declarations.get(i));
				return true;
			}
		}
		
		return false;
		
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
	public boolean contains(ParserRuleContext ctx) {
		if (this.contextmap.containsKey(ctx) == false)
			return false;
		
		Scope contains = this.contextmap.get(ctx);
		Scope current  = this.getCurrentScope();
		do {
			
			if (contains.equals(current))
				return true;
			
			current = current.parent;
			
		} while (current != null);
		
		return false;
		
	}

	/**
	 * 
	 * @throws IllegalArgumentException If variable is not found
	 * @param id
	 * @return
	 */
	public Type getType(String id) {
		
		for (int i = declarations.size()-1; i >= 0; i--) {
			if (declarations.get(i).containsKey(id)) {
				return declarations.get(i).getType(id);
			}
		}
		
		throw new IllegalArgumentException("Variable not found!");
		
	}

	private Type getType(ParserRuleContext ctx, String id) {
		
		if (this.contains(ctx) == false)
			return null;
		
		return this.contextmap.get(ctx).getType(id);
		
	}
	public Type getType(DeclStatContext ctx) {
		return this.getType((ParserRuleContext) ctx, ctx.ID().getText());
	}
	public Type getType(DeclAssignStatContext ctx) {
		return this.getType((ParserRuleContext) ctx, ctx.ID().getText());
	}
	public Type getType(AssignStatContext ctx) {
		return this.getType((ParserRuleContext) ctx, ctx.ID().getText());
	}

	/**
	 * 
	 * @throws IllegalArgumentException if variable is not found
	 * @param ctx
	 * @param id
	 * @return
	 */
	private int getOffset(ParserRuleContext ctx, String id) {
		
		if (this.contextmap.containsKey(ctx) == false)
			throw new IllegalArgumentException("Variable not found!");
		
		return this.contextmap.get(ctx).getOffset(id);
		
	}
	public int getOffset(DeclStatContext ctx) {
		return this.getOffset((ParserRuleContext) ctx, ctx.ID().getText());
	}
	public int getOffset(DeclAssignStatContext ctx) {
		return this.getOffset((ParserRuleContext) ctx, ctx.ID().getText());
	}
	public int getOffset(AssignStatContext ctx) {
		return this.getOffset((ParserRuleContext) ctx, ctx.ID().getText());
	}
	public int getOffset(IdExprContext ctx) {
		return this.getOffset((ParserRuleContext) ctx, ctx.ID().getText());
	}
	
	public int getOffset(String id){

		for (int i = declarations.size()-1; i >= 0; i--) {
			if (declarations.get(i).containsKey(id)) {
				return declarations.get(i).getOffset(id);
			}
		}
		
		throw new IllegalArgumentException("Variable not found!");
		
	}
	
}
