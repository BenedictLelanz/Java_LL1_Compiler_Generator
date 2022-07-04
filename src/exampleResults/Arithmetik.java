package exampleResults;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a compiler for Arithmetik with analysis phase and synthesis phase that was created by CompilerGenerator.
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
	
public class Arithmetik {
	
	// Are together the stream of token-lexeme-pairs
	private List<Token> streamToken = new ArrayList<>();
	private List<String> streamLexeme = new ArrayList<>();
	
	private int position = 0;
	
	public static void main(String[] args) {
		Arithmetik sp = new Arithmetik();
		sp.createTokenStream("12*(3+41)*(4+54)+1");
		String compiled = sp.E();
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
		
		EOF(""), IGNORE("[\\s\\t\\n\\x0B\\f\\r]"), Plus("\\+"), Mal("\\*"), Zahl("0|[1-9][0-9]*"), BrL("\\("), BrR("\\)");

		String regExp;

		Token(String regExp) {
			this.regExp = regExp;
		}

	}

	private String X() {
		String $$ = "";
		if (match(Token.EOF, Token.BrR)) {
			$$ = "";
		}
		else if (match(Token.Plus)) {
			$$ = accept(Token.Plus) + T() + X();
		}
		return $$;
	}

	private String F() {
		String $$ = "";
		if (match(Token.Zahl)) {
			$$ = accept(Token.Zahl);
		}
		else if (match(Token.BrL)) {
			$$ = accept(Token.BrL) + E() + accept(Token.BrR);
		}
		return $$;
	}

	private String Y() {
		String $$ = "";
		if (match(Token.EOF, Token.Plus, Token.BrR)) {
			$$ = "";
		}
		else if (match(Token.Mal)) {
			$$ = accept(Token.Mal) + F() + Y();
		}
		return $$;
	}

	private String E() {
		String $$ = "";
		if (match(Token.Zahl, Token.BrL)) {
			$$ = T() + X();
		}
		return $$;
	}

	private String T() {
		String $$ = "";
		if (match(Token.Zahl, Token.BrL)) {
			$$ = F() + Y();
		}
		return $$;
	}

}
