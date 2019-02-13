package org.cbc.application;

import java.io.Serializable;

/**
 * The class enables a check to be be made that a specified amount of time has elapsed since
 * a reference point.
 *
 *  @version <b>v1.0, 11/Jun/01, C.B. Close:</b>  Initial version.
 *  @version <b>v1.1, 11/Jul/01, C.B. Close:</b>  Implement Serializable.
 */
public class Interval implements Serializable {
    /*
     * Initialises the delay check to report true if future call exceeds
     * the current time by Delay milliseconds.
     *
     * If immediate is true the interval will signal Lapsed on the first
     * check.
     */

    public Interval(long Delay, boolean Immediate) {
        mTime = System.currentTimeMillis();

        if (Immediate) {
            mTime -= Delay;
        }

        mDelay = Delay;
    }
    /*
     * Returns false if the time lapsed since the reference time does not exceed
     * Delay.
     *
     * If time lapsed is greater than Delay, the reference time is set to the
     * current time and true is returned.
     */
    public boolean lapsed() {
        long time = System.currentTimeMillis();

        if ((mDelay + mTime) > time) {
            return false;
        }
        mTime = time;
        return true;
    }
    private long mDelay;
    private long mTime;
}
