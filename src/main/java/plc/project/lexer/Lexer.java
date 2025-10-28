package plc.project.lexer;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through a combination of {@link #lex()}, which repeatedly
 * calls {@link #lexToken()} and skips over whitespace/comments, and
 * {@link #lexToken()}, which determines the type of the next token and
 * delegates to the corresponding lex method.
 *
 * <p>Additionally, {@link CharStream} manages the lexer state and contains
 * {@link CharStream#peek} and {@link CharStream#match}. These are helpful
 * utilities for working with character state and building tokens.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    //Whitespace: ("[ \b\n\r\t]")
//comments: chars.peek("/", "/")
    public List<Token> lex() throws LexException {
        var tokens = new ArrayList<Token>();
        while (chars.has(0)) {

            if (chars.peek("[ \b\n\r\t]")) {
                lexWhitespace();
            } else if (chars.peek("/", "/")) {
                lexComment();
            } else {
               tokens.add(lexToken());
            }
            //tokens.add(lexToken());
        }
        return tokens;
    }

    private void lexWhitespace() {
        //throw new UnsupportedOperationException("TODO");
while(chars.has(0) && chars.peek("[ \b\n\r\t]")) {  // chars.has(0) will stop it from going past stirng
    chars.match("[ \b\n\r\t]");
    //chars.index++; match should now do this automatically
}
chars.emit();
    }

    private void lexComment() throws LexException {
        //throw new UnsupportedOperationException("TODO");
        if (!chars.match("/", "/")) {
            throw new LexException("Invalid comment start", chars.index);
        }

        while (chars.has(0) && !chars.peek("\n")) {
            chars.match(".");
        }
        chars.emit();
    }

    private Token lexToken() throws LexException {

        if (chars.peek("[A-Za-z_]")) {  //sends it to correct function
            return lexIdentifier();
        } else if (chars.peek("[+-]", "[0-9]") || chars.peek("[0-9]")) {
            return lexNumber();    //number ::= [+-]? [0-9]+ ('.' [0-9]+)? ('e' [+-]? [0-9]+)?
        } else if (chars.peek("'")) {
            return lexCharacter();
        } else if (chars.peek("\"")) {
            return lexString();
        } else {
            return lexOperator();
        }
        //throw new UnsupportedOperationException("TODO: lexToken"); //TODO
    }

    private Token lexIdentifier() {
//identifier ::= [A-Za-z_] [A-Za-z0-9_-]*
      Preconditions.checkState(chars.match("[A-Za-z_]"));
      while (chars.match("[A-Za-z0-9_-]*")) {}

      return new Token(Token.Type.IDENTIFIER, chars.emit());

        //throw new UnsupportedOperationException("TODO: identiier"); //TODO
    }

    private Token lexNumber() throws LexException {  //note: Preconditions cant check for the exceptions so use if statements and replace them
        //number ::= [+-]? [0-9]+ ('.' [0-9]+)? ('e' [+-]? [0-9]+)?
//optional sign consume
        if (chars.match("[+-]")) {} //consumes

        //Preconditions.checkState(chars.match("[0-9]"));
        if (!chars.match("[0-9]")) {
            throw new LexException("missing decimal ", chars.index);
        }

        while (chars.match("[0-9]")) {}

        if (chars.match("\\.")) {//dec check
            //check for num after dec
            if (!chars.match("[0-9]")) {
                throw new LexException("missing decimal ", chars.index);
            }
            while (chars.match("[0-9]")) {}
            if (chars.match("[eE]")) { //can it be after dec?
                chars.match("[+-]");
                if (!chars.match("[0-9]")) {//if theres nothing after digits
                    throw new LexException("Missing exponent", chars.index);
                }
                while (chars.match("[0-9]")) {}
            }
            return new Token(Token.Type.DECIMAL, chars.emit());
        }

        if (chars.match("[eE]")) {
            chars.match("[+-]");
            // must have digits in exponent
            if (!chars.match("[0-9]")) {
                throw new LexException("Missing exponent", chars.index);
            }
            while (chars.match("[0-9]")) {
                //return new Token(Token.Type.INTEGER, chars.emit());
            }
            return new Token(Token.Type.INTEGER, chars.emit());
        }

        return new Token(Token.Type.INTEGER, chars.emit());

        //throw new UnsupportedOperationException("TODO: num"); //TODO
    }

    private Token lexCharacter() throws LexException {
        //     ['] ([^'\n\r\\] | escape) [']
//if else
        //preconditions causing problems
        if (!chars.match("'")) {
            throw new LexException("lexCharacter", chars.index);
        }
        if(!chars.has(0)) {
            throw new LexException("lexCharacter unterminated", chars.index);
        }

        if (chars.match("\\\\")) {
            //Preconditions.checkState(chars.match("\\\\"));
            //Preconditions.checkState(chars.match("[bnrt'\"\\\\]"));
            lexEscape();
        } else {
            if (!chars.match("[^'\n\r\\\\]")) {
                throw new LexException("lexCharacter invalid", chars.index);
            }
        }
        //Preconditions.checkState(chars.match("'"));
        if (!chars.match("'")) {
            throw new LexException("LexCharacter unterminated", chars.index);
        }

        return new Token(Token.Type.CHARACTER, chars.emit());
        //throw new UnsupportedOperationException("TODO: char"); //TODO
    }

    private Token lexString() throws LexException {
        // ([^"\n\r\\] | escape)* '"'
        // needs to have open and closed " and can be broken by escape
        if (!chars.match("\"")) {
            throw new LexException("LexString double quote", chars.index);
        }
        while (chars.has(0) && !chars.peek("\"")) {
            if (chars.match("\\\\")) {
                lexEscape();
            } else {
                if (!chars.match("[^\"\\\\\n\r]")) {
                    throw new LexException("lexString invalid", chars.index);
                }
            }
        }
        //Preconditions.checkState(chars.match("\""));//should only work with " closing
        if (!chars.match("\"")) {
            throw new LexException("lexString unterminated ", chars.index);
        }

        return new Token(Token.Type.STRING, chars.emit());
    }

    private void lexEscape() throws LexException {
        // '\' [bnrt'"\]
        //Preconditions.checkState(chars.match("\\\\")); already consumed

        if (!chars.match("[bnrt'\"\\\\]")) { //should still match it
            throw new LexException("escape exception", chars.index);
        }
    }

    public Token lexOperator() {
        //  [<>!=] '='? | [^A-Za-z_0-9'" \b\n\r\t]
        //use if else
        if (chars.match("[<>!=]")) {
            chars.match("=");
            return new Token(Token.Type.OPERATOR, chars.emit());
        } else {
            Preconditions.checkState(chars.match("[^A-Za-z_0-9'\" \b\n\r\t]"));
            return new Token(Token.Type.OPERATOR, chars.emit());
        }
        //throw new UnsupportedOperationException("TODO: operator"); //TODO
    }

    /**
     * A helper class for maintaining the state of the character stream (input)
     * and methods for building up token literals.
     */
    private static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        /**
         * Returns true if the next character(s) match their corresponding
         * pattern(s). Each pattern is a regex matching ONE character, e.g.:
         *  - peek("/") is valid and will match the next character
         *  - peek("/", "/") is valid and will match the next two characters
         *  - peek("/+") is conceptually invalid, but will match one character
         *  - peek("//") is strictly invalid as it can never match one character
         */
        public boolean peek(String... patterns) {
            if (!has(patterns.length - 1)) {
                return false;
            }
            for (int offset = 0; offset < patterns.length; offset++) {
                var character = input.charAt(index + offset);
                if (!String.valueOf(character).matches(patterns[offset])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the character stream.
         */
        public boolean match(String... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
                length += patterns.length;
            }
            return peek;
        }

        /**
         * Returns the literal built by all characters matched since the last
         * call to emit(); also resetting the length for subsequent tokens.
         */
        public String emit() {
            var literal = input.substring(index - length, index);
            length = 0;
            return literal;
        }

    }

}
