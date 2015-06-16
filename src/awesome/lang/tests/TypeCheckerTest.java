package awesome.lang.tests;

import static org.junit.Assert.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

import awesome.lang.GrammarLexer;
import awesome.lang.GrammarParser;
import awesome.lang.checking.TypeChecker;

public class TypeCheckerTest {

	@Test
	public void test() {
		
		// read file into ANTLR
		FileReader fp;
		ANTLRInputStream ips;
		
		try {
			fp = new FileReader("src/awesome/lang/examples/basic.awl");
			ips = new ANTLRInputStream(fp);
	 		fp.close();
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
			return;
		}
		
		// tokenize+parse
		GrammarLexer lexer = new GrammarLexer(ips);
		TokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);
 		ParseTree tree = parser.program();
 		
 		// walk through
 		TypeChecker listener = new TypeChecker();
 		(new ParseTreeWalker()).walk(listener, tree);
 		
 		// list errors
 		ArrayList<String> errors = listener.getErrors();
 		for (String s : errors) {
 			System.out.println(s);
 		}
 		if (errors.isEmpty())
 			System.out.println("Valid program, no type-errors detected!");
		
		
		
		
	}

}
