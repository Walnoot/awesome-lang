package awesome.lang;

import java.io.IOException;
import java.util.ArrayList;

import awesome.lang.checking.TypeChecker;
import awesome.lang.model.Program;

/**
 * Combines Typechecker and Generator
 */
public class Compiler {
	public Program compile(String program) throws CompilationException {
//		ParseTree tree = Util.parseProgram(ips);
		ImportResolver resolver = new ImportResolver(program);
		
		// walk through
		TypeChecker checker = new TypeChecker();
		checker.checkProgram(resolver.getFunctions(), resolver.getStatements(), resolver.getEnums());
		
		if (checker.getErrors().size() > 0) {
			throw new CompilationException("Error(s) during type checking", checker.getErrors());
		}
		
		Generator generator = new Generator(checker.getSymbolTable(), checker.getFunctionTable());
		return generator.genProgram(resolver.getFunctions(), resolver.getStatements());
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
	
	public static void main(String[] args) throws CompilationException, IOException {
		String prog = "";
		for (int i = 0; i < args.length; i++) {
			if(i != 0) prog += " ";
			prog += args[i];
		}
		
		System.out.printf("Compiling %s\n", prog);
		
		Program program = new Compiler().compile(prog);
		program.writeSprockell("comp.hs");
	}
}
