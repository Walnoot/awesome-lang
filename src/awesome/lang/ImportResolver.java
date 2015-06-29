package awesome.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;

import awesome.lang.GrammarParser.EnumDefContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.ImprtContext;
import awesome.lang.GrammarParser.ProgramContext;
import awesome.lang.GrammarParser.StatContext;

public class ImportResolver extends GrammarBaseVisitor<Void> {
	private ArrayList<StatContext> statements = new ArrayList<StatContext>();
	private ArrayList<FunctionContext> functions = new ArrayList<FunctionContext>();
	private ArrayList<EnumDefContext> enums = new ArrayList<EnumDefContext>();
	
	//list of files that were imported, to resolve circular dependencies
	private ArrayList<String> imports = new ArrayList<String>();
	
	//directory of the main program
	private Path mainDir;
	
	/**
	 * Finds all imported programs. Path arguments is used to resolve relative
	 * paths.
	 */
	public ImportResolver(Path filePath) {
		mainDir = filePath.getParent();
		
		importDefault();
		attemptImport(filePath);
	}
	
	public ImportResolver(String program) {
		//search relative to working directory
		mainDir = Paths.get("/");
		
		importDefault();
		visitProgram(Util.parseProgram(new ANTLRInputStream(program)));
	}
	
	private void importDefault() {
		attemptImport(Paths.get("stdlib", "default.awl"));
	}
	
	@Override
	public Void visitProgram(ProgramContext ctx) {
		for (ImprtContext imprt : ctx.imprt()) {
			String file = Util.extractString(imprt.STRING());
			
			if (!attemptImport(mainDir.resolve(file))) {
				System.err.printf("Unable to resolve import \"%s\"\n", file);
			}
		}
		
		for(EnumDefContext enu : ctx.enumDef()) {
			enums.add(enu);
		}
		
		for (StatContext stat : ctx.stat()) {
			statements.add(stat);
		}
		
		for (FunctionContext func : ctx.function()) {
			functions.add(func);
		}
		
		return null;
	}
	
	private boolean attemptImport(Path path) {
		File file = path.toFile();
		
		if (imports.contains(file.getName())) {
			return true;
		} else {
			imports.add(file.getName());
		}
		
		try {
			ProgramContext program = Util.parseProgram(new ANTLRInputStream(new FileReader(file)));
			program.accept(this);
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public ArrayList<StatContext> getStatements() {
		return statements;
	}

	public ArrayList<FunctionContext> getFunctions() {
		return functions;
	}
	
	public ArrayList<EnumDefContext> getEnums() {
		return enums;
	}
	
	/**
	 * @return All the files that were included.
	 */
	public ArrayList<String> getImports() {
		return imports;
	}
}
