package awesome.lang;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import awesome.lang.GrammarParser.ProgramContext;

public class Util {
	public static String extractString(TerminalNode string) {
		if (string.getSymbol().getType() != GrammarLexer.STRING) {
			throw new IllegalArgumentException("Supplied token is not a string token. ");
		}
		
		String content = string.getText().substring(1, string.getText().length() - 1);
		StringBuilder builder = new StringBuilder();
		
		boolean escaping = false;
		for (int i = 0; i < content.length(); i++) {
			char chr = content.charAt(i);
			
			if (escaping) {
				switch (chr) {
				case 't':
					builder.append('\t');
					break;
				case 'r':
					builder.append('\r');
					break;
				case 'n':
					builder.append('\n');
					break;
				case '\\':
					builder.append('\\');
					break;
				case '"':
					builder.append('"');
					break;
				default:
					throw new IllegalArgumentException("Unknown escape character: '" + chr + "'");
				}
				
				escaping = false;
			} else {
				if (chr == '\\')
					escaping = true;
				else
					builder.append(chr);
			}
		}
		
		return builder.toString();
	}
	
	public static ProgramContext parseProgram(CharStream stream){
		GrammarLexer lexer = new GrammarLexer(stream);
		TokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);
		return parser.program();
	}
}
