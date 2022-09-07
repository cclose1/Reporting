package org.cbc.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Provides access to application configuration information read from a
 * configuration file. A configuration file consists of one or more sections.
 * Each section starts with *Identifier where Identifier is a string. There can
 * be more than one section with the same identifier.
 *
 * Each section has a number of properties, which can be zero. Each property
 * occupies a single line and is of the form
 *
 * Id[=[Value]]
 *
 * where Id is the property name and Value its value. Brackets surround optional
 * fields. Property names need not be unique within a section.
 *
 * Blank lines and comments are ignored. A comment starts with # character and
 * are terminated by the end of line.
 *
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.2, 31/Jul/01, C.B. Close:</b> Ignore case on section names.
 * @version <b>v1.2, 18/Nov/01, C.B. Close:</b> Trim Name and Value.
 */
public class Configuration implements Serializable {
    
    /*
     * Exception thrown by Configuration methods.
     */
    public class ConfigurationError extends Exception {

        public ConfigurationError(String Text, boolean IncludeLineNo) {
            this.Text = setPrefix(Text, IncludeLineNo);
        }
        public String toString() {
            return Text;
        }
        private String Text;
    }
    /*
     * Throws ConfigurationError exception allowing validation errors to be
     * associated with the configuration file.
     *
     * IncludeLineNo should only be set to true if the error is associated with
     * the last read section or property.
     */
    public void setError(String text, boolean includeLineNo) throws ConfigurationError {
        throw new ConfigurationError(text, includeLineNo);
    }

    private class ConfigurationItem {
        boolean isSection;

        String  id = "";
        String  value;
        int     intVal;
        int     lineNo;
        
        public ConfigurationItem(int lineNo, String line) throws ConfigurationError {
            try {
                this.lineNo = lineNo;
                line = line.trim();

                if (line.startsWith("*")) {
                    isSection = true;

                    if (line.length() > 0) {
                        id = line.substring(1).toUpperCase().trim();
                    }
                } else {
                    String values[] = line.split("=", 2);
                    isSection = false;

                    switch (values.length) {
                        case 0:
                            id = "";
                            value = "";
                            break;
                        case 1:
                            id = values[0].toUpperCase().trim();
                            value = "";
                            break;
                        default:
                            id = values[0].toUpperCase().trim();
                            value = Token.removeQuotes(values[1]).trim();
                    }
                }
            } catch (Exception e) {
                setError(e.getMessage(), true);
            }
        }
        public boolean isInteger() {
            try {
                intVal = Integer.parseInt(current.value);
                
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        public int getInteger() throws ConfigurationError {
            if (!isInteger()) {
                setError("Property value is not an integer", true);
            }
            return intVal;
        }

        public boolean getIsTrue() {
            return (!isSection && (
                    value.equalsIgnoreCase("true") || 
                    value.equalsIgnoreCase("yes")  || 
                    value.equalsIgnoreCase("t")    || 
                    value.equalsIgnoreCase("y")));
        }
        public boolean getIsFalse() {
            return (!isSection && (
                    value.equalsIgnoreCase("false") || 
                    value.equalsIgnoreCase("no")  || 
                    value.equalsIgnoreCase("f")    || 
                    value.equalsIgnoreCase("n")));
        }
        public boolean isBoolean() {
            return getIsTrue() || getIsFalse();
        }
        public boolean getBoolean() throws ConfigurationError {
            if (getIsTrue())  return true;
            if (getIsFalse()) return false;
            
            setError("Property value is not a boolean", true);
            
            return false;
        }
    }
    private void loadFile(File file) throws FileNotFoundException, ConfigurationError {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;
        
        loadLine = 0;
        
        try {
            while ((str = br.readLine()) != null) {
                loadLine++;
                
                if (!((str.startsWith("#") || str.equals("")))) {
                    items.add(new ConfigurationItem(loadLine, str));
                }
            }
            br.close();
        } catch (IOException e) {
            setError(e.toString(), true);
        }
    }  
    /*
     * Reads the property file.
     */
    public Configuration(File file) throws FileNotFoundException, ConfigurationError {
        this.file = file;
  
        loadFile(file);
    }

    public Configuration(String fileName) throws FileNotFoundException, ConfigurationError {
        this(new File(fileName));
    }

    public Configuration(String filePath, String fileName) throws FileNotFoundException, ConfigurationError {
        this(new File(filePath, fileName));
    }

    public String setPrefix(String text, boolean includeLineNo) {
        String fullText = "In file " + file.getAbsolutePath();

        if (includeLineNo) {
            fullText += "(" + (current == null? loadLine : current.lineNo) + ")";
        }
        return fullText += " " + text;
    }
    /*
     * Sets the read position to the first section that starts with the characters
     * Match. Case is ignored in checking for a match.
     *
     * Setting match to "" matches all section names, i.e this will position to the
     * first section.
     */
    public void setFirstSection(String match) {
        this.match  = match.toUpperCase();
        current = null;
        index   = -1;
    }

    /*
     * Changes the section match string for the next ReadSection so that the
     * ReadSection will return the next section following the last section
     * read that matches NewMatch.
     */
    public void changeMatch(String newMatch) {
        match = newMatch.toUpperCase();
    }

    /*
     * Reads the next section matching the Match string and sets the read position
     * to the next section matching Match.
     *
     * Returns true if a section is successfully read.
     */
    public boolean readSection() {
        current = null;
        sname   = null;

        for (index = index + 1; index < items.size(); index++) {
            ConfigurationItem item = items.get(index);

            if (item.isSection && item.id.startsWith(match)) {
                sname = item.id;
                return true;
            }
        }
        return false;
    }

    /*
     * Returns the section name.
     */
    public String getSectionName() throws ConfigurationError {
        if (sname == null) {
            setError("No current section", true);
        }
        return sname;
    }
    /*
     * Sets the property read pointer the first property of the current section. This
     * is implicitly called following a successful ReadSection.
     */
    public void setFirstProperty() throws ConfigurationError {
        if (sname == null) {
            setError("No current section", true);
        }
        current = null;
    }
    /*
     * Reads the next property and sets the read property pointer to the next property
     * in the current section.
     *
     * Returns true if a property is successfully read.
     */
    public boolean readProperty() throws ConfigurationError {
        if (sname == null) {
            setError("No current section", true);
        }
        current = null;

        if (index < 0 || index >= items.size() - 1) {
            return false;
        }

        current = items.get(index + 1);

        if (!current.isSection) {
            ++index;
            
            return true;
        }
        return false;
    }
    private void checkCurrent() throws ConfigurationError {
        if (current == null) {
            setError("No current property", true);
        }
    }
    /*
     * Returns the property name.
     */
    public String getPropertyValue() throws ConfigurationError {
        checkCurrent();
        
        return current.value;
    }

    public boolean getPropertyBooleanValue() throws ConfigurationError {
        checkCurrent();

        return current.getBoolean();
    }

    public int getPropertyIntegerValue() throws ConfigurationError {
        checkCurrent();

        if (!current.isInteger()) {
            setError("Property value is not an integer", true);
        }
        return current.intVal;
    }
    /*
     * Returns the property name.
     */
    public String getPropertyName() throws ConfigurationError {
        checkCurrent();
        
        return current.id;
    }
    public boolean isInteger() {
        if (current == null) {
            return false;
        }
        return current.isInteger();
    }

    public boolean isBoolean() {
        if (current == null) {
            return false;
        }
        return current.isBoolean();
    }
    private transient ArrayList<ConfigurationItem> items    = new ArrayList<ConfigurationItem>();
    private transient int                          index    = -1;
    private transient String                       match    = "";
    private transient ConfigurationItem            current  = null;
    private transient String                       sname    = null;
    private transient File                         file     = null;
    private transient int                          loadLine = 0;
}