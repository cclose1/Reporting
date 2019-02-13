package org.cbc.application.reporting;

import org.cbc.application.BaseException;
import org.cbc.application.Parameters;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.2, 17/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.3, 18/Nov/01, C.B. Close:</b> Added SetParameter and
 * ClearParameter methods.
 * @version <b>v1.4, 27/Jan/02, C.B. Close:</b> Allow for globally declared
 * trace modules.
 */
public class Thread implements Serializable {
    private static transient       Thread                            lastThread      = null;
    private static transient       HashMap<java.lang.Thread, Thread> threads         = new HashMap<java.lang.Thread, Thread>();
    private static transient       Set<String>                       reserved        = Collections.synchronizedSet(new HashSet<String>());
    private static transient       DecimalFormat                     fmt             = new DecimalFormat("0.000");
    private static transient       DecimalFormat                     twodigits       = new DecimalFormat("00");
    private static transient       boolean                           noTrace         = false;
    private static transient       Date                              traceUpdated    = null;
    private static transient final Object                            lock            = new Object();
    private transient              java.lang.Thread                  thread;
    private transient              Process                           process         = null;
    private transient              ArrayList<Module>                 modules         = new ArrayList<Module>();
    private transient              Parameters                        parameters      = null;
    private transient              ProcessStats                      initialStats    = null;
    private transient              int                               index           = 0;;
    private transient              boolean                           disabledByGroup = false;

    private Thread(java.lang.Thread thread, int index) {
        this.thread = thread;
        this.index  = index;
    }
    /*
     * NoTrace is used to minimize the locking caused by trace. It is set to true if none of the thread
     * processes have trace enabled. In this case it is not necessary to find the thread for the current
     * java thread.
     * 
     * If force is true then a change to threads has occured and noTrace is set up. Otherwise, it is is updated
     * if it Process has updated trace since the last updated by Thread.
     */
    private static void updateNoTrace(boolean force) {
        Date    newUpdate  = Process.getLastUpdate();
        boolean newNoTrace = noTrace;
                
        if (force || (newUpdate != null && (traceUpdated == null || traceUpdated.compareTo(newUpdate) < 0))) {
            synchronized (lock) {
                newNoTrace = true;
                
                for (Thread t : threads.values()) {
                    if (t.thread.isAlive() && t.process != null && t.process.isTraceEnabled()) {
                        newNoTrace = false;
                        break;
                    }
                }
            }
        }
        traceUpdated = newUpdate;
        noTrace      = newNoTrace;
    }
    /*
     * This is the only place where the threads object is accessed.
     */
    private static Thread loadThread() {
        java.lang.Thread thread = java.lang.Thread.currentThread();
        Thread           thrd   = lastThread;
        
        if (thrd == null || thrd.thread != thread) {
            /*
             * The thread is not the last one accessed. So get it from threads and create a
             * new Thread if not found.
             */
            synchronized (lock) {
                thrd = threads.get(thread);
                
                if (thrd == null) {
                    thrd = new Thread(thread, threads.size() + 1);
                    threads.put(thread, thrd);
                }
                lastThread = thrd;
            }
        }
        return thrd;
    }
    public static Thread getThread() {
        Thread thr = loadThread();

        if (thr.process == null) {
            attach(thr, null);
        }
        return thr;
    }

    private static void attach(Thread Thread, Process process) {
        if (process == null) {
            process = Process.getProcess("ANON");
        }       
        Thread.process         = process;
        Thread.initialStats    = new ProcessStats();
        Thread.parameters      = new Parameters(true);
        Thread.disabledByGroup = false;
        Thread.parameters.setValue("ID",          process.getIdentifier());
        Thread.parameters.setValue("PID",         Thread.thread.hashCode());
        Thread.parameters.setValue("THREADINDEX", Thread.index);
        Thread.parameters.setValue("HOSTNAME",    Thread.initialStats.hostName);
        
        updateNoTrace(true);
    }
    public static void attach(String identifier) {
        Process proc = Process.getProcess(identifier);

        Thread thr = getThread();

        //Return if the current thread is still executing the same process.

        if (thr.process == proc) {
            return;
        }
        attach(thr, proc);
    }
    public static void detach() {
        Thread thr = getThread();
        /*
         * Force exit on all modules in the modules stack;
         */
        if (thr.modules.size() > 0) thr.modules.get(0).exit(true);
        /*
         * Remove the current thread from threads. Detach should be called as the last thing before
         * closing the thread. If activity continues on the thread then another thread will be created.
         */
        synchronized(lock) {
            threads.remove(thr.thread);
            lastThread = null;
        }
        updateNoTrace(true);
    }
    public static void removeUnusedThreads() {
        boolean removed = false;
        
        synchronized(lock) {
            for (java.lang.Thread t : threads.keySet()) {
                if (!t.isAlive()) {
                    removed = true;
                    threads.remove(t);
                }
            }
        }
        if (removed) updateNoTrace(true);
    }
    public String getIdentifier() {
        return process.getIdentifier();
    }

    public static boolean isTraceEnabled(char traceType) {
        updateNoTrace(false);
        
        if (noTrace) return false;
        
        Process process = getThread().process;
        
        if (process == null) {
            return false;
        }
        return process.isTraceEnabled(traceType);
    }

    public static ProcessStats getInitialStats() {
        Thread thread = getThread();
        return thread.initialStats;
    }

    public static void report(String stream, String ref, boolean abort, String duplicateKey, String text, Exception exception, boolean stackTrace) {
        Thread thread = getThread();
        
        if (ref == null) {
            thread.parameters.clear("REF");
        } else {
            thread.parameters.setValue("REF", ref);
        } 
        thread.process.getStream(stream).output(null, null, text, thread.parameters, duplicateKey, exception, stackTrace);
    }    
    public static String reportText(String stream, String ref, String module, String text) {
        Thread thread = getThread();
        
        return thread.process.getStream(stream).reportText(ref, module, text, thread.parameters);
    }
    public static void report(String stream, String ref, boolean abort, String duplicateKey, String text) {
        report(stream, ref, abort, duplicateKey, text, null, false);
    }

    private static void setReserved() {
        if (reserved.isEmpty()) {
            reserved.add("ID");
            reserved.add("PID");
            reserved.add("MOD");
            reserved.add("REF");
            reserved.add("ERRORREF");
            reserved.add("ERRORFILE");
            reserved.add("ERRORLINE");
            reserved.add("THREADINDEX");
        }
    }

    private static void checkReservedParameter(String method, String name) {
        setReserved();

        if (name.length() != 0 && name.charAt(0) == '$' || reserved.contains(name.toUpperCase())) {
            throw new BaseException(method, "APPTHRD001", false, "", "Parameter " + name + " is reserved");
        }
    }

    public static Parameters getParameters() {
        Thread thread = getThread();
        return thread.parameters;
    }

    public static void setParameter(String name, String value) {
        checkReservedParameter("SetParameter", name);
        getThread().parameters.setValue(name, value);
    }

    public static void setParameter(String name, int value) {
        checkReservedParameter("SetParameter", name);
        getThread().parameters.setValue(name, value);
    }

    public static void clearParameter(String name) {
        checkReservedParameter("ClearParameter", name);
        getThread().parameters.clear(name);
    }

    public static void clearParameters() {
        setReserved();
        Parameters p = getParameters();

        for (String name : p.getNames()) {

            if (!reserved.contains(name)) {
                p.clear(name);
            }
        }
    }

    public static boolean getMeasure() {
        Thread thread = getThread();
        return thread.process.getMeasure();
    }

    public static String getErrorEventId() {
        Thread thread = getThread();
        return thread.process.getErrorEventId();
    }

    public static void clearDuplicate(String stream, String key) {
        Thread thread = getThread();
        thread.process.clearDuplicates(stream + key);
    }
    public class ThreadStatistics {
        private int     threads;
        private int     inactiveThreads;
        private Date    lastTraceUpdate;
        private boolean noTrace;
        
        public void reload() {
            threads = Thread.threads.size();
            
            for (java.lang.Thread t : Thread.threads.keySet()) {
                if (!t.isAlive()) inactiveThreads++;
            }
            lastTraceUpdate = Process.getLastUpdate();
            noTrace         = Thread.noTrace;
        }
        public ThreadStatistics() {
            reload();
        }

        /**
         * @return the threads
         */
        public int getThreads() {
            return threads;
        }

        /**
         * @return the inactiveThreads
         */
        public int getInactiveThreads() {
            return inactiveThreads;
        }

        /**
         * @return the lastTraceUpdate
         */
        public Date getLastTraceUpdate() {
            return lastTraceUpdate;
        }

        /**
         * @return the noTrace
         */
        public boolean isNoTrace() {
            return noTrace;
        }
    }
    private ThreadStatistics createThreadStatistics() {
        return new ThreadStatistics();
    }
    public static ThreadStatistics getThreadStatistics() {
        return loadThread().createThreadStatistics();
    }
    /*
     * Allows a different process to be assigned to thread which remains the attached
     * process until realease is called at which point the process at the time of construction
     * is reinstated.
     */
    public class ThreadProcess implements Serializable {
        private transient java.lang.Thread thread = java.lang.Thread.currentThread();
        private transient String           name;
        private transient int              traceLevel = modules.size();
        private transient Process          sProcess;
        private transient Parameters       sParameters;
        private transient ProcessStats     sInitialStats;
        private transient Process          oProcess;
        private transient Parameters       oParameters;
        private transient ProcessStats     oInitialStats;
        private transient boolean          released = false;
        
        private ThreadProcess(String name) {
            this.name     = name;
            sProcess      = process;
            sParameters   = parameters;
            sInitialStats = initialStats;
            Thread.this.attach(Thread.this, Process.getProcess(name));
            oProcess      = process;
            oParameters   = parameters;
            oInitialStats = initialStats;
            released      = false;
        }
        private ThreadProcess(Thread thread, String name) {
            this.name     = name;
            sProcess      = process;
            sParameters   = parameters;
            sInitialStats = initialStats;
            thread.attach(thread, Process.getProcess(name));
            oProcess      = process;
            oParameters   = parameters;
            oInitialStats = initialStats;
            released      = false;
        }
        /*
         * Restore the context before the creation clearing any module stack
         * entries adde since creation.
         */
        public void release() {
            if (thread == java.lang.Thread.currentThread() && !released) {
                reset();
                process      = sProcess;
                parameters   = sParameters;
                initialStats = sInitialStats;
                released     = true;
            }
        }
        /*
         * Clear module stack added since creation and restore context 
         * established by the creation.
         */
        public void reset() {
            if (thread == java.lang.Thread.currentThread() && !released) {
                if (traceLevel < modules.size() - 1) {
                    modules.get(traceLevel + 1).exit(true);
                }
                process      = oProcess;
                parameters   = oParameters;
                initialStats = oInitialStats;
            }
        }
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
    }
    
    private ThreadProcess getProcess(String name) {
        return new ThreadProcess(name);
    }
    public static ThreadProcess createThreadProcess(String name) {
        return loadThread().getProcess(name);
    }
    /*
    public static ThreadProcess createThreadProcessx(String name) {
        ThreadProcess p;
        Thread thr = loadThread(java.lang.Thread.currentThread());
        return loadThread(java.lang.Thread.currentThread()).getProcess(name);
    }
    */

    /**
     * If the maximum trace level has been reached returns the last allocated
     * module, otherwise creates a new module and returns it.
     */
    private Module newModule(String name, char traceType, String group) {
        Module mod;

        if (process == null) {
            attach("ANON");
        }

        if (modules.size() > process.getMaxTraceLevel()) {
            mod = modules.get(modules.size() - 1);
            mod.useCount += 1;
        } else {
            mod = new Module(name, traceType, group);
            modules.add(mod);
            mod.index = modules.size() - 1;
        }
        mod.traceReport(traceType, (group == null) ? "Enter" : "Enter in group " + group);

        return mod;
    }
    public static Module createModule(String name, char traceType, String group) {
        updateNoTrace(false);
        
        if (noTrace) return null;
        
        Thread mThread = Thread.getThread();
        return mThread.newModule(name, traceType, group);
    }
    /**
     * Creates a copy of Module that is not added to the Modules stack. This is
     * used by Trace module deserialization to reconnect the trace module to the
     * thread.
     */
    private Module newModule(Module module) {
        Module mod  = new Module(module.name, module.traceType, module.group);
        mod.copy    = true;
        mod.removed = module.removed;
        return mod;
    }
    public static Module createModule(Module module) {
        Thread mThread = Thread.getThread();

        Module mod  = mThread.newModule(module);
        mod.copy    = true;
        mod.removed = module.removed;
        return mod;
    }

    private static String toString(char ch) {
        if (ch >= ' ') {
            return "" + ch;
        }
        return twodigits.format((int) ch);
    }
    /*
     * The module class is used to hold details for the Trace object created on the entry to a method and
     * should always be a local method variable. If this is the case the thread active at the time of construction
     * will be the active thread for all the methods. 
     * 
     * However, there is no way to check that it is used in this way. It could be declared as global or static variable. This
     * will result in misleading trace information. An indication of incorrect usage is if the thread at the time
     * of executing a method is not as that at the time of construction.
     * 
     * Note: The initialised values of moduleTraceEnabled and removed ensure that that any serialized objects
     *       will not fail. They will however stop producing trace statements.
     */
    public class Module implements Serializable {
        private transient String           name;
        private transient char             traceType;
        private transient java.lang.Thread cThread;                    //Thread at time of construction
        private transient int              useCount           = 0;     //Number of times reused as a result of trace limit reached.
        private transient boolean          removed            = true;  //Set to true if already removed from the vector.
        private transient boolean          copy               = false; //Set to true if unattached copy.
        private transient boolean          exitCalled         = false;
        private transient String           group;
        private transient boolean          disabledByGroup    = Thread.this.disabledByGroup;
        private transient boolean          moduleTraceEnabled = false;
        private transient long             entryTime          = System.currentTimeMillis();
        private transient long             entryFree          = ProcessStats.runtime.freeMemory();
        private transient int              index              = -1;
        private transient Thread           lThread;                    //The last value returned by setCurrent.

        private Module(String name, char traceType, String group) {
            this.name      = name;
            this.traceType = traceType;
            cThread        = java.lang.Thread.currentThread();
            this.group     = group;
            lThread        = Thread.this;
            removed        = false;

            if (!disabledByGroup && group != null) {
                disabledByGroup = !process.isGroupEnabled(group.trim());
            }
            moduleTraceEnabled = !disabledByGroup && process.isModuleEnabled(name.trim());
        }

        public String getName() {
            return name;
        }
        /*
         * Note: This method and NoTrace should call setCurrent to ensure that Thrd is set for the
         *      current thread. However, these methods are called for every trace statement and setCurrent executes several
         *      statements. To make the trace enabled check as quick as possible lThread, the last value returned by setCurrent
         *      is used. The consequence is that on some occassions the trace  flag for the wrong process will be returned.
         */
        public boolean isTraceEnabled(char traceType) {
            return moduleTraceEnabled && lThread.isTraceEnabled(traceType);
        }

        public boolean noTrace() {
            return lThread.process.noTrace();
        }

        public void traceReport(char type, String text) {
            if (isTraceEnabled(type)) {
                report(setCurrent(), type, text);
            }
        }

        public void exit() {
            exit(setCurrent(), false);
        }

        private void report(Thread pThread, char type, String text) {
            if (moduleTraceEnabled && pThread.isTraceEnabled(type)) {
                String sType = Thread.toString(type);
                
                if (exitCalled) {
                    sType += " !!X";
                }
                if (removed) {
                    sType += ((sType.length() == 1) ? " !!R" : "R");
                }
                if (copy) {
                    sType += ((sType.length() == 1) ? " !!C" : "C");
                }
                pThread.process.getStream("TRACE").output(sType, name, text, parameters, null);
            }
        }

        private void exit(boolean forced) {
            exit(setCurrent(), forced);
        }

        private void exit(Thread pThread, boolean forced) {
            double lapsed = (System.currentTimeMillis() - entryTime) / 1000.0;

            if (useCount == 0 || forced) {
                report(pThread, traceType, "Exit" + ((forced) ? "-forced " : " ") + "elapsed " + fmt.format(lapsed));
            }

            if (useCount > 0) {
                useCount -= 1;
            } else {
                exitCalled = true;
            }

            if (!removed && !copy) {
                try {
                    modules.remove(index);
                } catch (Exception e) {
                    System.out.println("!!Got '" + e.toString() + "' on output of exit of module " + name);
                }
                removed = true;
            }
        }
        /*
         * Sets Thrd to the Thread object for the current java thread. This will be the owning thread unless
         * the current java thread has changed from that at the Module construction, i.e. there is misuse of the
         * trace facility. See class comment. This method reduces the impact of misuse but cannot correct for
         * all cases.
         * 
         * A force exit is perfomed for all the following modules in the methods stack as these modules have exited without
         * executing the module exit.
         */
        private Thread setCurrent() {
            Thread thrd;

            if (cThread != java.lang.Thread.currentThread()) {
                /*
                 * This should never be the case if the trace rules have been followed. Set to removed to
                 * prevent force exit from of following entries in the module stack as it belongs to a different
                 * thread and set thrd to that for the current thread to ensure that the process details for the 
                 * current java thread are used.
                 */
                removed = true;
                thrd = getThread();
            } else {
                thrd = Thread.this;
            }
            if (index >= modules.size()) {
                /*
                 * This can happen if trace module is declare as global and exit called for it later. This is a
                 * misuse of the trace facility.
                 */
                removed = true;
            }
            /*
             * Force exit on all modules at higher levels unless there is evidence that the trace intention has
             * been violated.
             */
            if (!removed && !copy) {
                for (int i = modules.size() - 1; index < i; i--) {
                    modules.get(i).exit(thrd, true);
                }
                lThread.disabledByGroup = disabledByGroup;
            }
            thrd.parameters.setValue("MOD", name);
            lThread = thrd;
            return thrd;
        }
    }
}
