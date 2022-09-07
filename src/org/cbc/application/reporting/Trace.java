package org.cbc.application.reporting;

import org.cbc.application.TraceInterface;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * The Trace class provides methods that enables the generation of trace
 * reports. Each trace report has a type and report text. Each trace report is
 * associated with a module and optionally a trace group. The module name and
 * trace group are defined when the trace object is created. The Trace class
 * should be declared at the start of each method or class constructor for which
 * identified trace statements are required. The Trace class should never be
 * used to declare variables with global scope as this can cause the module
 * entry and exit order to be inaccurate. Where possible the trace module
 * indicates there is a problem by following the trace type in the report with
 * !!codes. The code letters are X for exit called more than once and R if not
 * on the correct thread.
 * <P>The trace type is a character that can have any value with an ascii code
 * in the range 1 to 255.
 * <P>The module name provides the value of the MOD report parameter and ideally
 * should bear some obvious relationship to the method in which the trace object
 * is created.
 * <P>A trace group provides a means of controlling the trace statements in the
 * group. A trace statement belongs to a group if it is called from the trace
 * object created with the group or any method in calling tree below it. A trace
 * statement can belong to a number of trace groups.
 * <P>Trace can be controlled by Module, Trace Type or Group, e.g. all trace for
 * group X could be turned off.
 *
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 28/Jun/01, C.B. Close:</b> Implement TraceInterface.
 * @version <b>v1.2, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.3, 25/Oct/01, C.B. Close:</b> Added trace group.
 * @version <b>v1.4, 25/Oct/01, C.B. Close:</b> Handle deserialization.
 * @version <b>v1.5, 03/Jan/02, C.B. Close:</b> Parameter removed from module
 * exit.
 */
public class Trace implements TraceInterface, Serializable {

    public final static char SPARAM = 'p';
    public final static char LPARAM = 'P';
    public final static char SVALUE = 'v';
    public final static char LVALUE = 'V';
    public final static char SRESULT = 'r';
    public final static char LRESULT = 'R';
    public final static char SCOMMENT = 'c';
    public final static char LCOMMENT = 'C';
    public final static char EXCEPTION = 'X';
    public final static char SParam = 'p';
    public final static char LParam = 'P';
    public final static char SValue = 'v';
    public final static char LValue = 'V';
    public final static char SResult = 'r';
    public final static char LResult = 'R';
    public final static char SComment = 'c';
    public final static char LComment = 'C';
    public final static char Exception = 'X';

    /**
     * Returns true if TraceType is enabled for the process attached to the
     * current thread. True is returned if there is no process attached to the
     * current thread.
     *
     * @param TraceType Trace type to be tested for.
     * @return True if trace is enabled.
     */
    public static boolean isTraceTypeEnabled(char traceType) {
        return Thread.isTraceEnabled(traceType);
    }
    /**
     * Constructs a trace instance for Module with entry and exit trace reports
     * of EntryTraceType belonging to Group.
     *
     * @param Module Trace module name.
     * @param EntryTraceType Trace type for the entry and exit trace reports.
     * @param Group Trace group to which trace reports are attached.
     */
    public Trace(String module, char entryTraceType, String group) {
        this.module = Thread.createModule(module, entryTraceType, group);
    }
    /**
     * Constructs a trace instance for Module with entry and exit trace reports
     * of EntryTraceType.
     *
     * @param Module the trace module name.
     * @param EntryTraceType the trace type for the entry and exit trace
     * reports.
     */
    public Trace(String module, char entryTraceType) {
        this(module, entryTraceType, null);
    }
    /**
     * Constructs a trace instance for Module with entry and exit trace reports
     * of trace type E.
     *
     * @param Module the trace module name.
     * @param Group Trace group to which trace reports are attached.
     */
    public Trace(String module, String group) {
        this(module, 'E', group);
    }
    /**
     * Constructs a trace instance for Module with entry and exit trace reports
     * of trace type E belonging to Group.
     *
     * @param Module the trace module name.
     */
    public Trace(String module) {
        this(module, 'E', null);
    }
    /**
     * Returns true if TraceType is enabled for the Trace object.
     *
     * @param TraceType Trace type to be tested for.
     * @return True if trace is enabled.
     */
    public boolean isTraceEnabled(char traceType) {
        return module != null && module.isTraceEnabled(traceType);
    }
    /**
     * Generates a report to the trace stream.
     *
     * @param Type The report trace type.
     * @param Text The report text.
     */
    public void report(char type, String text) {
        if (module != null && !module.noTrace()) {
            module.traceReport(type, text);
        }
    }
    /**
     * Generates a report for an identifier to the trace stream. The text of the
     * report is
     * <P>Identifier = Text
     *
     * @param Type The report trace type.
     * @param Identifer The identifier for the text.
     * @param Text The report text.
     */
    public void report(char type, String identifier, String text) {
        report(type, identifier + " = " + text);
    }
    /**
     * Called on completion of the trace module and generates the exit trace
     * report. If this method is not called the first method executed by a
     * previous trace class instantiation will execute a call to Exit for later
     * trace class instantiations.
     */
    public void exit() {
        if (module != null) {
            module.exit();
        }
    }
    /*
     * Trace objects should never be serailized as they should only ever be declared as local method
     * variables. If a trace object were serialized the thread to which mModule is attached would no longer
     * be relevant. So reallocate it to the current thread.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        if (module != null) {
            module = Thread.createModule(module);
        }
    }
    private Thread.Module module = null;
}
