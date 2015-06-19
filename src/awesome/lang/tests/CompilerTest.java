package awesome.lang.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Assert;
import org.junit.Test;

import awesome.lang.Generator;
import awesome.lang.GrammarLexer;
import awesome.lang.GrammarParser;
import awesome.lang.model.Program;

public class CompilerTest {
	private Generator generator = new Generator();
	
	@Test
	public void testBasic() {
		parse("{}");
		parse("{int i = 0;}");
		parse("{int i = 1+1+2+3+4;}");
	}
	
	@Test
	public void testCompile() throws IOException, InterruptedException {
		testProgram("{int i = 0;}", "");
//		testProgram("{int x = 1;print(x);}", "1");
		testProgram("{print(6);}", "6");
		testProgram("{print(2+2+3);}", "7");
	}
	
	@Test
	public void testIf() throws IOException, InterruptedException {
		testProgram("{ if(true) print(4); }", "4");
		testProgram("{ if(false) print(4); else print(3); }", "3");
//		testProgram("{ int x = 1;if(x==1) print(4); }", "4");
	}
	
	private void testProgram(String prog, String expected) throws IOException, InterruptedException {
		parse(prog).writeSprockell("test.hs");
		
		Process process = Runtime.getRuntime().exec("ghc -isprockell/src gen/test.hs -e main");

		String output = getOutput(process.getInputStream());
		String err = getOutput(process.getErrorStream());
		
		int exitCode = process.waitFor();
		if(exitCode != 0){
			System.out.println(err);
			Assert.fail("Haskell compilation failed");
		}
		
		Assert.assertEquals("program output", expected, output);
	}
	
	private String getOutput(InputStream stream) throws IOException {
		String output;
		
		try {
			Scanner scanner = new Scanner(stream);
			scanner.useDelimiter("\\A");
			output = scanner.next();
			
			scanner.close();
		} catch(NoSuchElementException e){
			//scanner will throw this exception when there is no output
			output = "";
		}
		
		return output;
	}
	
	private Program parse(String text) {
		Program program = generator.genProgram(parse(new ANTLRInputStream(text)));
		//System.out.println(program.generateSprockell());
		return program;
	}
	
	private ParseTree parse(CharStream chars) {
		Lexer lexer = new GrammarLexer(chars);
		TokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);
		ParseTree result = parser.program();
		return result;
	}
}
