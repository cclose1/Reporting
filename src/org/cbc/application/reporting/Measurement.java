package org.cbc.application.reporting;

import org.cbc.application.MeasurementInterface;
import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 01/Jul/01, C.B. Close:</b> Implements MeasurementInterface.
 * @version <b>v1.2, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.3, 02/Dec/01, C.B. Close:</b> Add text to report.
 * @version <b>v1.4, 30/Jan/02, C.B. Close:</b> Changed measure report to always
 * absolute memory used.
 */
public class Measurement implements MeasurementInterface, Serializable {

    public Measurement() {
        setBase();
    }
    /**
     * Resets the measurement base.
     */
    public void setBase() {
        if (measure) {
            stats = new ProcessStats();
        }
    }
    /**
     * Generates a report to the Measurement stream.
     *
     * Relative is true if the report is to be relative to the measure base,
     * otherwise it is relative to the point at which the process was attached
     * to the thread.
     */
    public void report(boolean relative, String reference, String text) {
        long time;
        long free;

        if (!measure) {
            return;
        }

        ProcessStats current = new ProcessStats();
        free = current.freeMemory;
        time = current.time - ((relative) ? stats.time : initialStats.time);
        Thread.report(
                "MEASUREMENT",
                reference,
                false,
                null,
                text + " " + ((relative) ? "R time " : "A time ")
                + fmt.format(time / 1000.0)
                + " memory free " + free
                + " total " + current.totalMemory);
    }

    public void report(boolean relative, String text) {
        report(relative, null, text);
    }
    private transient static DecimalFormat fmt          = new DecimalFormat("0.000");
    private transient        ProcessStats  stats        = null;
    private transient        ProcessStats  initialStats = Thread.getInitialStats();
    private transient        boolean       measure      = Thread.getMeasure();
}
