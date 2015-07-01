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
	
	public void add(EnumDefContext ctx) {
		this.getEnumlist().add(ctx);
	}
	public void add(ClassDefContext ctx) {
		this.getClasslist().add(ctx);
	}
	public void add(FunctionContext ctx) {
		this.getFunclist().add(ctx);
	}
	public void add(StatContext ctx) {
		this.getStatlist().add(ctx);
	}
	public ArrayList<EnumDefContext> getEnumlist() {
		return enumlist;
	}
	public ArrayList<ClassDefContext> getClasslist() {
		return classlist;
	}
	public ArrayList<FunctionContext> getFunclist() {
		return funclist;
	}
	public ArrayList<StatContext> getStatlist() {
		return statlist;
	}
	
}
