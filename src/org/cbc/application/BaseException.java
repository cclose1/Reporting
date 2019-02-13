package org.cbc.application;

import org.cbc.application.reporting.Report;
import org.cbc.application.reporting.Trace;
import java.io.Serializable;

/**
 * The base class for application exceptions.
 *
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.1, 24/Nov/01, C.B. Close:</b> Added new constuctor and
 * accessor methods.
 */
public class BaseException extends RuntimeException implements Serializable {

    private void setException(Trace trace, String module, String reference, boolean reported, String shortText, String fullText) {
        this.module    = toString(module);
        this.reference = toString(reference);
        this.reported  = reported;
        this.shortText = shortText;
        this.fullText  = fullText;
        this.type      = "ERROR";
        this.number    = -1;
        trace.report(Trace.EXCEPTION, "Module " + this.module + " reference " + this.reference + " fulltext " + this.fullText);
    }

    public BaseException(Trace trace, String module, String reference, boolean reported, String shortText, String fullText) {    
        setException(trace, module, reference, reported, shortText, fullText);
    }
    public BaseException(String module, String reference, boolean reported, String shortText, String fullText) {
        Trace trace = new Trace("Application.Exception", Trace.EXCEPTION);
        setException(trace, module, reference, reported, shortText, fullText);
        trace.exit();
    }

    public void setError(String type, int number) {
        this.type   = type;
        this.number = number;
    }

    public String getModule() {
        return module;
    }

    public String getReference() {
        return reference;
    }

    public boolean getReported() {
        return reported;
    }

    public String getShortText() {
        return shortText;
    }

    public String getFullText() {
        return fullText;
    }

    public String getUserText() {
        return (shortText.equals("")) ? fullText : shortText;
    }

    public String getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    public String toString() {
        StringBuffer Text = new StringBuffer();
        addText(Text, "Module ", module);
        addText(Text, "Reference ", reference);
        addText(Text, "Type ", type);
        
        if (number != -1) {
            addText(Text, "Number ", new Integer(number).toString());
        }
        addText(Text, "-", fullText);
        return Text.toString();
    }

    public void report() {
        if (!reported) {
            if (type.equals("ERROR")) {
                Report.error(reference, fullText);
            } else if (type.equals("EVENT")) {
                Report.event(reference, fullText);
            } else {
                Report.audit(reference, fullText);
            }
        }
        reported = true;
    }

    private void addText(StringBuffer text, String key, String value) {
        if (!value.equals("")) {
            text.append(((text.length() == 0) ? key : ' '));
            text.append(key.toLowerCase());
            text.append(value);
        }
    }

    private String toString(String Text) {
        return (Text == null) ? "" : Text.trim();
    }
    private String  module;
    private String  reference;
    private boolean reported;
    private String  shortText;
    private String  fullText;
    private String  type;
    private int     number;
}
