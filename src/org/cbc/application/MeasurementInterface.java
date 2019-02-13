/*
 * MeasurementInterface.java
 *
 * Created on 01 July 2001, 21:55
 */
package org.cbc.application;

/**
 *
 * @author cclose
 * @version <b>v1.0, 01/Jul/01, C.B. Close:</b> Initial version.
 */
public interface MeasurementInterface {
    /**
     * Resets the measurement base.
     */
    public void setBase();

    /*
     * Generates a report to the Measurement stream.
     *
     * Relative is true if the report is to be relative to the measure base,
     * otherwise it is relative to the point at which the process was attached
     * to the thread.
     */
    public void report(boolean relative, String reference, String text);

    public void report(boolean relative, String text);
}
