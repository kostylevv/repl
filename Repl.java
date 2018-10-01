package com.kostylevv.repl;

import java.util.*;
import java.util.regex.*;

/**
 * The Repl class implements a simple REPL.
 * It supports addition and substraction operations, variables are also supported.
 * It calculates the expressions like these: 4 + 6 - 8, 2 - 3 - 4 and so on.
 * /vars commands shows assigned variables
 * /help command explains these operations
 * /exit command terminates application
 */

public class Repl {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		boolean cont = true;
		while (cont) {
			String line = scanner.nextLine();

			if (line != null && line.length() > 0 && line.trim().replace(" ","").length() > 0) {
				//processing commands
				if (line.startsWith("/")) {
					switch (line.trim()) {
						case "/help":
						System.out.println("Program calculates the expressions like these: 4 + 6 - 8, 2 - 3 - 4 and so on. " +
									"It supports both unary and binary minuses. Enter '/exit' to terminate program. Enter '/vars' to show variables.");
						break;

						case "/vars":
						System.out.println(showVariables());
						break;

						case "/exit":
						cont = false;
						break;

						default:
						System.out.println("Unsupported command");
					}
				//processing expression
				} else {
					try {
						Expression expr = new Expression(line);
						expr.eval(variables);
						System.out.println(expr.getResult());
					}  catch (NumberFormatException e) {
						System.out.println("Numbers in an expression should be in range of Integer type : -2^31 ... 2^31-1, got: " + e.getMessage());
					} catch (IllegalArgumentException e) {
						System.out.println(e.getMessage());
					}
				}
			}
        }
		System.out.println("Bye!");
	}

	//show assigned variables
	public static String showVariables() {
		if (variables.size() == 0) return "Variables are not set";
		StringBuilder result = new StringBuilder();
		result.append("Variables: ");
		for (String key : variables.keySet()) {
			result.append(key);
			result.append(" = ");
			result.append(variables.get(key));
			result.append(" ");
		}
		return result.toString();
	}


	private static Map<String, Integer> variables = new HashMap<String, Integer>();
}

//This class represents an expression
class Expression {

	//constructs a new Expression object with raw expression string
	public Expression(String rawLine) {
		try {

			infixLine = atomize(rawLine);

		} catch (IllegalArgumentException iae) {
			throw iae;
		}
	}

	public String getResult() {
		StringBuilder sb = new StringBuilder();
		if (expAssignsValueToVariable) {
			sb.append(assignedVariableName);
			sb.append(" = ");
			sb.append(result);
		} else {
			sb.append(result);
		}
		return sb.toString();
	}

	public void eval(Map<String, Integer> variables) {
		if (infixLine != null) {

			//case for "assign" type of expression e.g. "x = ..."
			if (expAssignsValueToVariable) {
				//extract right side of an expression
				List<ExPart> eval = infixLine.subList(2, infixLine.size());

				//in case we have a simple assignment
				if (eval.size() == 1) {
					//substitute variables
					substLine = substituteVariables(eval, variables);

					//get variable name
					String varName = infixLine.get(0).getValue();
					assignedVariableName = varName;

					//get value to be assigned
					result = Integer.parseInt(substLine.get(0).getValue());

					//assign
					variables.put(varName, result);
				} else {
					//substitute variables
					substLine = substituteVariables(eval, variables);

					//convert to Postfix form
					postfix = infixToPostfix(substLine);

					//evaluate
					result = calculatePostfix(postfix);

					//assign & set result
					String varName = infixLine.get(0).getValue();
					assignedVariableName = varName;
					variables.put(varName, result);
				}
			} else {
				//substitute variables
				substLine = substituteVariables(infixLine, variables);

				//convert to Postfix form
				postfix = infixToPostfix(substLine);

				//evaluate
				result = calculatePostfix(postfix);
			}

		} else throw new IllegalArgumentException("Expression does not contain proper infix line ");

	}


	/**
   * Evaluates arithmetic expression in postfix notation.
   * Simplified Dijkstra algorithm: supports only + and - operations
   *
   * @param postfix arithmetic expression in postfix notation
   * @return result of calculation
  **/
  private int calculatePostfix(List<ExPart> postfix) {
    if (postfix != null && postfix.size() > 0) {
      Deque<Integer> res = new LinkedList<Integer>();       //results stack
      res.addFirst(0);                                      //add dummy value to allow unary operations
      for (ExPart word : postfix) {
        if (word.getType() == Type.DIGIT) {
          res.addFirst(Integer.parseInt(word.getValue()));
        } else if (word.getType().isOperator() && res.size() >= 2) {
            int operand1 = (Integer) res.removeFirst();
            int operand2 = (Integer) res.removeFirst();
            int result = 0;
            if (word.getType() == Type.MINUS) {
              result = operand2 - operand1;
            } else if (word.getType() == Type.PLUS) {
              result = operand1 + operand2;
            } else throw new IllegalArgumentException("Unsupported operation " + word);
            res.addFirst(result);
        } else throw new IllegalArgumentException("Can't process an expression");
      }
      return (Integer) res.removeFirst();
    } else throw new IllegalArgumentException("Expression is null or zero length");
  }

	/**
   * Converts infix expression to postfix notation.
   *
   * @param words arithmetic expression in infix notation
   * @return expression in postfix notation
  **/
  private static List<ExPart> infixToPostfix(List<ExPart> words) throws IllegalArgumentException {
    if (words != null && words.size() > 0) {
      List<ExPart> result = new ArrayList<ExPart>();
      int i = 0;
      Deque<ExPart> stack = new LinkedList<ExPart>();
      boolean prevIsOperation = true;
      for (ExPart word : words){
          if (word.getType().isOperator()) {
              if (stack.size() > 0) {
                result.add(stack.removeFirst());
              }
              stack.addFirst(word);
              prevIsOperation = true;
            } else if (word.getType() == Type.DIGIT && prevIsOperation) {
              result.add(word);
              prevIsOperation = false;
            } else throw new IllegalArgumentException("Unsupported expression: " + word);
      }
      if (stack.size() > 0) {
        result.add(stack.removeFirst());
      }
      return result;
    } else throw new IllegalArgumentException("Right side of an expression should not be empty");
  }


	private List<ExPart> substituteVariables(List<ExPart> line, Map<String, Integer> variables) {
		List<ExPart> result = new ArrayList<ExPart>();
		for (ExPart ex : line) {
			if (ex.getType() == Type.VARIABLE) {
				if (variables.containsKey(ex.getValue())) {
					ExPart nw = new ExPart(variables.get(ex.getValue()));
					result.add(nw);
				} else throw new IllegalArgumentException("Undefined variable " + ex.getValue());
			} else {
				result.add(ex);
			}
		}
		return result;
	}


	private void addAtomized(ExPart ex, List<ExPart> list) {
		if (list == null) throw new IllegalArgumentException("Can't add word to a NULL list");
		if (ex == null) throw new IllegalArgumentException("Can't add a NULL expression");
		if (ex.getType() == Type.UNSUPPORTED) throw new IllegalArgumentException("Can't add a word of unsupported type: " + ex.getValue());

		if (list.size() > 0) {
			ExPart prev = list.get(list.size()-1);

			switch (ex.getType()) {
				case EQUALS:
					if (expAssignsValueToVariable) {
						throw new IllegalArgumentException("Expression can't contain more than one assignment");
					} else if (prev.getType() == Type.VARIABLE){
						expAssignsValueToVariable = true;
					} else {
						throw new IllegalArgumentException("Left side of an assignment operator should be a variable, got: \"" + prev.getValue() + ex.getValue() + "\"");
					}
					break;
				case VARIABLE:
				case DIGIT:
					if (!prev.getType().isOperator()) {
						throw new IllegalArgumentException("Illegal variable identifier or expression (left side of a variable or value should be an operator, got: \"" + prev.getValue() + ex.getValue() + "\"");
					}
					break;
			}

		}

		list.add(ex);
	}


	/**
	* Converts raw input into correct mathematical expression in infix form.
	*
	*
	* @throws IllegalArgumentException in case input can not be converted
	* to correct mathematical expression
	* @param raw input
	* @return expression in postfix notation
	**/
	private List<ExPart> atomize(String line) {
		List<ExPart> result = new ArrayList<ExPart>();

		//dirty checks
		if (line == null) throw new IllegalArgumentException("Null line");
		Matcher matcher = ALLOWED_CHARS.matcher(line.trim().replace(" ",""));
		if (!matcher.matches()) throw new IllegalArgumentException("Unsupported expression: " + line);

		String[] words = line.trim().split(" ");

		//outer loop through character groups separated by spaces
		for (String word : words) {
			try {
				word = word.replace(" ","");
				//if we have only one symbol we can simply output it, after type determination
				if (word.length() == 1) {
					Type curType = determineType(word.charAt(0));
					if (curType == Type.UNSUPPORTED) {
						throw new IllegalArgumentException("Unsupported expression: " + word);
					} else {
						addAtomized(new ExPart(word, curType), result);
					}
				//otherwise we proceed expression character by character
				} else if (word.length() > 1) {
					List<ExPart> processedWords = normalizeWord(word);
					for (ExPart ex : processedWords) {
						addAtomized(ex, result);
					}
				}
			} catch (IllegalArgumentException iae) {
				throw iae;
			}
		}
		return result;
	}

	private List<ExPart> normalizeWord(String word) {
		if (word == null) throw new IllegalArgumentException("Null word");

		char[] chars = word.toCharArray();
		List<ExPart> result = new ArrayList<ExPart>();

		boolean ongoing = false;
		StringBuilder current = new StringBuilder(word.length());
		Type curCharType = null;

		for (char c : chars) {
			if (!ongoing) {
				curCharType = determineType(c);
				if (curCharType == Type.UNSUPPORTED) {
					throw new IllegalArgumentException("Unsupported char: " + c);
				} else {
					current.append(c);
					ongoing = true;
				}
			} else {
						Type newCharType = determineType(c);
						if (newCharType == Type.UNSUPPORTED) {
							throw new IllegalArgumentException("Unsupported char: " + c);
						} else if (newCharType.isOfSameGroup(curCharType)) {
							current.append(c);
						} else {
						      try {
							         ExPart ex = new ExPart(current.toString(), curCharType);
							         result.add(ex);
							         current.setLength(0);
							         current.append(c);
							         curCharType = newCharType;
						       } catch (IllegalArgumentException iae) {
							        throw iae;
						       }
						}
					}
			}

		result.add(new ExPart(current.toString(), curCharType));
		return result;

	}

	/**
	* Converts operations to unified form.
	* E.g: --- = -, ++++ = +, -- = + etc.
	* @param operator operation in raw form
	* @return operation in unified form: +, - or =
	* @throws IllegalArgumentException if operation can not be converted to unified form
	**/
	private char processOperator(String operator) {
		if (operator == null) throw new IllegalArgumentException("Unsupported operator NULL");

		//equals
		Pattern pattern = Pattern.compile("^[=]*$");
		Matcher matcher = pattern.matcher(operator);
		if (matcher.matches()) {
			return '=';
		}

		//plus
		pattern = Pattern.compile("^[+]*$");
		matcher = pattern.matcher(operator);
		if (matcher.matches()) {
			return '+';
		}

		//plusminus
		pattern = Pattern.compile("^[-+]*$");
		matcher = pattern.matcher(operator);
		if (matcher.matches()) {
			operator = operator.replace("+","");
		}

		//minus
		pattern = Pattern.compile("^[-]*$");
		matcher = pattern.matcher(operator);
		if (matcher.matches()) {
			if (operator.length() % 2 == 0) {
				return '+';
			} else {
				return '-';
			}
		}

		throw new IllegalArgumentException("Unsupported operator: " + operator);

	}

	//determines the type of character in expression
	private Type determineType(char c) {
		if (Character.isDigit(c)) {
			return Type.DIGIT;
		} else if (Character.isLetter(c)) {
			return Type.VARIABLE;
		} else if (c == '=' || c == '+' || c == '-') {
			switch(c) {
				case '=':
				return Type.EQUALS;
				case '+':
				return Type.PLUS;
				case '-':
				return Type.MINUS;
			}
		}
		return Type.UNSUPPORTED;
	}

	//possible types of expression parts
	enum Type {
		VARIABLE, DIGIT, PLUS, MINUS, EQUALS, UNSUPPORTED;

		boolean isOfSameGroup(Type other) {
			if (other == null || !(other instanceof Type)) return false;
			if (this == UNSUPPORTED || other == UNSUPPORTED) return false;

			if ((this == VARIABLE && other == VARIABLE) ||
				(this == DIGIT && other == DIGIT) 		||
				(this.isPlusMinus() && other.isPlusMinus())) {
					return true;
			} else {
				return false;
			}
		}

		boolean isOperator() {
			return (this == PLUS || this == MINUS || this == EQUALS);
		}

		boolean isPlusMinus() {
			return (this == PLUS || this == MINUS);
		}
	}

	//represents a part of an expression
	class ExPart {

		ExPart(String expr, Type type) {
			if (type.isOperator() && expr.length() > 1) {
				try {
					char oper = processOperator(expr);
					type = determineType(oper);
					expr = Character.toString(oper);
				} catch (IllegalArgumentException iae) {
					throw iae;
				}
			}
			this.expr = expr;
			this.type = type;

		}

		ExPart(Integer expr) {
			this.expr = expr.toString();
			this.type = Type.DIGIT;

		}

		public Type getType() {
			return this.type;
		}

		public String getValue() {
			return this.expr;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public void setValue(String expr) {
			this.expr = expr;
		}

		private Type type;
		private String expr;
	}

	//stores "correct" expression in infix form
	private List<ExPart> infixLine;

	//stores expression with substituted vars
	private List<ExPart> substLine;

	//stores expression in postfix form
	private List<ExPart> postfix;

	//stores eval result
	private int result;

	private boolean expAssignsValueToVariable = false;
	private String assignedVariableName;

	//pattern which include all allowed chars in expression
	private static final Pattern ALLOWED_CHARS = Pattern.compile("^[a-zA-Z0-9+-=]*$");

}
