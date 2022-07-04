package cg;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * CompilerGenerator creates scanner and parser with syntax driven translation 
 * for the given reduced LL(1) grammar.
 * 
 * Nonterminals and tokens have to be defined in the enum Symbol.
 * Rules have to be defined in the method addRules().
 * 
 * When the program terminates a new Java file will appear in the source folder.
 * 
 * If the grammar isn't LL(1) the accepting of a word isn't guaranteed because 
 * the generator writes down the production rules in a random sequence and the final 
 * compiler takes the production rule that appears first.
 * 
 * The example grammar is ZR (Zeichenroboter) 
 * 
 * @author Benedict Lelanz
 */

public class CompilerGenerator {	
	
	private HashMap<Symbol, Set<List<Symbol>>> rules = new HashMap<>();
	
	public static void main(String[] args) throws IOException {
		
		CompilerGenerator cg = new CompilerGenerator();
		cg.defineGrammar();
		// Start symbol, destination path, class name (Filename without '.java' has to be the same!)
		cg.generate(Symbol.Programm, "Zeichenroboter.java", "Zeichenroboter");
		
	}
	
	/**
	 * Here the terminals and nonterminals have to be defined
	 * @author Benedict Lelanz
	 *
	 */
	
	private enum Symbol {
		
		// EOF = End of file, EPSILON and IGNORE are necessary symbols
		// EPSILON doesn't have to be appear in the grammar
		EOF(), EPSILON(), IGNORE(),
		
		// Your nonterminals (Note the start symbol with 'S', all other nonterminals with 'N')
		Programm('S'), Anweisungen('N'), Anweisung('N'), Farbwert('N'),
		
		// Your token (Symbols that have their own meaning in regExp like [ require \\\\ before!)
		Farbe("Farbe"), VW("VW"), RE("RE"), Zahl("0|[1-9][0-9]*"), WH("WH"), BrL("\\\\["), BrR("\\\\]");
		
		private Character NT;
		private String regExp = null;
		
		Symbol(){
			this.NT = '0';
		}
		
		Symbol(Character NT) {
			this.NT = NT;
		}
		
		Symbol(String regExp) {
			this.regExp = regExp;
			this.NT = 'T';			
		}
		
		boolean isTerminal() {
			return NT == 'T';
		}
		
		boolean isNonterminal() {
			return NT == 'N' || NT == 'S';
		}
		
		boolean isStartsymbol() {
			return NT == 'S';
		}
		
		String getRegExp() {
			return regExp;
		}
		
	}
	
	/**
	 * For defining grammar please chose the syntax: 
	 * addRule(Symbol.YourLeftSide, Symbol.ElementOfRightSide... );
	 * Example: addRule(Symbol.A, Symbol.a, Symbol.A, Symbol.b);
	 */
	
	private void defineGrammar() {
		addRule(Symbol.Programm, Symbol.Anweisungen);
		addRule(Symbol.Anweisungen, Symbol.Anweisung, Symbol.Anweisungen);
		addRule(Symbol.Anweisungen, Symbol.EPSILON);
		addRule(Symbol.Farbwert, Symbol.Zahl, Symbol.Zahl, Symbol.Zahl);
		addRule(Symbol.Anweisung, Symbol.VW, Symbol.Zahl);
		addRule(Symbol.Anweisung, Symbol.RE, Symbol.Zahl);
		addRule(Symbol.Anweisung, Symbol.WH, Symbol.Zahl, Symbol.BrL, Symbol.Anweisungen, Symbol.BrR);
		addRule(Symbol.Anweisung, Symbol.WH, Symbol.Farbwert);
	}	
	
	/**
	 * Adds a rule to the rule HashMap
	 * @param left is the left side of a production rule
	 * @param right is an array of symbols that matches the right side of a production rule
	 */
	
	private void addRule(Symbol left, Symbol... right) {
		try {
			List<Symbol> rightside = new ArrayList<>();
			for (Symbol r : right) {
				rightside.add(r);
			}
			rules.get(left).add(rightside);
		} catch (Exception e) {
			rules.put(left, new HashSet<List<Symbol>>());
			addRule(left, right);
		}
	}
	
	/**
	 * Determines with a recursive algorithm the follow set of the given nonterminal
	 * @param s is nonterminal on which the follow set should be determined
	 * @param determined is a set of symbol that already have been in the process of follow 
	 * @return a set of symbols that matches the follow set
	 */
	
	private Set<Symbol> follow(Symbol s, Set<Symbol> determined) {
		Set<Symbol> result = new HashSet<>();
		if (s.isStartsymbol()) {
			result.add(Symbol.EOF);
		}
		for (Symbol left : rules.keySet()) {
			for (List<Symbol> right : rules.get(left)) {
				if (right.contains(s)) {
					if (right.indexOf(s)+1 != right.size()) {
						result.addAll(first(right.subList(right.indexOf(s)+1, right.size())));
					} else {
						result.add(Symbol.EPSILON);
					}
				}
			}
			if (result.contains(Symbol.EPSILON)) {
				result.remove(Symbol.EPSILON);
				if (!determined.contains(left)) {
					determined.add(left);
					result.addAll(follow(left, determined));
				}				
			}
		}
		return result;		
	}
	
	/**
	 * Determines with a recursive algorithm the first set of the given sentence
	 * @param sentence is a right side of a production rule
	 * @return a set of symbols that is the first set
	 */
	
	private Set<Symbol> first(List<Symbol> sentence) {	
		
		Set<Symbol> result = new HashSet<>();
		
		// Case 0: sentence is empty
		if (sentence.size() == 0) {
			return result;
			
		} else if (sentence.size() == 1) {
			
			// Case 1: FIRST(EPSILON) = {EPSILON}
			if (sentence.get(0) == Symbol.EPSILON) {				
				result.add(Symbol.EPSILON);
				return result;
			}
			
			// Case 2: FIRST(Terminal a) = {a}
			if (sentence.get(0).isTerminal()) {
				result.add(sentence.get(0));
				return result;
			}
			
			// Case 3: a -> b1 | b2 | ... | bn
			if (sentence.get(0).isNonterminal()) {
				for (List<Symbol> right : rules.get(sentence.get(0))) {
					result.addAll(first(right));
				}
				return result;
			}
			
		}			
		
		// Case 4: a = X1X2...Xn and first(X1) doesn't contain EPSILON
		if (!first(sentence.subList(0, 1)).contains(Symbol.EPSILON)) {
			result.addAll(first(sentence.subList(0, 1)));
			return result;
		}
		
		// Case 5: a = X1X2...Xn and every first(X) contains EPSILON
		for (int i = 0; i < sentence.size()-1; i++) {
			if (!first(sentence.subList(i, i+1)).contains(Symbol.EPSILON)) {
				break;
			}
			if (i == sentence.size()-2) {
				result.addAll(first(sentence.subList(0, 1)));
				result.addAll(first(sentence.subList(1, sentence.size())));
				return result;
			}
		}
		
		// Case 6: else-case
		result.addAll(first(sentence.subList(0, 1)));
		result.addAll(first(sentence.subList(1, sentence.size())));
		return result;
			
	}
	
	/**
	 * Generates scanner and parser for the given grammar and saves a java file at the given path
	 * @param start is the start symbol of the grammar
	 * @param path here the destination file will be saved
	 * @param classname is the class name of the destination file
	 * @throws IOException if an error occurs while writing
	 */
	
	private void generate(Symbol start, String path, String classname) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		
		// Main code
		writer.write("import java.util.ArrayList;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "/**\r\n"
				+ " * This is a compiler for " + classname + " with analysis phase and synthesis phase that was created by CompilerGenerator.\r\n"
				+ " * \r\n"
				+ " * Below are methods for each rule your grammar has. \r\n"
				+ " * There are only simple semantic rules that define their attributes with their successor nodes.\r\n"
				+ " * You can expand it by adding your own semantic rules.\r\n"
				+ " * \r\n"
				+ " * You can use it in the following way: \r\n"
				+ " * - Enter a string representing your input in the main method into sp.createTokenStream(String input)\r\n"
				+ " * - After processing you will get an output that may be like this:\r\n"
				+ " * 	- Your word without whitespace which means that the compiler ended up with success\r\n"
				+ " * 	- A message that tells you that some lexical error occurred\r\n"
				+ " * 	- A message that tells you that some other token was expected at a certain position\r\n"
				+ " * \r\n"
				+ " *  @author CompilerGenerator\r\n"
				+ " *\r\n"
				+ " */\r\n"
				+ "	\r\n"
				+ "public class " + classname + " {\r\n"
				+ "	\r\n"
				+ "	// Are together the stream token-lexeme-pairs\r\n"
				+ "	private List<Token> streamToken = new ArrayList<>();\r\n"
				+ "	private List<String> streamLexeme = new ArrayList<>();\r\n"
				+ "	\r\n"
				+ "	private int position = 0;\r\n"
				+ "	\r\n"
				+ "	public static void main(String[] args) {\r\n"
				+ "		" + classname + " sp = new " + classname + "();\r\n"
				+ "		sp.createTokenStream(args[0]);\r\n"
				+ "		String compiled = sp." + start.toString() + "();\r\n"
				+ "		sp.accept(Token.EOF);\r\n"
				+ "		System.out.println(compiled);\r\n"
				+ "	}\r\n"
				+ "	\r\n"
				+ "	/**\r\n"
				+ "	 * Creates the token stream with usage of longest prefix strategy\r\n"
				+ "	 * @param input is the word that will be converted into a stream of token\r\n"
				+ "	 */\r\n"
				+ "	\r\n"
				+ "	private int createTokenStream(String input) {\r\n"
				+ "		if (input.length() == 0) {\r\n"
				+ "			return 1;\r\n"
				+ "		}\r\n"
				+ "		for (int i = input.length(); i > 0; i--) {	\r\n"
				+ "			for (Token t : Token.values()) {\r\n"
				+ "				if (input.substring(0, i).matches(t.regExp)) {\r\n"
				+ "					if (t != Token.IGNORE) {\r\n"
				+ "						streamToken.add(t);\r\n"
				+ "						streamLexeme.add(input.substring(0, i));\r\n"
				+ "					}\r\n"
				+ "					return createTokenStream(input.substring(i));\r\n"
				+ "				}\r\n"
				+ "			}\r\n"
				+ "		}\r\n"
				+ "		error(\"A lexical error occured!\");\r\n"
				+ "		return -1;\r\n"
				+ "	}\r\n"
				+ "	\r\n"
				+ "	/**\r\n"
				+ "	 * Determine the token at the current position and increases the current position\r\n"
				+ "	 * @return The token at the current position\r\n"
				+ "	 */\r\n"
				+ "	\r\n"
				+ "	private Token getNextToken() {\r\n"
				+ "		try {\r\n"
				+ "			return streamToken.get(position++);\r\n"
				+ "		} catch(IndexOutOfBoundsException e) {\r\n"
				+ "			position--;\r\n"
				+ "			return Token.EOF;\r\n"
				+ "		}\r\n"
				+ "	}\r\n"
				+ "	\r\n"
				+ "	/**\r\n"
				+ "	 * Checks whether the received token matches the expected token or not\r\n"
				+ "	 * @param received the given token\r\n"
				+ "	 * @return a string that is the lexeme that matches the token\r\n"
				+ "	 * @throws ParsingError if the received token doesn't match the expected token\r\n"
				+ "	 */\r\n"
				+ "	\r\n"
				+ "	private String accept(Token expected) {\r\n"
				+ "		Token received = getNextToken();\r\n"
				+ "		if (received != expected) {\r\n"
				+ "			error(\"Expected token at position \" + (position-1) + \": \" + expected + \". \"\r\n"
				+ "					+ \"Received token: \" + received + \".\");\r\n"
				+ "			return \"\";\r\n"
				+ "		} else {\r\n"
				+ "			return streamLexeme.get(position-1);\r\n"
				+ "		}\r\n"
				+ "	}\r\n"
				+ "	\r\n"
				+ "	/**\r\n"
				+ "	 * Prints an error and calls the system exit method\r\n"
				+ "	 * @param msg the error message\r\n"
				+ "	 */\r\n"
				+ "	\r\n"
				+ "	private void error(String msg) {\r\n"
				+ "		System.out.println(msg);\r\n"
				+ "		System.exit(-1);\r\n"
				+ "	}\r\n"
				+ "	\r\n"
				+ "	/**\r\n"
				+ "	 * Checks whether one of the given tokens matches the current token\r\n"
				+ "	 * @param t given tokens for checking\r\n"
				+ "	 * @return true if one of the tokens mets the conditions\r\n"
				+ "	 */\r\n"
				+ "	\r\n"
				+ "	private boolean match(Token... t) {\r\n"
				+ "		for (Token check : t) {\r\n"
				+ "			try {\r\n"
				+ "				if (streamToken.get(position) == check) return true;\r\n"
				+ "			} catch(IndexOutOfBoundsException e) {}\r\n"
				+ "		}\r\n"
				+ "		return false;\r\n"
				+ "	}\r\n"
				+ "	\r\n"
				+ "	private enum Token {\r\n"
				+ "		\r\n"
				+ "		EOF(\"\"), IGNORE(\"[\\\\s\\\\t\\\\n\\\\x0B\\\\f\\\\r]\")");
		
		// Add all token
		for (Symbol s: Symbol.values()) {
			if (s.isTerminal()) {
				writer.write(", " + s.toString() + "(\"" + s.getRegExp() + "\")");
			}
		}
		
		// Add the token constructor
		writer.write(";\r\n\r\n"
				+ "		String regExp;\r\n"
				+ "\r\n"
				+ "		Token(String regExp) {\r\n"
				+ "			this.regExp = regExp;\r\n"
				+ "		}\r\n"
				+ "\r\n"
				+ "	}\r\n\r\n");
		
		// Add methods for nonterminals
		for (Symbol s : rules.keySet()) {			
			// Head of a method
			writer.write("	private String " + s.toString() + "() {\r\n		String $$ = \"\";\r\n");	
			// Boolean for checking whether if or if else is helpful
			boolean elseIf = false;
			// Iterate through all right sides
			for (List<Symbol> right : rules.get(s)) {
				// Check condition match
				if (elseIf) {
					writer.write("		else if (match(");
				} else {
					writer.write("		if (match(");
					elseIf = true;
				}				
				// Determine if first contains EPSILON -> Add follow(current left side s)
				Set<Symbol> first = first(right);
				if (first.contains(Symbol.EPSILON)) {
					first.remove(Symbol.EPSILON);
					first.addAll(follow(s, new HashSet<Symbol>()));
				}
				Iterator<Symbol> iterator = first.iterator();
				// Fill the match block
				if (iterator.hasNext()) {
					writer.write("Token." + iterator.next().toString());
				}
				while (iterator.hasNext()) {
					writer.write(", Token." + iterator.next().toString());
				}
				writer.write(")) {\r\n			$$ = ");
				// Write commands for terminals and nonterminals
				Iterator<Symbol> iterator1 = right.iterator();
				if (iterator1.hasNext()) {
					Symbol t = iterator1.next();
					if (t.isTerminal()) {
						writer.write("accept(Token."+t.toString()+")");
					} else if (t.isNonterminal()) {
						writer.write(t.toString()+"()");
					} else {
						writer.write("\"\"");
					}
				}
				while (iterator1.hasNext()) {
					Symbol t = iterator1.next();
					if (t.isTerminal()) {
						writer.write(" + accept(Token."+t.toString()+")");
					} else if (t.isNonterminal()) {
						writer.write(" + " + t.toString()+"()");
					}
				}
				writer.write(";\r\n		}\r\n");
			}
			writer.write("		return $$;\r\n	}\r\n\r\n");			
		}
		writer.write("}\r\n");	
		
		writer.close();
		
	}
	
}
