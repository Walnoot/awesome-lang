package awesome.lang.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
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
	}
	
	public Program parse(String text) {
		return generator.genProgram(parse(new ANTLRInputStream(text)));
	}
	
	public Program parse(File file) throws IOException {
		return generator.genProgram(parse(new ANTLRInputStream(new FileReader(file))));
	}
	
	private ParseTree parse(CharStream chars) {
		Lexer lexer = new GrammarLexer(chars);
		TokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);
		ParseTree result = parser.program();
		return result;
	}
}
