/*
 * TraceInterface.java
 *
 * Created on 28 June 2001, 08:37
 */
package org.cbc.application;

/**
 *
 * @author cclose
 * @version
 */
public abstract interface TraceInterface {
    /**
     * Returns true if traceType is enabled for the Trace object.
     *
     * @param traceType is the trace type to be tested for.
     */
    public boolean isTraceEnabled(char traceType);
    /**
     * Generates a report to the trace stream.
     *
     * @param Type is the report trace type.
     * @param Text is the report text.
     */
    public void report(char type, String text);
    /**
     * Called on completion of the trace module and generates the exit trace
     * report. If this method is not called the first method executed by a
     * previous trace class instantiation will execute a call to Exit for later
     * trace class instantiations.
     */
    public void exit();
}
