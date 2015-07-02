package awesome.lang.checking;

import java.util.ArrayList;

import awesome.lang.GrammarParser.ClassDefContext;
import awesome.lang.GrammarParser.EnumDefContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.StatContext;

public class CompilationUnit {
	private ArrayList<EnumDefContext> enumlist = new ArrayList<EnumDefContext>(); 
	private ArrayList<ClassDefContext> classlist = new ArrayList<ClassDefContext>(); 
	private ArrayList<FunctionContext> funclist = new ArrayList<FunctionContext>(); 
	private ArrayList<StatContext> statlist = new ArrayList<StatContext>();
	
	/**
	 * Add an enum to this unit, which can be retrieved later by calling .getEnumList() 
	 */
	public void add(EnumDefContext ctx) {
		this.getEnumlist().add(ctx);
	}

	/**
	 * Add a class definition to this unit, which can be retrieved later by calling .getClassList() 
	 */
	public void add(ClassDefContext ctx) {
		this.getClasslist().add(ctx);
	}

	/**
	 * Add a function to this unit, which can be retrieved later by calling .getFuncList() 
	 */
	public void add(FunctionContext ctx) {
		this.getFunclist().add(ctx);
	}

	/**
	 * Add a statement to this unit, which can be retrieved later by calling .getStatList() 
	 */
	public void add(StatContext ctx) {
		this.getStatlist().add(ctx);
	}
	
	/**
	 * Retrieves all added enums
	 */
	public ArrayList<EnumDefContext> getEnumlist() {
		return enumlist;
	}
	
	/**
	 * Retrieves all added classes
	 */
	public ArrayList<ClassDefContext> getClasslist() {
		return classlist;
	}
	
	/**
	 * Retrieves all added functions
	 */
	public ArrayList<FunctionContext> getFunclist() {
		return funclist;
	}
	
	/**
	 * Retrieves all added statements
	 * @return
	 */
	public ArrayList<StatContext> getStatlist() {
		return statlist;
	}
	
}
