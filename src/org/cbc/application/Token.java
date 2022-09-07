package org.cbc.application;

import java.io.Serializable;

/**
 * The token class provides basic facilities for extracting information from a
 * string. A token has a string value and a token index that points to the
 * current character in the string. The index can point to beyond the end of the
 * string in which case its value will be the token's string size and step
 * operations will not increase index beyond this size.
 *
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.2, 04/Dec/01, C.B. Close:</b> Corrected comment.
 */
public class Token implements Serializable {
    private String value = "";
    private int    index = 0;
    /**
     * Removes quotation marks, i.e. the character ", from text. Single
     * quotation characters are removed and consecutive quotation characters are
     * replaced by a single quote.
     *
     * @param text text value from which quotations are to be stripped.
     * @return text with quotation characters removed.
     */
    public static String removeQuotes(String text) {
        String value = "";
        Token  t     = new Token(text.trim());

        while (t.moreCharacters()) {
            char ch = t.nextCharacter();

            if (ch == '"') {
                ch = t.nextCharacter();
            }
            if (ch != 0) {
                value = value + ch;
            }
        }
        return value;
    }

    /**
     * Creates a token for Value.
     *
     * @param Value Text value for token
     */
    public Token(String Value) {
        value = Value;
    }

    /**
     * Returns the next character from the token and increments the token index.
     * If the token is empty or the token index is past the last character the
     * character '\0' is returned.
     *
     * @return The next character as described in the method comment.
     */
    public char nextCharacter() {
        if (index >= value.length()) {
            return 0;
        }
        return value.charAt(index++);
    }

    /**
     * Returns true if the token index points to a character in the string.
     *
     * @return True if there is a current character.
     */
    public boolean moreCharacters() {
        return (index < value.length());
    }
    /**
     * Returns the token string, i.e. the value passed to the constructor.
     *
     * @return The token string.
     */
    public String value() {
        return value;
    }
    /**
     * Returns the token text after the current character or the empty string if
     * there are none.
     *
     * @return Returns the token string following the token index.
     */
    public String remainder() {
        return (index >= value.length()) ? "" : value.substring(index);
    }
    /**
     * Steps the token index back by one character. Has no effect if the token
     * index is already at the start of the token.
     */
    public void stepBack() {
        if (index > 0) {
            index -= 1;
        }
    }
    /**
     * Same as nextCharacter except the token index is not incremented.
     *
     * @return The current character or '\0' if there is none.
     */
    public char nextCharacterNoStep() {
        int Index = this.index;
        char ch = nextCharacter();
        this.index = Index;
        return ch;
    }
    /**
     * Returns the token index. If the index is beyond the end of the string the
     * value will be the string's size.
     *
     * @return The token index.
     */
    public int getIndex() {
        return index;
    }
    /**
     * Sets the token index to index. If index is &lt; 0, it the token index is
     * set to 0. If index is beyond the end of the string, index is set to the
     * token string size.
     *
     * @param index New token index value.
     */
    public void setIndex(int Index) {
        if (Index < 0) {
            this.index = 0;
        } else if (Index >= value.length()) {
            this.index = value.length();
        } else {
            this.index = Index;
        }
    }
    /**
     * Returns true if value matches key. Where key contains an * characters in
     * value are considered to match. The * matching terminates when a character
     * matching that following * is encountered. Consecutive * characters are
     * equivalent to a single *. All other characters in key and value (omitting
     * characters matching *) must match. E.g. for the value abcdef, the
     * following match
     * <PRE>    *a*, a*d*, ab*d*f, ab*
     * and the following don't match
     *    x*, a*e, ab, abcdEf</PRE>
     *
     * @param key String to compare against.
     * @param value value to be compared with.
     * @return True if value matches key according to the rules defined in the
     * method comment.
     */
    public static boolean isMatch(String key, String value) {
        Token   k     = new Token(key);
        Token   v     = new Token(value);
        boolean step  = true;
        boolean equal = true;
        char    kCh   = 0;
        char    vCh;

        while (equal && v.moreCharacters() && (k.moreCharacters() || !step)) {
            if (step) {
                step = kCh != '*';
                kCh = k.nextCharacter();
            }

            if (kCh == '*') {
                step = true;
            } else {
                vCh = v.nextCharacter();
                if (kCh == vCh) {
                    step = true;
                } else if (step) {
                    equal = false;
                }
            }
        }

        //If not equal then there is definately not a match so return false.

        if (!equal) {
            return false;
        }

        //If equal then the value and key match on the parts compared, there may be
        //remaining bits of the key value not check. So check for conditions that mean
        //there is positively a match and return true. In general there is not a match
        //if there are more chacters in the key and value apart from the following
        //exceptions.

        //Final character of key is * so even if more in value it is a match.

        if (kCh == '*') {
            return true;
        }

        //If the remainder of the key is * it is a match.

        if (k.moreCharacters() && k.remainder().equals("*")) {
            return true;
        }

        //If no more in key and value and step is in progress there is a match.
        //Note: step is false when the penultimate key character is *, e.g.
        //        k = AB*x v = ABC
        //and in this case there is not a match even though the remainder of the key
        //and value is empty.

        if (!v.moreCharacters() && step && !k.moreCharacters()) {
            return true;
        }
        return false;
    }
}
