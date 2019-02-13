package org.cbc.application.reporting;

import org.cbc.application.BaseException;
import org.cbc.application.Parameters;
import java.io.Serializable;

/**
 * The Reports class provides static methods for writing reports. The static
 * method Attach is used to assign a process description to the current thread.
 * The process description determines how the report is handled. If there is no
 * attached process when a report method is executed, an implicit Attach is
 * performed with Identifier ANON.
 *
 * The Attach establishes Identifier as the Id report parameter.
 *
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 28/Jun/01, C.B. Close:</b> Correction to comments.
 * @version <b>v1.2, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.3, 18/Nov/01, C.B. Close:</b> Added user parameters.
 * @version <b>v1.4, 24/Nov/01, C.B. Close:</b> Added throwError Module
 * overload.
 * @version <b>v1.5, 27/Jan/02, C.B. Close:</b> Added Process.
 */
public class Report implements Serializable {
    /**
     * Generates a report to the Comment stream.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     */
    public static void comment(String ref, String text) {
        Thread.report("COMMENT", ref, false, null, text);
    }    
    /**
     * Returns the report that be written to the comment stream by the above.
     *
     * @param Ref  Report reference.
     * @param Text Report text.
     */
    public static String commentText(String ref, String text) {
        return Thread.reportText("COMMENT", ref, null, text);
    }
    /**
     * Generates a report to the Error stream.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     */
    public static void error(String ref, String text) {
        error(null, ref, false, false, null, text, null, false);
    }   
    /**
     * Returns the report that be written to the error stream by the above.
     *
     * @param Ref  Report reference.
     * @param Text Report text.
     */
    public static String errorText(String ref, String text) {
        return Thread.reportText("ERROR", ref, null, text);
    }    
    /**
     * Generates a report to the Error stream.
     *
     * @param Ref       Report reference.
     * @param Exception Report exception.
     */
    public static void error(String ref, Exception exception) {
        error(null, ref, false, false, null, "Exception", exception, true);
    }
    /**
     * Generates a report to the Error stream.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     * @param Exception Report exception.
     */
    public static void error(String ref, String text, Exception exception) {
        error(null, ref, false, false, null, text, exception, true);
    }
    /**
     * Generates a report to the Error stream.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     * @param Exception Report exception.
     * @param StackTrace If true stack trace is output.
     */
    public static void error(String ref, String text, Exception exception, boolean stackTrace) {
        error(null, ref, false, false, null, text, exception, stackTrace);
    }
    /**
     * Generates a report to the Error stream and throws exception
     * ApplicationException.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     */
    public static void throwError(String ref, String text) {
        error(null, ref, false, true, null, text, null, false);
    }
    /**
     * Generates a report to the Error stream and throws exception
     * ApplicationException.
     *
     * @param Module Report module.
     * @param Ref Report reference.
     * @param Brief Description of the exception suitable for display to a user.
     * @param Full Description of the exception giving the maximum information
     * suitable for technical support.
     */
    public static void throwError(String module, String ref, String brief, String full) {
        error(module, ref, false, true, brief, full, null, false);
    }
    /**
     * Generates a report to the error stream and throws ApplicationException.
     * The difference between this and throwError is that abort gives the
     * interceptor the option to throw an implementation specific exception. The
     * interceptor should always return for error and throwError.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     */
    public static void abort(String ref, String text) {
        error(null, ref, true, true, null, text, null, false);
    }
    /**
     * Generates a report to the Audit stream.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     */
    public static void audit(String ref, String text) {
        Thread.report("AUDIT", ref, false, null, text);
    }
    /**
     * Generates a report to the event stream.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     */
    public static void event(String ref, String text) {
        Thread.report("EVENT", ref, false, null, text);
    }
    /**
     * Generates a report to the event stream. The report will not be generated
     * if the previous report text held for DuplicateKey matches Text. The
     * report text is held against DuplicateKey to check against later call
     * using the same DuplicateKey.
     *
     * The scope of the duplicate check is any process running with the same
     * identifier.
     *
     * @param Ref Report reference.
     * @param Text Report text.
     * @param DuplicateKey Key to the message to be checked against Text.
     */
    public static void event(String ref, String duplicateKey, String text) {
        Thread.report("EVENT", ref, false, duplicateKey, text);
    }
    /**
     * Clears the message held for Key. If Key is null all duplicate keys for
     * the process are cleared.
     *
     * @param Key Key to duplicate check data.
     */
    public static void clearDuplicate(String key) {
        Thread.clearDuplicate("EVENT", key);
    }
    /**
     * User parameters can be defined for reports. The case of the parameter
     * name is ignored. Reserved names are any identifier starting with $ and
     * the following PID, HOSTNAME, ID, MOD, REF, ERRORREF, ERRORFILE, ERRORLINE
     * HREADINDEX. Attempts to use a reserved parameter in the setParameter or
     * clearParameter methods will result in an Exception.
     * <P>
     * Creates a new parameter or replaces the value if it already exists.
     *
     * @param Name is the parameter name. It is not case sensitive.
     * @param Value Parameter value.
     */
    public static void setParameter(String name, String value) {
        Thread.setParameter(name, value);
    }
    /**
     * Creates a new parameter or replaces the value if it already exists.
     *
     * @param Name is the parameter name. It is not case sensitive.
     * @param Value Parameter value.
     */
    public static void setParameter(String name, int value) {
        Thread.setParameter(name, value);
    }
    /**
     * Removes parameter Name.
     *
     * Note: It is not an error if parameter Name does not exist.
     *
     * @param Name of parameter to be removed.
     */
    public static void clearParameter(String name) {
        Thread.clearParameter(name);
    }
    /**
     * Clears all the parameters set using the setParameter methods, i.e. the
     * reserved parameters are not cleared.
     */
    public static void clearParameters() {
        Thread.clearParameters();
    }
    /**
     * Returns the parameter's value.
     *
     * The empty string is returned if the parameter does not exist.
     *
     * @param Name is the name of the parameter.
     * @return Parameter value.
     */
    public static String getParameterValue(String Name) {
        return Thread.getParameters().getValue(Name);
    }
    /*
     * Returns true if the parameter exists.
     *
     * Note : The getParameterValue method returns the empty string if either the parameter value is
     *        the empty string or does not exist. This method enables you to determine the
     *        reason the empty string is returned.
     *
     * @param Name the parameter name.
     */
    /**
     * Tests if parameter Name exists.
     *
     * @param Name Parameter name.
     * @return True if parameter exists and false if not
     */
    public static boolean parameterExists(String name) {
        return Thread.getParameters().exists(name);
    }

    /*
     * Reports the error for all the above error methods.
     *
     * It generates the event report first if the ErrorEventId for the
     * thread is not null.
     */
    private static void error(String module, String ref, boolean abort, boolean throwException, String brief, String full, Exception exception, boolean stackTrace) {
        Parameters params       = Thread.getParameters();
        String     errorEventId = Thread.getErrorEventId();
        String     mod          = null;

        if (module != null) {
            mod = params.getValue("MOD");
            params.setValue("MOD", module);
        }
        if (errorEventId != null) {
            params.setValue("ERRORREF", ref);
            Thread.report("EVENT", errorEventId, abort, null, full);
            params.clear("ERRORREF");
        }
        Thread.report("ERROR", ref, abort, null, full, exception, stackTrace);
        if (mod != null) {
            params.setValue("MOD", mod);
        }
        if (throwException) {
            throw new BaseException((module == null) ? params.getValue("MOD") : module, ref, true, brief, full);
        }
    }
}
