package awesome.lang;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import awesome.lang.GrammarParser.ProgramContext;

public class Util {
	/**
	 * change escaped string to actual values 
	 */
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
				builder.append(getEscapedChar(chr));
				
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
	
	public static char extractChar(TerminalNode chr) {
		if (chr.getSymbol().getType() != GrammarLexer.CHARLITERAL) {
			throw new IllegalArgumentException("Supplied token is not a char token. ");
		}
		
		String content = chr.getText().substring(1, chr.getText().length() - 1);
		
		if(content.length() == 1) {
			//unescaped char
			return content.charAt(0);
		} else {
			//escaped char
			return getEscapedChar(content.charAt(1));
		}
	}
	
	private static char getEscapedChar(char c) {
		switch (c) {
		case 't':
			return '\t';
		case 'r':
			return '\r';
		case 'n':
			return '\n';
		case '\\':
			return '\\';
		case '"':
			return '"';
		case '0':
			return (char) 0;
		default:
			throw new IllegalArgumentException("Unknown escape character: '" + c + "'");
		}
	}
	
	public static ProgramContext parseProgram(CharStream stream) {
		GrammarLexer lexer = new GrammarLexer(stream);
		ErrorListener listener = new ErrorListener();
		lexer.addErrorListener(listener);
		TokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);
		parser.addErrorListener(listener);
		ProgramContext program = parser.program();
		return listener.error ? null : program;
	}
	
	private static class ErrorListener extends BaseErrorListener {
		private boolean error = false;
		
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			error = true;
		}
	}
}
