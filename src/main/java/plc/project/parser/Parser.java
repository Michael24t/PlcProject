package plc.project.parser;

import com.google.common.base.Preconditions;
import plc.project.lexer.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This style of parser is called <em>recursive descent</em>. Each rule in our
 * grammar has dedicated function, and references to other rules correspond to
 * calling that function. Recursive rules are therefore supported by actual
 * recursive calls, while operator precedence is encoded via the grammar.
 *
 * <p>The parser has a similar architecture to the lexer, just with
 * {@link Token}s instead of characters. As before, {@link TokenStream#peek} and
 * {@link TokenStream#match} help with traversing the token stream. Instead of
 * emitting tokens, you will instead need to extract the literal value via
 * {@link TokenStream#get} to be added to the relevant AST.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public Ast parse(String rule) throws ParseException {
        var ast = switch (rule) {
            case "source" -> parseSource();
            case "stmt" -> parseStmt();
            case "expr" -> parseExpr();
            default -> throw new AssertionError(rule);
        };
        if (tokens.has(0)) {
            throw new ParseException("Expected end of input.", tokens.getNext());
        }
        return ast;
    }

    private Ast.Source parseSource() throws ParseException {
        var statements = new ArrayList<Ast.Stmt>();
        while (tokens.has(0)) {
            statements.add(parseStmt());
        }
        return new Ast.Source(statements);
    }

    private Ast.Stmt parseStmt() throws ParseException {
        //stmt::= let_stmt | def_stmt | if_stmt | for_stmt | return_stmt | expression_or_assignment_stmt
        if (tokens.peek("LET")){
            return parseLetStmt();
        }
        else if (tokens.peek("DEF")){
            return parseDefStmt();
        }
        else if (tokens.peek("IF")) {
            return parseIfStmt();
        } else if (tokens.peek("FOR")) {
            return parseForStmt();
        } else if (tokens.peek("RETURN")) {
            return parseReturnStmt();
        } else {
            return parseExpressionOrAssignmentStmt();
        }
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt parseLetStmt() throws ParseException {
        //let_stmt ::= 'LET' identifier ('=' expr)? ';'
        //needs semicolon at the end

        Preconditions.checkState(tokens.match("LET"));
        if (!tokens.peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Should have something after LET", tokens.getNext());
        }
        var name = tokens.get(0).literal();

        tokens.match(Token.Type.IDENTIFIER);

        Optional<Ast.Expr> val = Optional.empty();
        if (tokens.match("=")) {
            val = Optional.of(parseExpr());
        }
        if (!tokens.match(";")) {
            throw new ParseException("need ';'", tokens.getNext());
        }
        return new Ast.Stmt.Let(name, val);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt parseDefStmt() throws ParseException {
//def_stmt ::= 'DEF' identifier '(' (identifier (',' identifier)*)? ')' 'DO' stmt* 'END'
        Preconditions.checkState(tokens.match("DEF"));

        if (!tokens.peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Missing name", tokens.getNext());
        }
        var name = tokens.get(0).literal(); //identifier
        tokens.match(Token.Type.IDENTIFIER);

        if (!tokens.match("(")) {
            throw new ParseException("Need'(' after function", tokens.getNext());
        }

        var param = new ArrayList<String>(); // ["x","x"...]
        if (tokens.peek(Token.Type.IDENTIFIER)) { //checking parameters
            param.add(tokens.get(0).literal());

            tokens.match(Token.Type.IDENTIFIER);
            while (tokens.match(",")) {
                if (!tokens.peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Need identifier after comma", tokens.getNext());
                }
                param.add(tokens.get(0).literal());
                tokens.match(Token.Type.IDENTIFIER);
            }
        }
        if (!tokens.match(")")) {
            throw new ParseException("Need ')' after parameter list", tokens.getNext());
        }
        if (!tokens.match("DO")) { //body
            throw new ParseException("need DO after header", tokens.getNext());
        }
        var name2 = new ArrayList<Ast.Stmt>();
        while (!tokens.peek("END")) {
            if (!tokens.has(0)) {
                throw new ParseException("no END", tokens.getNext());
            }
            name2.add(parseStmt());
        }
        if (!tokens.match("END")) {
            throw new ParseException("Need end after everything", tokens.getNext());
        }
        return new Ast.Stmt.Def(name, param, name2);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt parseIfStmt() throws ParseException {
        //if_stmt ::= 'IF' expr 'DO' stmt* ('ELSE' stmt*)? 'END'

        Preconditions.checkState(tokens.match("IF"));
        var cond = parseExpr(); //parses conditions

        if (!tokens.match("DO")) {
            throw new ParseException("Need DO after if", tokens.getNext());
        }

        //then statement
        var then = new ArrayList<Ast.Stmt>();
        while (!tokens.peek("END") && !tokens.peek("ELSE")) {
            if (!tokens.has(0)) {
                throw new ParseException("end missing", tokens.getNext());
            }
            then.add(parseStmt());
        }
        //else statement
        var elses = new ArrayList<Ast.Stmt>();
        if (tokens.match("ELSE")) {
            while (!tokens.peek("END")) {
                if (!tokens.has(0)) {
                    throw new ParseException("end missing", tokens.getNext());
                }
                elses.add(parseStmt());
            }
        }

        if (!tokens.match("END")) {
            throw new ParseException("Need end", tokens.getNext());
        }

        return new Ast.Stmt.If(cond, then, elses);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }


    private Ast.Stmt parseForStmt() throws ParseException {
        //for_stmt ::= 'FOR' identifier 'IN' expr 'DO' stmt* 'END'
        Preconditions.checkState(tokens.match("FOR"));

        if (!tokens.peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after FOR", tokens.getNext());
        }

        var nameIdentifier = tokens.get(0).literal();
        tokens.match(Token.Type.IDENTIFIER);

        if (!tokens.match("IN")) {
            throw new ParseException("Need IN", tokens.getNext());
        }

        var it = parseExpr();
        if (!tokens.match("DO")) {
            throw new ParseException("expecting Do", tokens.getNext());
        }

        var name2 = new ArrayList<Ast.Stmt>();
        while (!tokens.peek("END")) {
            if (!tokens.has(0)) {
                throw new ParseException("missing end", tokens.getNext());
            }
            name2.add(parseStmt());
        }

        if (!tokens.match("END")) {
            throw new ParseException("missing END", tokens.getNext());
        }
        return new Ast.Stmt.For(nameIdentifier, it, name2);

        //throw new UnsupportedOperationException("TODO"); //TODO
    }


    private Ast.Stmt parseReturnStmt() throws ParseException {
        //return_stmt ::= 'RETURN' expr? ('IF' expr)? ';'

        Preconditions.checkState(tokens.match("RETURN"));
        if (tokens.peek("IF")) {
            tokens.match("IF");
            var cond = parseExpr();
            if (!tokens.match(";")) {
                throw new ParseException("ExpectS ; after RETURN IF", tokens.getNext());
            }
            return new Ast.Stmt.If(cond, List.of(new Ast.Stmt.Return(Optional.empty())), List.of());
        }

        Optional<Ast.Expr> val = Optional.empty();
        Optional<Ast.Expr> temp = Optional.empty();

        if (!tokens.peek(";")) {
            val = Optional.of(parseExpr());
            if (tokens.match("IF")) {
                temp = Optional.of(parseExpr());
            }
        }

        if (!tokens.match(";")) {
            throw new ParseException("Need ';' after RETURN", tokens.getNext());
        }

        return new Ast.Stmt.Return(val);
    }

    private Ast.Stmt parseExpressionOrAssignmentStmt() throws ParseException {
//expression_or_assignment_stmt ::= expr ('=' expr)? ';'

        var before = parseExpr();
        if(tokens.match("=")){
            var after = parseExpr(); //before = after
            if(!tokens.match(";")) {
                throw new ParseException("Missing ';' after assignment", tokens.getNext());
            }
            if(!(before instanceof Ast.Expr.Variable || before instanceof Ast.Expr.Property)){ //check this
                throw new ParseException("assignments in wrong order", tokens.getNext());
            }

            return new Ast.Stmt.Assignment(before,after);
        }
        else{
            if (!tokens.match(";")) {
                throw new ParseException("Needs ;", tokens.getNext());
            }
        }
        return new Ast.Stmt.Expression(before);
    }

    private Ast.Expr parseExpr() throws ParseException {
        return parseLogicalExpr();
    }

    private Ast.Expr parseLogicalExpr() throws ParseException {
      //logical_expr ::= comparison_expr (('AND' | 'OR') comparison_expr)*
        var expr = parseComparisonExpr();
        while (tokens.peek(Token.Type.IDENTIFIER, "AND") || tokens.peek(Token.Type.IDENTIFIER, "OR")) {
            String operator = tokens.get(0).literal();
            tokens.match(Token.Type.IDENTIFIER, operator); //consumes
            var right = parseComparisonExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
        //throw new UnsupportedOperationException("TODO: logical"); //TODO
    }

    private Ast.Expr parseComparisonExpr() throws ParseException {
        //comparison_expr ::= additive_expr (('<' | '<=' | '>' | '>=' | '==' | '!=') additive_expr)*

        var expr = parseAdditiveExpr();
        while (tokens.peek("<") || tokens.peek("<=")
                || tokens.peek(">") || tokens.peek(">=")
                || tokens.peek("==") || tokens.peek("!=")) {
            var operator = tokens.get(0).literal();
            tokens.match(operator);
            var right = parseAdditiveExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseAdditiveExpr() throws ParseException {
        //LECTURE CODE
        //additive ::= mult_expr (('+' | '-') mult_expr)*
        var expr = parseMultiplicativeExpr();
        while (tokens.match("+") || tokens.match("-")) {
            var operator = tokens.get(-1).literal();
            var right = parseMultiplicativeExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseMultiplicativeExpr() throws ParseException {
//LECTURE CODE
        //multiplicative_expr ::= secondary_expr (('*' | '/') secondary_expr)*
        //add 1 + 2
        //var expr = parsePrimaryExpr();
        var expr = parseSecondaryExpr();
        while (tokens.match("*") || tokens.match("/")) {
            var operator = tokens.get(-1).literal();
            var right = parseSecondaryExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseSecondaryExpr() throws ParseException {
        //secondary_expr ::= primary_expr property_or_method*
        var expr = parsePrimaryExpr();
        while (tokens.peek(".")) {
            expr = parsePropertyOrMethod(expr);
        }
        return expr;
    }

    private Ast.Expr parsePropertyOrMethod(Ast.Expr receiver) throws ParseException {
        //property_or_method ::= '.' identifier ('(' (expr (',' expr)*)? ')')?
//property_or_method ::= '.' identifier ('(' (expr (',' expr)*)? ')')?
        if (!tokens.match(".")) {
            throw new ParseException("has to begin with .", tokens.getNext());
        }

        if (!tokens.peek(Token.Type.IDENTIFIER)) { // name
            throw new ParseException("Needs identifier", tokens.getNext());
        }

        var name = tokens.get(0).literal();
        tokens.match(Token.Type.IDENTIFIER); //advance after finding identifier


        if (tokens.peek("(")) {  //if method
            tokens.match("(");
            var innerP = new ArrayList<Ast.Expr>();
            if (!tokens.peek(")")) {
                innerP.add(parseExpr());
                while (tokens.match(",")) { //handles comma
                    if (tokens.peek(")")) break; //trailing
                    innerP.add(parseExpr());
                }
            }

            if (!tokens.match(")")) {
                throw new ParseException("No closing parenthasese ", tokens.getNext());
            }
            return new Ast.Expr.Method(receiver, name, innerP);
        }
//if Property might need to specify more here with if statemetn
        else {
            return new Ast.Expr.Property(receiver, name);
        }
    }

    private Ast.Expr parsePrimaryExpr() throws ParseException {
        //primary_expr ::= literal_expr | group_expr | object_expr | variable_or_function_expr
        //LECTURE CODE
        //NEED TO ADD ALL LITERALS
        if (tokens.peek(Token.Type.INTEGER)) {  //consume or peek?
            return parseLiteralExpr();
        }
        else if (tokens.peek(Token.Type.DECIMAL)) {
            return parseLiteralExpr();
        }
        else if (tokens.peek(Token.Type.CHARACTER)) {
            return parseLiteralExpr();
        }
        else if (tokens.peek(Token.Type.STRING)) {
            return parseLiteralExpr();
        }
        else if (tokens.peek("TRUE") || tokens.peek("FALSE")) {
            return parseLiteralExpr();
        }
        else if (tokens.peek("NIL")) {
            return parseLiteralExpr();
        }


        else if (tokens.peek("(")){ //group
            return parseGroupExpr();
        }
        else if (tokens.peek("OBJECT")) { //object
            return parseObjectExpr();
        }
        else if (tokens.peek(Token.Type.IDENTIFIER)) { //variablefunction
            return parseVariableOrFunctionExpr();
        }

        throw new ParseException("TODO: primary", tokens.getNext());
        //throw new UnsupportedOperationException("TODO primary"); //TODO
    }
//missing character here
    private Ast.Expr parseLiteralExpr() throws ParseException {
        //literal_expr ::= 'NIL' | 'TRUE' | 'FALSE' | integer | decimal | character | string

        if (tokens.match("NIL")){
            return new Ast.Expr.Literal(null);
        }
        else if (tokens.match("TRUE") || tokens.match("FALSE")) {
            return new Ast.Expr.Literal(Boolean.valueOf(tokens.get(-1).literal()));
        }

        if (tokens.match(Token.Type.INTEGER)) {  //cpnsumes 1 int token
            var literal = tokens.get(-1).literal(); //use -1 since match consumed the token
            return new Ast.Expr.Literal(new BigInteger(literal));
        }
        else if (tokens.match(Token.Type.DECIMAL)) {
            var literal = tokens.get(-1).literal();
            return new Ast.Expr.Literal(new BigDecimal(literal));
        }
        else if (tokens.match(Token.Type.CHARACTER)) {
            //String literal = tokens.get(-1).literal();
            //System.out.println("character test");
            String temp = tokens.get(-1).literal(); //getting rid of quotes
            String character = temp.substring(1, temp.length() - 1);

            if (character.equals("\\n")) { //newline check
                character = "\n";
            }

            return new Ast.Expr.Literal(character.charAt(0));
        }
        else if (tokens.match(Token.Type.STRING)) { //TOD: NEWLINE solved i think
            String temp = tokens.get(-1).literal();
            String strings = temp.substring(1, temp.length() - 1);

            strings = strings.replace("\\n","\n");

            return new Ast.Expr.Literal(strings);
        }
        throw new UnsupportedOperationException("Literal problem"); //TODO
    }

    private Ast.Expr parseGroupExpr() throws ParseException {
        //group ::+ '(' expr ')'
        Preconditions.checkState(tokens.match("("));
        var expr = parseExpr();
        if(!tokens.match(")")) {
            throw new ParseException("Expected ')'", tokens.getNext());//should be correct as long as abstraction is right
            //var operator = tokens.get(-1).literal();
        }
return new Ast.Expr.Group(expr);
    }

    private Ast.Expr parseObjectExpr() throws ParseException {
        //object_expr ::= 'OBJECT' identifier? 'DO' let_stmt* def_stmt* 'END'
        Preconditions.checkState(tokens.match("OBJECT"));
        Optional<String> objName = Optional.empty();

        if (tokens.peek(Token.Type.IDENTIFIER) && !tokens.get(0).literal().equals("DO")) { //checking name
            objName = Optional.of(tokens.get(0).literal());
            tokens.match(Token.Type.IDENTIFIER); //continue
        }

        if (!tokens.match("DO")) {
            throw new ParseException("Need do in literal", tokens.getNext());
        }

        var let = new ArrayList<Ast.Stmt.Let>();
        var def = new ArrayList<Ast.Stmt.Def>();

        while (!tokens.peek("END")) { //check edge cases here
            if (tokens.peek("LET")) {
                //Ast.Stmt parsed = parseLetStmt();
                let.add((Ast.Stmt.Let) parseLetStmt());

            } else if (tokens.peek("DEF")) {
                def.add((Ast.Stmt.Def) parseDefStmt());
            } else {
                throw new ParseException("Something other than LET or DEF", tokens.getNext());
            }
        }

        if (!tokens.match("END")) {
            throw new ParseException("End shouldnt be here ", tokens.getNext());
        }

        return new Ast.Expr.ObjectExpr(objName, let, def);
    }

    private Ast.Expr parseVariableOrFunctionExpr() throws ParseException {
        //variable_or_function_expr ::= identifier ('(' (expr (',' expr)*)? ')')?
        //Use because this is an internal method : LECTURE CODE TODO
        Preconditions.checkState(tokens.match(Token.Type.IDENTIFIER));
        var name = tokens.get(-1).literal();
        //FUNCTION SIDE
        if (tokens.match("(")){
            //throw new UnsupportedOperationException("TODO");
            var arguments = new ArrayList<Ast.Expr>();
            if (!tokens.peek(")")) {
                arguments.add(parseExpr()); //check
                while (tokens.match(",")){ //string
                    if (tokens.peek(")")) break; //trailing commas
                    arguments.add(parseExpr());
                }
            }
if (!tokens.match(")")) {
    throw new ParseException("Expected ')'", tokens.getNext());
}
return new Ast.Expr.Function(name,arguments);
        }
        //VARIABLE SIDE
        else{
            return new Ast.Expr.Variable(name);
        }
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at (index + offset).
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Returns the token at (index + offset).
         */
        public Token get(int offset) {
            Preconditions.checkState(has(offset));
            return tokens.get(index + offset);
        }

        /**
         * Returns the next token, if present.
         */
        public Optional<Token> getNext() {
            return index < tokens.size() ? Optional.of(tokens.get(index)) : Optional.empty();
        }

        /**
         * Returns true if the next characters match their corresponding
         * pattern. Each pattern is either a {@link Token.Type}, matching tokens
         * of that type, or a {@link String}, matching tokens with that literal.
         * In effect, {@code new Token(Token.Type.IDENTIFIER, "literal")} is
         * matched by both {@code peek(Token.Type.IDENTIFIER)} and
         * {@code peek("literal")}.
         */
        public boolean peek(Object... patterns) {
            if (!has(patterns.length - 1)) {
                return false;
            }
            for (int offset = 0; offset < patterns.length; offset++) {
                var token = tokens.get(index + offset);
                var pattern = patterns[offset];
                Preconditions.checkState(pattern instanceof Token.Type || pattern instanceof String, pattern);
                if (!token.type().equals(pattern) && !token.literal().equals(pattern)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the token stream.
         */
        public boolean match(Object... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
            }
            return peek;
        }

    }

}
