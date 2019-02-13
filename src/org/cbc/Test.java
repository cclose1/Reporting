package org.cbc;


import org.cbc.application.Configuration;
import org.cbc.application.Parameters;
import org.cbc.application.reporting.Measurement;
import org.cbc.application.reporting.OutputFile;
import org.cbc.application.reporting.Process;
import org.cbc.application.reporting.Process.Stream;
import org.cbc.application.reporting.ProcessStats;
import org.cbc.application.reporting.Report;
import org.cbc.application.reporting.Trace;
import org.cbc.application.reporting.Thread;
import org.cbc.application.Token;
import org.cbc.application.Utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

class TestThread extends java.lang.Thread {

    private String identifier = null;
    private int    count = 0;
    private long   delay = 1;
    private int    waitMS = 0;

    private void delay(int units) {
        try {
            java.lang.Thread.currentThread().sleep(waitMS * units);
            Thread.attach(identifier);
        } catch (InterruptedException e) {
            System.out.println("Delay " + e.toString());
        }
    }

    public TestThread(String identifier, int count, long delay, int waitMS) {
        this.identifier = identifier;
        this.count      = count;
        this.delay      = delay;
        this.waitMS     = waitMS;
    }

    private void test3() {
        Trace t = new Trace("Test3");
        delay(3);
        t.exit();
    }

    private void test2(int calls, int index, boolean exit) {
        Trace t = new Trace("Test2-" + index);

        if (calls > 0) {
            test2(calls - 1, index + 1, exit);
        }
        delay(2);
        test3();
        if (exit) {
            t.exit();
        }
    }

    private void test1() {
        Trace       trace   = new Trace("Test1", 'Q');
        Measurement measure = new Measurement();

        delay(1);
        Report.error("ERR1", "Error in test 1");
        Report.event("TS1", "KEY1", "Test1 event1");
        Report.event("TS1", "KEY2", "Test1 event2");
        Report.event("TS1", "KEY1", "Test1 event1");
        Report.event("TS1", "KEY3", "Test1 event3");
        Report.clearDuplicate("KEY1");
        Report.event("TS1", "KEY1", "Test1 event1");
        Report.audit("TS1", "Test1 audit1");
        test2(5, 1, false);
        test2(3, 1, true);
        measure.report(true, "Test1");
        measure.report(false, "Test1");
        trace.exit();
    }

    public void run() {
        Thread.attach(identifier);
        
        try {
            for (int i = 0; i < count; i++) {
                test1();
                java.lang.Thread.currentThread().sleep(delay);
            }
        } catch (InterruptedException e) {
            System.out.println("Run " + e.toString());
        }
        Thread.detach();
    }
}

public class Test {

    static ArrayList<String> ids = new ArrayList<String>();

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

    static void test1() {
        Trace trace = new Trace("Test1");
    }

    static void print(String prefix, Stream stream) {
        Trace trace = new Trace("Print 1");

        test1();
        System.out.println(prefix
                + "Stream "
                + stream.getName()
                + " file template " + stream.getFileTemplate()
                + " report prefix " + stream.getReportPrefix());
        trace.exit();
    }

    static void print(Process process) {
        Trace trace = new Trace("Print 2");
        trace.report((char) 1, "trace 1");
        System.out.println("Identifier " + process.getIdentifier());

        for (Stream str : process.getStreams()) {
            print("  ", str);
        }
        trace.exit();
    }

    static void getIds(String File) {
        try {
            Configuration Config = new Configuration(File);
            Config.setFirstSection("ARID");

            while (Config.readSection()) {
                Config.setFirstProperty();
                while (Config.readProperty()) {
                    if (Config.getPropertyName().equals("NAME")) {
                        ids.add(Config.getPropertyValue());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.toString());
        } catch (Configuration.ConfigurationError e) {
            System.out.println(e.toString());
        }
    }

    private static void startThread(String identifier, int count, long delay, int waitMS) {
        TestThread t = new TestThread(identifier, count, delay, waitMS);
        new java.lang.Thread(t).start();
    }

    private static void testSerialize() throws IOException {
        Trace       test       = new Trace("testSerialize");
        Measurement measure    = new Measurement();
        OutputFile  outputFile = OutputFile.open("ARConfig.cfg", true);
        Process     process    = Process.getProcess("ANON");
        Utilities   utilities  = new Utilities();
        Token       token      = new Token("");
        Configuration config;
        try {
            FileOutputStream   ostream = new FileOutputStream("t.tmp");
            ObjectOutputStream p       = new ObjectOutputStream(ostream);
            
            config = new Configuration("ARConfig.cfg");

            p.writeObject(test);
            p.writeObject(measure);
            p.writeObject(outputFile);
            p.writeObject(process);
            p.writeObject(utilities);
            p.writeObject(token);
            p.writeObject(config);

            p.flush();
            ostream.close();
            System.out.println("Serialized");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.toString());
        }
        test.exit();
    }

    public static void main(String[] args) {
        //Trace Trace = new Trace("main");
        Thread.ThreadStatistics stats  = Thread.getThreadStatistics();
        Parameters              params = new Parameters();
        
        try {
            Process.setReportingRoot("C:\\MyFiles\\Java\\Reporting");
            testSerialize();
            Report.comment("COM1", "Text" + '\n' + "another line");
            System.out.println(Report.commentText("COM1", "Text" + '\n' + "another line"));
            params.setValue("Id", "Identifier");
            OutputFile file1;
            OutputFile file2;
            OutputFile file3;
            printTraceFlags("Id1");

            file1 = OutputFile.open("Test1.txt", true);
            file2 = OutputFile.open("Test1.txt", true);
            file3 = OutputFile.open("Test1.txt", false);
            FileOutputStream file4 = new FileOutputStream("Test2.txt");
            PrintWriter file5 = new PrintWriter((OutputStream) file4);

            System.getProperties().list(file5);

            file5.close();
            file4.close();
            System.out.println("File1 same as File2 " + (file1 == file2));
            System.out.println("File1 same as File3 " + (file1 == file3));
            file1.out.println("Write via file1");
            file2.out.println("Write via file2");
            file3.out.println("Write via file3");
            file2.out.flush();
            file1.out.close();
            file2.out.close();
            file3.out.close();
        } catch (IOException e) {
            System.out.println("File exception " + e.toString());
        }
        getIds("ARConfig.cfg");
        System.out.println("Ascii a" + (int) 'a');
        System.out.println(params.substitute("%a %b %d%m%y %H:%M:%S:%T %Y"));
        System.out.println(params.substitute("%a %b %d%m%y %H:%M:%S:%T %Y"));
        System.out.println(params.substitute("%a %b %d%m%y %H:%M:%S:%T %Y %z %% !ID!x"));
        System.out.println(params.substitute("%a %b %d%m%y %H:%M:%S:%T %Y %z %% !ID+c !y"));
        System.out.println(params.substitute("%a %b %d%m%y %H:%M:%S:%T %Y %z %% a!ID1+c !x"));
        System.out.println(params.substitute("%a %b %d%m%y %H:%M:%S:%T %Y %z %% b!ID1!x"));

        File file = new File("ARConfig.cfg");
        System.out.println(file.getAbsolutePath());

        for (Process p : Process.getProcesses()) {
            print(p);
        }
        startThread("Id1", 2, 100, 20);
        startThread("Id2", 2, 500, 10);
        startThread("Id2", 2, 500, 10);
        startThread("Id3", 2, 250, 30);
        System.out.println("Free " + ProcessStats.runtime.freeMemory());
        System.out.println("Free " + ProcessStats.runtime.freeMemory());
        printTraceFlags("test1");
        printTraceFlags("test1");
        try {
            java.lang.Thread.currentThread().sleep(3000);
        } catch (InterruptedException ex) {
            Report.error(null, ex.getMessage());
        }
        stats.reload();
        System.out.println("Threads " + stats.getThreads() + " inactive " + stats.getInactiveThreads());
    }
}