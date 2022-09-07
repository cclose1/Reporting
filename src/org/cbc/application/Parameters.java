package org.cbc.application;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * The class maintains a set of parameters and provides a method to substitute
 * parameter values within a string.
 *
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.1, 18/Nov/01, C.B. Close:</b> Added getNames.
 */
public class Parameters implements Serializable {

    /**
     * Setting ignoreNameCase to true causes parameter name case to be ignored.
     * i.e upper and lower case letters in parameter names are equivalent.
     */
    public Parameters(boolean ignoreNameCase) {
        this.ignoreNameCase = ignoreNameCase;
    }

    /**
     * Creates a parameters instance with IgnoreNameCase set to true.
     */
    public Parameters() {
        this(true);
    }

    /**
     * Sets parameter Name to Value. The current value is overwritten.
     */
    public void setValue(String Name, String Value) {
        params.put(key(Name), Value);
    }

    public void setValue(String Name, int Value) {
        params.put(key(Name), Integer.toString(Value));
    }

    /**
     * Returns the value for parameter Name. The empty string is returned if
     * parameter Name does not exist.
     */
    public String getValue(String Name) {
        String Value = (String) params.get(key(Name));
        return (Value == null) ? "" : Value;
    }

    /**
     * Returns true if parameter Name exists.
     */
    public boolean exists(String Name) {
        return params.containsKey(key(Name));
    }

    /**
     * Clears all the parameters.
     */
    public void clear() {
        params.clear();
    }
    /*
     * Removes parameter Name. No action is taken if the parameter does not exist.
     */
    public void clear(String Name) {
        params.remove(key(Name));
    }
    public Set<String> getNames() {
        return params.keySet();
    }
    /**
     * Substitutes the parameters embedded in template with their values.
     * template can contain two type of parameters: time and named.
     *
     * The characters |, %, ! and + are control characters in the template.
     * These characters can be treated as characters by preceding them with the
     * escape character |, i.e the sequence |x where x is any printable
     * character will be taken as x and not a potential control character.
     *
     * Time parameters consist of % followed by a single format character. The
     * format characters are those implemented by the strftime C function
     * defined by time.h. There is an additional format character T which gives
     * the fractional part of the second to 3 decimal places. Local time is used
     * as the time source for the substitution.
     *
     * Name parameters are delimited by the ! character.
     *
     * Spaces surrounding the parameter name are ignored. Format specifiers can
     * be included between the parameter delimiters and the name. The format
     * specifiers are:-
     *
     * :...+ This identifies the default to supplied if the parameter does not
     * have a value. The default value is the characters between the : and +
     * This specifier must follow the initial ! ignoring spaces. +s..+ Inserts
     * the characters between the s and + characters if the parameter has a
     * value. +cx Inserts the character x if the parameter has a value.
     *
     * The order of parameter substitution is named parameters followed by time
     * parameters. Named parameters within substituted values are not expanded.
     * Time parameters within substituted values are expanded.
     */
    public String substitute(String template, Parameters local) {
        Token token   = new Token(template);
        String output = "";

        while (token.moreCharacters()) {
            boolean add = true;
            char chr = token.nextCharacter();

            if (chr != '|') {
                if (chr == '!') {
                    output += expandParam(token, local);
                    add = false;
                }
            } else {
                chr = token.nextCharacter();
            }

            if (add && chr != '\0') {
                output += chr;
            }
        }
        return Utilities.formatTime(output);
    }

    public String substitute(String template) {
        return substitute(template, null);
    }
    private void formatError(String reason, Token format) {
        //***Need to do something about this
        //throw ParameterException(
        //Reason + " at character " + (Format.getIndex() + 1) + " in template " + Format.value());
    }

    private String executeFormat(Token t, boolean string) {
        String value = "";
        char control = (string) ? 's' : t.nextCharacter();
        char ch = 0;

        if (control != 's' && control != 'c') {
            formatError("Invalid format control", t);
        }

        while (t.moreCharacters() && (ch = t.nextCharacter()) != '!') {
            boolean plus = (ch == '+');

            if (ch == '|') {
                if (!t.moreCharacters()) {
                    formatError("Escaped character missing", t);
                }
                ch = t.nextCharacter();
            }

            if (control == 'c') {
                value += ch;
                return value;
            }
            if (plus) {
                return value;
            }
            value += ch;
        }
        formatError("Unterminated format", t);
        return "";
    }

    private String expandParam(Token t, Parameters local) {
        String  name         = "";
        String  prefix       = "";
        String  postfix      = "";
        String  def          = "";
        String  value        = "";
        boolean nameComplete = false;
        boolean first        = true;
        char    ch           = 0;

        while (t.moreCharacters() && (ch = t.nextCharacter()) != '!') {
            if (ch == '|') {
                ch = t.nextCharacter();
            }
            switch (ch) {
                case '\0':
                    formatError("Escaped character missing", t);
                    break;
                case ':':
                    if (!first) {
                        formatError("Default not at start", t);
                    }
                    def = executeFormat(t, true);
                    break;
                case '+':
                    nameComplete = (name.length() != 0);
                    if (nameComplete) {
                        postfix += executeFormat(t, false);
                    } else {
                        prefix += executeFormat(t, false);
                    }
                    break;
                case ' ':
                    if (name.length() != 0) {
                        name += ch;
                    }
                    break;
                default:
                    if (nameComplete) {
                        formatError("Unexpected character", t);
                    }
                    name += ch;
            }
            if (first && ch != ' ') {
                first = false;
            }
        }
        if (ch != '!') {
            formatError("Parameter specifier not terminated by !", t);
        }
        if (local != null && local.exists(name)) {
            value = local.getValue(name);
        } else {
            value = getValue(name);
        }
        return (value.length() == 0) ? def : prefix + value + postfix;
    }
    
    private String expandParam(Token t) {
        return expandParam(t, null);
    }
    private String key(String name) {
        return (ignoreNameCase) ? name.toUpperCase() : name;
    }
    private transient HashMap<String, String> params         = new HashMap<String, String>();
    private transient boolean                 ignoreNameCase = true;
}