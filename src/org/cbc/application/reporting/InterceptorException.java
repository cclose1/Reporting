package org.cbc.application.reporting;

import java.io.Serializable;
/**
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.0, 11/Jul/01, C.B. Close:</b> Initial version.
 */
public class InterceptorException extends Exception implements Serializable {

    public InterceptorException(int errorCode, String errorText) {
        mErrorCode = errorCode;
        mErrorText = errorText;
    }
    public String toString() {
        return "Error " + mErrorCode + "-" + mErrorText;
    }
    int    mErrorCode = 0;
    String mErrorText;
}
