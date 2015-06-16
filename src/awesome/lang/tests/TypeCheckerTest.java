package awesome.lang.tests;

import static org.junit.Assert.*;

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
	public void testExamples() {

		assertEquals(0, doTest(programValid).size());
		assertEquals(1, doTest(programInvalid1).size());
		assertEquals(3, doTest(programInvalid2).size());
		assertEquals(1, doTest(programInvalid3).size());
		assertEquals(1, doTest(programInvalid4).size());
		
	}
	
	
	private ArrayList<String> doTest(String input) {
		
		// read file into ANTLR
		ANTLRInputStream ips = new ANTLRInputStream(input);
		
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
 		if (errors.size() > 0)
 			System.out.println("============================================ Errors (=expected) ============================================");
 		for (String s : errors) {
 			System.out.println(s);
 		}
 		
 		return errors;
		
	}

	final String programValid = "{"
							  + "	bool test2 = true;"
							  + "	bool test1;"
							  + "	if (test2)"
							  + "		test1 = not test2;"
							  + "	else {"
							  + "		asm \"debug \"\"Test is false\"\"\";"
							  + "	}"
							  +	"	if (test1) {"
							  + "		int test2 = 5;"
							  + "	}"
							  + "}";

	final String programInvalid1 = "{"
							     + "	bool test2 = true;"
							     + "	bool test1;"
							     + "	if (test2 < test1)"
							     + "		test1 = not test2;"
							     + "}";

	final String programInvalid2 = "{"
							     + "	bool test2 = true;"
							     + "	bool test1;"
							     + "	if (test2)"
							     + "		test1 = -test3;"
							     + "}";

	final String programInvalid3 = "{"
							     + "	bool test2 = true;"
							     +	"	if (10) {"
							     + "		int test2 = 5;"
							     + "	}"
							     + "}";

	final String programInvalid4 = "{"
							     + "	bool test2 = true;"
							     + "	bool test1;"
							     +	"	if (test1) {"
							     + "		int test2 = 5;"
							     + "	}"
							     + "	int test2 = 5;"
							     + "}";


	
}
