package awesome.lang.checking;

import java.util.ArrayList;

import awesome.lang.GrammarParser.ClassDefContext;
import awesome.lang.GrammarParser.EnumDefContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.StatContext;

public class ContextDataSet {
	private ArrayList<EnumDefContext> enumlist = new ArrayList<EnumDefContext>(); 
	private ArrayList<ClassDefContext> classlist = new ArrayList<ClassDefContext>(); 
	private ArrayList<FunctionContext> funclist = new ArrayList<FunctionContext>(); 
	private ArrayList<StatContext> statlist = new ArrayList<StatContext>();
	
	public void add(EnumDefContext ctx) {
		this.enumlist.add(ctx);
	}
	public void add(ClassDefContext ctx) {
		this.classlist.add(ctx);
	}
	public void add(FunctionContext ctx) {
		this.funclist.add(ctx);
	}
	public void add(StatContext ctx) {
		this.statlist.add(ctx);
	}
	
}
