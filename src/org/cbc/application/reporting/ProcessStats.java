/**
 *****************************************************************************
 *
 * CONFIDENTIALITY STATEMENT
 *
 * All information contained in this program is confidential to CSC Computer
 * Sciences Limited. No part of this program may be reproduced by any means, nor
 * transmitted, nor translated into a machine language or other language without
 * the permission of CSC Computer Sciences Limited
 *
 *****************************************************************************
 */
package org.cbc.application.reporting;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 */
public class ProcessStats implements Serializable {

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Unknown";
        }
    }
    final public transient static Runtime runtime     = Runtime.getRuntime();
    final public transient        long    time        = System.currentTimeMillis();
    final public transient        long    freeMemory  = runtime.freeMemory();
    final public transient        long    totalMemory = runtime.totalMemory();
    final public transient        String  hostName    = getHostName();
}
