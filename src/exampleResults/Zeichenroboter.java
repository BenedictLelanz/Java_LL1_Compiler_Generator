package exampleResults;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a compiler for Zeichenroboter with analysis phase and synthesis phase that was created by CompilerGenerator.
 * 
 * Below are methods for each rule your grammar has. 
 * There are only simple semantic rules that define their attributes with their successor nodes.
 * You can expand it by adding your own semantic rules.
 * 
 * You can use it in the following way: 
 * - Enter a string representing your input in the main method into sp.createTokenStream(String input)
 * - After processing you will get an output that may be like this:
 * 	- Your word without whitespace which means that the compiler ended up with success
 * 	- A message that tells you that some lexical error occurred
 * 	- A message that tells you that some other token was expected at a certain position
 * 
 *  @author CompilerGenerator
 *
 */
	
public class Zeichenroboter {
	
	// Are together the stream token-lexeme-pairs
	private List<Token> streamToken = new ArrayList<>();
	private List<String> streamLexeme = new ArrayList<>();
	
	private int position = 0;
	
	public static void main(String[] args) {
		Zeichenroboter sp = new Zeichenroboter();
		sp.createTokenStream("RE 45 Farbe 255 255 255 VW 100 Farbe 255 0 0 WH 4 [ VW 50 RE 90 ]");
		String compiled = sp.Programm();
		sp.accept(Token.EOF);
		System.out.println(compiled);
	}
	
	/**
	 * Creates the token stream with usage of longest prefix strategy
	 * @param input is the word that will be converted into a stream of token
	 */
	
	private int createTokenStream(String input) {
		if (input.length() == 0) {
			return 1;
		}
		for (int i = input.length(); i > 0; i--) {	
			for (Token t : Token.values()) {
				if (input.substring(0, i).matches(t.regExp)) {
					if (t != Token.IGNORE) {
						streamToken.add(t);
						streamLexeme.add(input.substring(0, i));
					}
					return createTokenStream(input.substring(i));
				}
			}
		}
		error("A lexical error occured!");
		return -1;
	}
	
	/**
	 * Determine the token at the current position and increases the current position
	 * @return The token at the current position
	 */
	
	private Token getNextToken() {
		try {
			return streamToken.get(position++);
		} catch(IndexOutOfBoundsException e) {
			position--;
			return Token.EOF;
		}
	}
	
	/**
	 * Checks whether the received token matches the expected token or not
	 * @param received the given token
	 * @return a string that is the lexeme that matches the token
	 * @throws ParsingError if the received token doesn't match the expected token
	 */
	
	private String accept(Token expected) {
		Token received = getNextToken();
		if (received != expected) {
			error("Expected token at position " + (position-1) + ": " + expected + ". "
					+ "Received token: " + received + ".");
			return "";
		} else {
			return streamLexeme.get(position-1);
		}
	}
	
	/**
	 * Prints an error and calls the system exit method
	 * @param msg the error message
	 */
	
	private void error(String msg) {
		System.out.println(msg);
		System.exit(-1);
	}
	
	/**
	 * Checks whether one of the given tokens matches the current token
	 * @param t given tokens for checking
	 * @return true if one of the tokens mets the conditions
	 */
	
	private boolean match(Token... t) {
		for (Token check : t) {
			try {
				if (streamToken.get(position) == check) return true;
			} catch(IndexOutOfBoundsException e) {}
		}
		return false;
	}
	
	private enum Token {
		
		EOF(""), IGNORE("[\\s\\t\\n\\x0B\\f\\r]"), Farbe("Farbe"), VW("VW"), RE("RE"), Zahl("0|[1-9][0-9]*"), WH("WH"), BrL("\\["), BrR("\\]");

		String regExp;

		Token(String regExp) {
			this.regExp = regExp;
		}

	}

	private String Farbwert() {
		String $$ = "";
		if (match(Token.Zahl)) {
			$$ = accept(Token.Zahl) + accept(Token.Zahl) + accept(Token.Zahl);
		}
		return $$;
	}

	private String Anweisung() {
		String $$ = "";
		if (match(Token.Farbe)) {
			$$ = accept(Token.Farbe) + Farbwert();
		}
		else if (match(Token.RE)) {
			$$ = accept(Token.RE) + accept(Token.Zahl);
		}
		else if (match(Token.VW)) {
			$$ = accept(Token.VW) + accept(Token.Zahl);
		}
		else if (match(Token.WH)) {
			$$ = accept(Token.WH) + accept(Token.Zahl) + accept(Token.BrL) + Anweisungen() + accept(Token.BrR);
		}
		return $$;
	}

	private String Programm() {
		String $$ = "";
		if (match(Token.EOF, Token.WH, Token.VW, Token.Farbe, Token.RE)) {
			$$ = Anweisungen();
		}
		return $$;
	}

	private String Anweisungen() {
		String $$ = "";
		if (match(Token.WH, Token.VW, Token.Farbe, Token.RE)) {
			$$ = Anweisung() + Anweisungen();
		}
		else if (match(Token.EOF, Token.BrR)) {
			$$ = "";
		}
		return $$;
	}

}
