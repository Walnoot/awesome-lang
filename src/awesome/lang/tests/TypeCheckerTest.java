package awesome.lang.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

import com.sun.org.apache.xalan.internal.xsltc.compiler.CompilerException;

import awesome.lang.Compiler.CompilationException;
import awesome.lang.GrammarLexer;
import awesome.lang.GrammarParser;
import awesome.lang.Compiler;
import awesome.lang.ImportResolver;
import awesome.lang.checking.CompilationUnit;
import awesome.lang.checking.TypeChecker;
import awesome.lang.model.Program;
import awesome.lang.model.Type;

// if any test has more errors than you would expect, remind that any expression with an undeterminable type will be addressed as boolean.
public class TypeCheckerTest {
	
	@Test
	public void testScopes() {

		doTest(1, "bool test2 = true; bool test1; if (test2 < test1) test1 = not test2;");
		doTest(2, "bool test2 = true; bool test1; if (test2) test1 = -test3;");
		doTest(1, "bool test2 = true; bool test1;if (test1) { int test2 = 5; } int test2 = 5;");
		
	}
	
	@Test
	public void testParse() throws CompilationException {
		doTest(1, "float i = 0;");
		doTest(1, "int i = 1+1+int(2.0)+3+4.0;");
	}
	
	@Test
	public void testAssignment() throws IOException, InterruptedException, CompilationException {
		doTest(1, "int i = int(0); i = 7;");
		doTest(1, "int i; if(false) i = 'a'; else i = 4;");
	}
	
	@Test
	public void testCompile() throws IOException, InterruptedException, CompilationException {
		doTest(2, "[char] x = \"2+2\"+'3';");
		doTest(2, "int x = 1; int y = x+y;");
	}
	
	@Test
	public void testSwitch() throws IOException, InterruptedException, CompilationException {
		doTest(2, "int i = 5;"
				+ "switch(i) {"
				+ "		case 5.0 { next; }"
				+ "		case 6 { }"
				+ "		case 7 { }"
				+ "		default{ next; }"
				+ "}");
		doTest(1, "int i = 5;"
				+ "int j = 5;"
				+ "switch(i) {"
				+ "		case 5 { j = 10; next; }"
				+ "		case 6 { if (j == 10) next; }"
				+ "		case 7 { next; }"
				+ "}");

	}
	@Test
	public void testIf() throws IOException, InterruptedException, CompilationException {
		doTest(1, "if(10) int x;");
		doTest(1, "float x = 1.0;if(x==1) int y;");
		doTest(1, "if(not 5) int y;");
	}
	
	@Test
	public void testWhile() throws IOException, InterruptedException, CompilationException {
		doTest(1, "while(5){} ");
	}
	
	@Test
	public void testEnums() throws IOException, InterruptedException, CompilationException {
		doTest(1, "enum pers { michiel, jacco} if (pers:anotherone != pers:jacco) { }");
		doTest(1, "enum pers { michiel, jacco, michiel} pers x = pers:michiel; if(x == pers:michiel){}");
		doTest(1, "room location = room:basement; "
				  + "enum room { kitchen, livingroom, basement } "
				  + "enum room { repeated }");
	}

	@Test
	public void testArrays() throws IOException, InterruptedException, CompilationException {
		doTest(1, "[int] x = [5]; x[0] = 'a';");
		doTest(1, "[[int]] x = [[0,7], [1,2], ['a', '0']];");
	}

	@Test
	public void testStrings() throws IOException, InterruptedException, CompilationException {
		doTest(1, "print(\"ab\\c\");");
	}

	@Test
	public void testChars() throws IOException, InterruptedException, CompilationException {
		doTest(1, "[char] x = \"abc\"; x[0] = '\\c';");
	}

	@Test
	public void testFunctionsTypecheck() throws IOException, InterruptedException, CompilationException {
		doTest(1, "int i = 1; add(int a, bool b, int c):{ i = a; return 5; }");
		doTest(1, "int i = 1; int add(int a, bool b, int c):{ i = a; int b; b=a; if (false) return 1; else { } }");
		doTest(2, "int i = 7; bool add():{ i = 2; return true; } int add():{ return i; } ");
		doTest(1, "int i; int add():{ i = 2; return true; } ");
	}
	
	@Test
	public void testObjects() throws IOException, InterruptedException, CompilationException {
		doTest(2, "class Int {int x;} Int obj = new Int(); obj.notexisting = 4; ");
		doTest(1, "class Int {[int] x; int x; } Int obj = new Int(); ");
		doTest(1, "class Pair { "
				+ " int v1; int v2; "
				+ " thread setV1() : {  }"
				+ "}");
		doTest(1, "class Test {"
			+ "	int a;"
			+ "	int b;"
			+ " init (int b) : { this.b = b; }"
			+ "	int setA(int a) : {"
			+ "		this.a = a;"
			+ "		return 0;"
			+ "	}"
			+ "}"
			+ "Test t = new Test();");
		
		doTest(2, "class Test {"
				+ " int a;"
				+ " int b;"
				+ " init(int a, int b, int c): {"
				+ "  this.a = a;"
				+ "  this.b = b+c;"
				+ " }"
				+ " init(int a): {"
				+ "  this.a = a;"
				+ "  this.b = 0;"
				+ " }"
				+ "}"
				+ "Test obj = new Test(3,2,2,2);");
	}
	
	@Test
	public void testFloats() throws IOException, InterruptedException, CompilationException {
		doTest(2, "float x = 1.5 * 2;");
		doTest(2, "float x = 1.5 * int(2.0);");
	}
	
	@Test
	public void testThreads() throws IOException, InterruptedException, CompilationException {
		//if print wasnt atomic, the two numbers would be mixed
		doTest(2, "thread do1(): { return 1; } thread do2(): { } do1(); do2(1);");
	}
	
	@Test
	public void testFor() throws IOException, InterruptedException, CompilationException {
		doTest(1, "for (int i = 0; i + 10; i = i + 1) {  }");
	}
	
	@Test
	public void testDo() throws IOException, InterruptedException, CompilationException {
		doTest(1, "int i = 0; do { i = i + 1; } while(i + 10);");
	}

	
	private void doTest(int expectedNumErrors, String input) {
		
 		// clear old types
 		Type.clearUserTypes();
 		
 		// create listener/visitor
 		TypeChecker listener = new TypeChecker();
 		
		// fix imports
		ImportResolver imports = new ImportResolver(input);
		CompilationUnit cUnit = imports.getContextDataSet();
		listener.checkProgram(cUnit);
		
 		// list errors
 		ArrayList<String> errors = listener.getErrors();
 		if (errors.size() > 0)
 			System.out.println("============================================ Errors " + (expectedNumErrors ==  errors.size() ? "(=expected) " : "") + "============================================");
 		for (String s : errors) {
 			System.out.println(s);
 		}
 		
 		assertEquals(expectedNumErrors, errors.size());
		
	}
	
}
