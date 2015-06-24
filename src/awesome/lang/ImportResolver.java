package awesome.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;

import awesome.lang.GrammarParser.FunctionContext;
import awesome.lang.GrammarParser.ImprtContext;
import awesome.lang.GrammarParser.ProgramContext;
import awesome.lang.GrammarParser.StatContext;

public class ImportResolver extends GrammarBaseVisitor<Void> {
	private ArrayList<StatContext> statements;
	private ArrayList<FunctionContext> functions;
	
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
		
		attemptImport(filePath);
	}
	
	@Override
	public Void visitProgram(ProgramContext ctx) {
		for (ImprtContext imprt : ctx.imprt()) {
			String file = Util.extractString(imprt.STRING());
			
			//see if this program was already imported
			boolean isNew = true;
			for (String resolved : imports) {
				if (resolved.equals(file)) {
					isNew = false;
					break;
				}
			}
			
			if (isNew) {
				//first attempt searching relative to working directory
				boolean success = attemptImport(Paths.get(file));
				
				//then attempt to search relative to directory of main program
				if (!success) {
					success = attemptImport(mainDir.resolve(file));
				}
				
				if (!success) {
					System.err.printf("Unable to resolve import \"%s\"\n", file);
				}
			}
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
}
