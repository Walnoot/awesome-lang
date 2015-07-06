package awesome.lang;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import awesome.lang.checking.CompilationUnit;
import awesome.lang.checking.TypeChecker;
import awesome.lang.model.Program;
import awesome.lang.model.Type;

/**
 * Combines ImportResolver, Typechecker and Generator
 */
public class Compiler {
	
	/**
	 * Compiles a program by String path 
	 */
	public Program compile(String program) throws CompilationException {
		return build(new ImportResolver(program));
	}
	
	/**
	 * Compiles a program by Path-object path 
	 */
	public Program compile(Path path) throws CompilationException {
		return build(new ImportResolver(path));
	}

	/**
	 * Compiles a program, based on the given importresolver 
	 */
	private Program build(ImportResolver resolver) throws CompilationException {
		CompilationUnit cUnit = resolver.getContextDataSet();
		
		Type.clearUserTypes();
		TypeChecker checker = new TypeChecker();
		checker.checkProgram(cUnit);
		
		if (checker.getErrors().size() > 0) {
			throw new CompilationException("Error(s) during type checking", checker.getErrors());
		}
		
		
		Generator generator = new Generator(checker.getSymbolTable(), checker.getFunctionTable(), checker.getExpressionTypes());
		return generator.genProgram(cUnit);
	}
	
	public class CompilationException extends Exception {
		private static final long serialVersionUID = -5100147228313379450L;
		private ArrayList<String> errors;
		
		public CompilationException(String text, ArrayList<String> errors) {
			super(text + ": " + errors);
			this.errors = errors;
		}
		
		public ArrayList<String> getErrors() {
			return errors;
		}
	}
	
	/**
	 * Compiles a program and writes the generated haskell code to the gen folder in project root.
	 * If the first argument is -example, it searches relative to the examples directory.
	 * Example usage: "-example gameoflife.awl" compiles the game of life example program and writes
	 * it to gen/gameoflife.awl
	 */
	public static void main(String[] args) throws CompilationException, IOException {
		Path path;
		
		if(args[0].equals("-example")) {
			path = Paths.get("src/awesome/lang/examples/", args[1]);
		} else {
			path = Paths.get(args[0]);
		}
		
		String name = path.getFileName().toString().split("\\.")[0];
		new Compiler().compile(path).writeSprockell(name + ".hs");
	}
}
