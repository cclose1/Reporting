package org.cbc.application.reporting;

import org.cbc.application.Token;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.1, 18/Nov/01, C.B. Close:</b> Added Module and Group lists.
 */
public class TraceMask implements Serializable {
    /*
     * If the first character is # the following characters up to the first
     * none digit are the level. The level is returned as an integer and if
     * the terminating character is #, it is discarded
     *
     * -1 is returned if no level is present.
     */

    static int getLevel(Token mask) {
        int ch    = mask.nextCharacter();
        int level = -1;

        if (ch == '#') {
            level = 0;

            while ((ch = mask.nextCharacter()) >= '0' && ch <= '9') {
                if (level < 1000) {
                    level = 10 * level + ch - '0';
                } else {
                    //Error("Trace level too large - " + Mask.value());
                    break;
                }
            }
        }
        if (ch != '#') {
            mask.stepBack();
        }
        return level;
    }

    public TraceMask() {
        for (int i = 0; i < 256; i++) {
            flags[i] = false;
        }
    }
    public void setMask(Token mask) {
        String  lFlags   = "";
        boolean relative = true;
        boolean on       = true;

        switch (mask.nextCharacter()) {
            case '+':
                break;
            case '-':
                on = false;
                break;
            default:
                relative = false;
                mask.stepBack();

                for (int i = 0; i < this.flags.length; i++) {
                    this.flags[i] = false;
                }
        }

        //If mask is only + or - set all flags to the appropriate value.

        if (!mask.moreCharacters() && relative) {
            for (int i = 0; i < 255; i++) {
                this.flags[i] = on;
            }
            return;
        }

        if (mask.nextCharacter() == '!') {
            char ch;
            int  flag = -1;

            while ((ch = mask.nextCharacter()) != '!' && ch != '\0') {
                switch (ch) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':

                        if (flag == -1) {
                            flag = 0;
                        }

                        flag = 10 * flag + ch - '0';

                        if (flag > 255) {
                            //Error("Invalid character code - " + Token.value());
                            break;
                        }
                    case ' ':
                        break;
                    case ',':
                        if (flag != -1) {
                            lFlags += (char) flag;
                        }
                        flag = -1;
                    default:
                    //Error("Numeric mask not numeric
                }
            }
            if (flag != -1) {
                lFlags += (char) flag;
            }
        } else {
            mask.stepBack();
        }

        lFlags += mask.remainder();

        for (int i = 0; i < lFlags.length(); i++) {
            this.flags[lFlags.charAt(i)] = on;
        }
    }

    public boolean isEnabled(char flag) {
        return flags[flag];
    }
    /**
     * 
     * @return True if any of the trace flags are set.
     */
    public boolean isEnabled() {
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) return true;
        }
        return false;
    }

    public void updateModules(Token list) {
        updateSet(modules, list);
    }

    public void updateGroups(Token list) {
        updateSet(groups, list);
    }

    public boolean isGroupEnabled(String name) {
        return !groups.contains(name.toUpperCase());
    }
    /*
     * Needs to be changed to do a wild card match on Name
     */
    public boolean isModuleEnabled(String name) {
        synchronized (modules) {
            for (String module : modules) {
                if (Token.isMatch(module, name)) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * List contains comma separated identifier preceded by an optional + or - character. The
     * identifiers are added removed from Set if the first character is -, added to Set if it
     * is + and if neither + or - is present Set is replaced by the identifiers.
     */
    private void updateSet(Set<String> set, Token list) {
        boolean add  = true;
        String  name = "";

        if (!list.moreCharacters()) {
            set.clear();
        } else {
            char ch = list.nextCharacter();
            if (ch == '-') {
                add = false;
            } else if (ch != '+') {
                set.clear();
                list.stepBack();
            }
            while (list.moreCharacters()) {
                ch = list.nextCharacter();

                if (ch != ',') {
                    name += ch;
                }
                if (ch == ',' || !list.moreCharacters()) {
                    name = name.trim().toUpperCase();

                    if (name.length() != 0) {
                        if (add) {
                            set.add(name);
                        } else {
                            set.remove(name);
                        }
                    }
                    name = "";
                }
            }
        }
    }
    private boolean     flags[] = new boolean[256];
    private Set<String> groups  = Collections.synchronizedSet(new HashSet<String>());
    private Set<String> modules = Collections.synchronizedSet(new HashSet<String>());
}
