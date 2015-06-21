package awesome.lang;

import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import awesome.lang.checking.TypeChecker;
import awesome.lang.model.Program;

/**
 * Combines Typechecker and Generator
 */
public class Compiler {
	public Program compile(String program) throws CompilationException {
		ANTLRInputStream ips = new ANTLRInputStream(program);
		
		// tokenize+parse
		GrammarLexer lexer = new GrammarLexer(ips);
		TokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);
		ParseTree tree = parser.program();
		
		// walk through
		TypeChecker checker = new TypeChecker();
		new ParseTreeWalker().walk(checker, tree);
		
		if (checker.getErrors().size() > 0) {
			throw new CompilationException("Error(s) during type checking", checker.getErrors());
		}
		
		Generator generator = new Generator(checker.getSymbolTable());
		return generator.genProgram(tree);
	}
	
	public class CompilationException extends Exception {
		private static final long serialVersionUID = -5100147228313379450L;
		private ArrayList<String> errors;
		
		public CompilationException(String text, ArrayList<String> errors) {
			super(text);
			this.errors = errors;
		}
		
		public ArrayList<String> getErrors() {
			return errors;
		}
	}
}
