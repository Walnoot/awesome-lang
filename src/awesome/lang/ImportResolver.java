package awesome.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;

import awesome.lang.GrammarParser.ClassDefContext;
import awesome.lang.GrammarParser.EnumDefContext;
import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.ImprtContext;
import awesome.lang.GrammarParser.ProgramContext;
import awesome.lang.GrammarParser.StatContext;
import awesome.lang.checking.CompilationUnit;

public class ImportResolver extends GrammarBaseVisitor<Void> {
	
	//list of files that were imported, to resolve circular dependencies
	private ArrayList<String> imports = new ArrayList<String>();
	
	//Store certain contexes which are defined in different global scopes
	private CompilationUnit contextDataSet = new CompilationUnit();
	
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
			this.contextDataSet.add(enu);
		}
		for (StatContext stat : ctx.stat()) {
			this.contextDataSet.add(stat);
		}
		for (FunctionContext func : ctx.function()) {
			this.contextDataSet.add(func);
		}
		for (ClassDefContext cls : ctx.classDef()) {
			this.contextDataSet.add(cls);
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
	
	public CompilationUnit getContextDataSet() {
		return this.contextDataSet;
	}
	
	/**
	 * @return All the files that were included.
	 */
	public ArrayList<String> getImports() {
		return imports;
	}
}
