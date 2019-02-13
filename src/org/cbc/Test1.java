package org.cbc;

import org.cbc.application.reporting.Trace;
import org.cbc.application.reporting.Thread;
import org.cbc.application.reporting.Process;


/*
 * Test1.java
 *
 * Created on 07 October 2001, 10:06
 */
/**
 *
 * @author cclose
 * @version
 */
public class Test1 extends Object {

    private void delay(int seconds) {
        try {
            java.lang.Thread.currentThread().sleep(1000 * seconds);
        } catch (InterruptedException e) {
            System.out.println("Delay " + e.toString());
        }
    }

    static void printTraceFlags(String identifier) {
        Process process = Process.getProcess(identifier);
        String flags = "";

        for (int i = 0; i < 256; i++) {
            if (process.isTraceEnabled((char) i)) {
                if ((char) i <= ' ') {
                    if (flags.equals("")) {
                        flags = "!";
                    } else {
                        flags = flags + ',';
                    }
                    flags = flags + i;
                } else {
                    flags = flags + (char) i;
                }
            }
        }
        System.out.println("Trace flags " + flags);
    }

    /**
     * Creates new Test1
     */
    public Test1() {
    }

    private void innerProcess(String name, boolean release) {
//        Thread.ThreadProcess process = Thread.getThreadProcess("Inner." + name);
        Trace t = new Trace("InnerProcess");
        if (release) {
//            process.release();
        }
    }

    private void testProcess(String name, boolean release) {
//        Thread.ThreadProcess process = Thread.getThreadProcess(name);

        Trace t = new Trace("TestProcess");
        innerProcess(name, release);
        if (!release) {
   //         process.reset();
        }
        t.report('A', "After call");
        t.exit();
     //   process.release();
    }

    public void execute(boolean printFlags) {
        Trace t = new Trace("Test1.Execute");
        testProcess("Pr1", true);
        int count = 5;

        while (count-- > 0) {
            t.report('F', "Trace flag F");
            testProcess("Pr2", false);
            t.report('X', "Trace flag X");
            if (printFlags) {
                printTraceFlags("Test1");
            }
            delay(1);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        Thread.attach(null);

        Test1 Test = new Test1();
        Test.execute(false);
        Thread.detach();
        System.out.println("Test Complete");
    }
}
