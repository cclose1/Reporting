package org.cbc.application.reporting;

/**
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 * @version <b>v1.2, 31/Jul/01, C.B. Close:</b> Improve error reporting.
 * @version <b>v1.3, 28/Nov/01, C.B. Close:</b> Remove text formatting.
 */
import org.cbc.application.Configuration;
import org.cbc.application.Interval;
import org.cbc.application.Parameters;
import org.cbc.application.Token;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
/*
    private static final           int                      traceRefreshRate = 5;
    private static final           int                      fileRefreshRate = 600;
    private static transient final Object      
 * This class is serializable because WebLogic requires it's server code to be so. It does not
 * need to be, so all its data is transient.
 */
public class Process implements Serializable {
    private static final           int                      traceRefreshRate = 5;
    private static final           int                      fileRefreshRate  = 600;
    private static transient final Object                   lock             = new Object();
    private transient static       boolean                  initialised      = false;
    private transient static       Configuration            config           = null;
    private transient static       ReentrantLock            traceLock        = new ReentrantLock();
    private transient static       HashMap<String, Process> processes        = new HashMap<String, Process>();
    private transient static       String                   traceControl     = "TRACE.CTL";
    private transient static       String                   configFile       = "ARConfig.cfg";
    private transient static       String                   reportingRoot    = System.getProperty("user.home");
    private transient static       boolean                  noTrace          = false;
    private transient static       Interval                 traceRefresh     = new Interval(1000 * traceRefreshRate, true);
    private transient static       Date                     lastUpdate       = null;
    
    /*
     * If file is a valid file its File object is returned, otherwise, it is assumed to be relative to the 
     * reporting root and its File object is returned.
     */
    private static File getFile(String file) {
        File f = new File(file);
        
        if (!f.isFile()) f = new File(reportingRoot, file);
        
        return f;
    }
    /**
     * @return the last modified timestamp of the trace control file from which the process trace information
     * was set up. Null is returned if there no trace control file.
     * 
     * Can be used by other methods to determine if any of the process trace data could have changed.
     */
    public static Date getLastUpdate() {
        updateTrace(false);
        return lastUpdate;
    }

    /**
     * @param aTraceControl the traceControl to set
     */
    public static void setTraceControl(String aTraceControl) {
        traceControl = aTraceControl;
    }

    /**
     * @param ConfigFile the configFile to set
     */
    public static void setConfigFile(String reportingRoot, String configFile) {
        Process.configFile    = configFile;
        Process.reportingRoot = reportingRoot;
    }
    public static void setReportingRoot(String rootDir) {
        Process.reportingRoot = rootDir;
    }
    public static void setConfigFile(String configFile) {
        Process.configFile = configFile;
    }
    public static File getConfigFile() {
        return getFile(configFile);
    }
    private transient              Date                     updated          = null;
    private transient              HashMap<String, Stream>  streams          = new HashMap<String, Stream>();
    private transient              HashMap<String, String>  duplicates       = new HashMap<String, String>();
    private transient              String                   identifier       = "";
    private transient              String                   errorEventId     = null;
    private transient              TraceMask                trace            = new TraceMask();
    private transient              boolean                  measurements     = true;
    private transient              boolean                  traceEnabled     = true;
    private transient              String                   defaultTrace     = "+";
    private transient              int                      maxTrace         = 100;
    private transient              int                      maxDuplicates    = 100;

    private void error(String report) {
        System.out.println(config.setPrefix(report, true));
    }
    /*
     * If the Trace refresh time has elapsed or Immediate is true and tracing is enabled,
     * the traceControl file is read and acted upon.
     *
     * The control value consists of [Identifier]:Mask. If Identifier is
     * present the mask is applied to the configuration if it exists,
     * otherwise, Mask is applied to all the configurations.
     */
    protected static void updateTrace(boolean immediate) {
        if (traceControl == null) return;
        
        File tFile = getFile(traceControl);
       
        if (!tFile.exists() || noTrace || !(traceRefresh.lapsed() || immediate)) {
            return;
        } 
        Date fileTime = new Date(tFile.lastModified());
        /*
         * Return if the traceControl modified time is before or equal to the last process update.
         */
        if (getLastUpdate() != null && fileTime.compareTo(getLastUpdate()) <= 0) return;
        /*
         * Return if any thread already has the lock. If the lock is held then trace update is
         * in progress and repeating the update will yield the same reult.
         */
        if (traceLock.getHoldCount() != 0 || !traceLock.tryLock()) return;
        
        try {
            String         line;
            BufferedReader br    = new BufferedReader(new FileReader(traceControl));

            while ((line = br.readLine()) != null) {
                Token  mask       = new Token(line.trim());
                String delimiters = ":*$";
                String identifier = "";
                char   ch         = '\0';

                while (mask.moreCharacters() && delimiters.indexOf(ch = mask.nextCharacter()) == -1) {
                    identifier += ch;
                }
                identifier = identifier.trim();
                /*
                 * If the mask does not start with an identifier, set the token extract to the start
                 * to replace characters read in attempting to find an identifier.
                 */
                if (delimiters.indexOf(ch) == -1) {
                    mask.setIndex(0);
                }
                if (identifier.equals("")) {
                    int index = mask.getIndex();

                    for (Process p : processes.values()) {
                        p.setTrace(mask, ch);
                        p.updated = fileTime;
                        mask.setIndex(index);
                    }
                } else {
                    Process process = processes.get(identifier);

                    if (process != null) {
                        process.setTrace(mask, ch);
                        process.updated = fileTime;
                    }
                }
            }
            br.close();
        } catch (IOException e) {
        }
        lastUpdate = fileTime;
        traceLock.unlock();
    }
    private static void initialise() {
        
        if (initialised) {
            return;
        }
        if (reportingRoot != null && !(new File(reportingRoot)).isDirectory()) {
            System.err.println(reportingRoot + " is not a directory. Changed to " + System.getProperty("user.home"));
            reportingRoot = System.getProperty("user.home");
        }             
        File file = getFile(configFile);

        try {
            config = new Configuration(file);
        } catch (FileNotFoundException e) {
            System.err.println("Reporting configuration file "
                    + file.getAbsolutePath() + " not found. Applying default streams");
        } catch (Configuration.ConfigurationError e) {
            System.err.println("File error-" + e.toString() + " Applying default streams");
        }
        initialised = true;
    }
    public static Process getProcess(String identifier) {
        Process process;
        
        synchronized (lock) {
            process = processes.get(identifier);

            if (process == null) {
                process = new Process(identifier);
                processes.put(identifier, process);
                
                if (process.traceEnabled) noTrace = false;
                
                lastUpdate = null;
                updateTrace(true);
            } else {
                updateTrace(false);
            }
        }
        return process;
    }

    public static void close() {
        initialised = false;
        config      = null;
        processes   = null;
    }
    private int getValue(Configuration config, int def) throws Configuration.ConfigurationError {
        if (!config.isInteger()) {
            error("Integer expected. Defaulting to " + def);
            return def;
        }
        return config.getPropertyIntegerValue();
    }
    private boolean getValue(Configuration config, boolean def) throws Configuration.ConfigurationError {
        if (!config.isBoolean()) {
            error("Flag expected. Defaulting to " + def);
            return def;
        }
        return config.getPropertyBooleanValue();
    }

    private Interceptor getInterceptor(String className) {
        String myName = this.getClass().getName();
        int    end    = myName.lastIndexOf('.');

        String interceptorName = (end > 0) ? myName.substring(0, end) + ".Interceptor" : "Interceptor";

        try {
            Class   interceptor  = Class.forName(className);
            Class[] interceptors = interceptor.getInterfaces();

            for (int i = 0; i < interceptors.length; i++) {
                if (interceptors[i].getName().equals(interceptorName)) {
                    return (Interceptor) interceptor.newInstance();
                }
            }
            error("Class " + className + " does not implement Interceptor " + interceptorName);
        } catch (Exception e) {
            error(e.toString() + " setting interceptor " + className);
        }
        return null;
    }

    private Stream setStream(String name) {
        Stream str = getStream(name);

        if (str == null) {
            str = new Stream(name);
            streams.put(name, str);
        }
        return str;
    }

    private Stream setStream(String name, String fileTemplate, String reportPrefix) {
        Stream str = setStream(name);
        
        str.setFileTemplate(fileTemplate);
        str.setReportPrefix(reportPrefix);
        return str;
    }

    private void setStream(Configuration config) throws Configuration.ConfigurationError {
        String      open        = null;
        Interceptor interceptor = null;
        boolean     override    = false;

        config.setFirstProperty();
        Stream str;

        if (!config.readProperty() || !config.getPropertyName().equals("NAME")) {
            error("NAME not first property");
            return;
        }
        str = setStream(config.getPropertyValue().toUpperCase());

        while (config.readProperty()) {
            if (config.getPropertyName().equals("FILE")) {
                str.setFileTemplate(config.getPropertyValue());
            } else if (config.getPropertyName().equals("PREFIX")) {
                str.setReportPrefix(config.getPropertyValue());
            } else if (config.getPropertyName().equals("OPENINTERCEPTOR")) {
                open = config.getPropertyValue();
            } else if (config.getPropertyName().equals("INTERCEPTOR")) {
                interceptor = getInterceptor(config.getPropertyValue());
            } else if (config.getPropertyName().equals("OVERRIDEINTERCEPTOR")) {
                override = getValue(config, false);
            } else if (config.getPropertyName().equals("ALLOWREENTER")) {
                str.setAllowReenter(getValue(config, false));
            } else {
                error("Property " + config.getPropertyName() + " not recognised");
            }
        }

        if (interceptor != null) {
            str.setInterceptor(interceptor, open, override);
        }
    }

    private void configure(String key, String name) throws Configuration.ConfigurationError {
        boolean found = false;

        if (config == null) {
            return;
        }

        config.setFirstSection(key);

        while (config.readSection()) {
            if (name == null) {
                found = true;
                break;
            } else {
                config.readProperty();

                if (config.getPropertyName().equals("NAME") && config.getPropertyValue().equalsIgnoreCase(name)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            return;
        }

        while (config.readProperty()) {
            if (config.getPropertyName().equals("ERROREVENTID")) {
                errorEventId = config.getPropertyValue();
            } else if (config.getPropertyName().equals("TRACEDEFAULTS")) {
                defaultTrace = config.getPropertyValue();
            } else if (config.getPropertyName().equals("TRACECONTROL")) {
                setTraceControl(config.getPropertyValue());
            } else if (config.getPropertyName().equals("MAXDUPLICATES")) {
                maxDuplicates = getValue(config, 100);
            } else if (config.getPropertyName().equals("MAXTRACE")) {
                maxTrace = getValue(config, 100);
            } else if (config.getPropertyName().equals("MEASURES")) {
                measurements = getValue(config, true);
            } else if (config.getPropertyName().equals("TRACE")) {
                traceEnabled = getValue(config, true);
            } else {
                error("Property " + config.getPropertyName() + " not recognised");
            }
        }
        config.changeMatch("AR");

        while (config.readSection() && config.getSectionName().equals("ARSTREAM")) {
            setStream(config);
        }
    }
    /*
     * Only called from getProcess, which applies a sychonization lock.
     */
    private Process(String identifier) {
        if (!initialised) {
            initialise();
        }
        /*
         * Set default streams and configuration.
         */
        try {
            this.identifier = identifier;
            setStream("TRACE", "Trace%d%b.log", "%H:%M:%S !ID! !MOD+c !!REF+c !");
            setStream("ERROR", "Error%d%b.log", "%H:%M:%S !ID! !MOD+c !!REF+c !");
            setStream("EVENT", "Event%d%b.log", "%H:%M:%S !ID! !REF+c !");
            setStream("AUDIT", "Audit%d%b.log", "%H:%M:%S !ID! !REF+c !");
            setStream("COMMENT", "Comment%d%b.log", "%H:%M:%S !ID+c !");
            setStream("MEASUREMENT", "Measurement%d%b.log", "%H:%M:%S !ID+c !");

            configure("ARGLOBAL", null);
            configure("ARJDEFAULT", null);
            configure("ARIDENTIFIER", identifier);
            trace.setMask(new Token(defaultTrace));
        } catch (Configuration.ConfigurationError e) {
            System.out.println(e.toString());
        }
    }

    public boolean getMeasure() {
        return measurements;
    }

    public String getErrorEventId() {
        return errorEventId;
    }
    /*
     * Adds Key and Value to duplicates list. Returns true if the list already
     * contains an entry for Key and Value.
     */
    public boolean setDuplicate(String key, String value) {
        synchronized (this) {
            String listValue = duplicates.get(key);

            if (listValue != null && value.equals(listValue)) {
                return true;
            }

            if (duplicates.size() >= maxDuplicates) {
                duplicates.clear();
            }

            duplicates.put(key, value);
            return false;
        }
    }
    /*
     * Clears the duplicates if Key is null, otherwise removes entry with Key.
     * Note: It is not an error if an entry with Key does not exist.
     */
    public void clearDuplicates(String key) {
        synchronized (this) {
            if (key == null) {
                duplicates.clear();
            } else {
                duplicates.remove(key);
            }
        }
    }
    /*
     * Updates trace level and flags.
     *
     * If Mask does not contain a level specifier it remains unchanged.
     * If Mask does not contain flags they are set to default defined at
     * initialisation.
     */
    private void setTrace(Token mask, char delimiter) {
        switch (delimiter) {
            case '*':
                trace.updateGroups(mask);
                break;
            case '$':
                trace.updateModules(mask);
                break;
            default: {
                int level = TraceMask.getLevel(mask);
                if (level != -1) {
                    maxTrace = level;
                }
                trace.setMask((mask.remainder().equals("")) ? new Token(defaultTrace) : mask);
            }
        }
    }
    public String getIdentifier() {
        return identifier;
    }
    public static Collection<Process> getProcesses() {
        return processes.values();
    }
    public int getMaxTraceLevel() {
        return maxTrace;
    }
    /**
     * Returns true if trace is unconditionally turned off, i.e. cannot be
     * controlled at run time.
     */
    public boolean noTrace() {
        return !traceEnabled;
    }
    /**
     * 
     * @return True if any of the trace flags are set.
     */
    public boolean isTraceEnabled () {
        return !noTrace && trace.isEnabled();
    }
    /*
     * The methods that call updateTrace should wait until the traceLock clears. However, all this means is
     * that the latest settings of the trace attributes may not be picked up until the next refresh interval.
     */
    public boolean isGroupEnabled(String name) {
        updateTrace(false);
        return trace.isGroupEnabled(name);
    }
    public boolean isModuleEnabled(String name) {
        updateTrace(false);
        return trace.isModuleEnabled(name);
    }
    public boolean isTraceEnabled(char type) {
        updateTrace(false);
        return traceEnabled && trace.isEnabled(type);
    }
    public Collection<Stream> getStreams() {
        return streams.values();
    }
    public Stream getStream(String name) {
        return streams.get(name);
    }
    public Stream.Summary getStreamSummary(String name) {
        return getStream(name).getSummary() ;
    }
    public HashMap<String, Stream.Summary> getStreamSummaries() {
        HashMap<String, Stream.Summary> summaries = new HashMap<>();
        
        streams.entrySet().forEach((entry) -> {             
            summaries.put(entry.getKey(), entry.getValue().getSummary());
        }); 
        return summaries;
    }
    /**
     * @return the timestamp of the last
     */
    public Date getUpdated() {
        return updated;
    }
    public class Stream {

        /**
         * @return the entryCount
         */
        public int getEntryCount() {
            return entryCount;
        }
        private String      name               = "";
        private String      fileTemplate        = "";
        private String      reportPrefix        = "";
        private Interceptor interceptor         = null;
        private boolean     interceptorOverride = false;
        private boolean     error               = false;
        private Object      control             = null;
        private OutputFile  file                = null;
        private Interval    refresh             = null;
        private String      fileName            = "";
        private int         entryCount          = 0;
        private boolean     allowReenter        = false;

        public class Summary {            
            public String getName() {
                return name;
            }
            public boolean getAllowReenter() {
                return allowReenter;
            }
            public String getFileName() {
                return fileName;
            }
            public String getFullFilename() {
                return file == null? null : file.getFilename();
            }
            public String getFileTemplate() {
                return fileTemplate;
            }
            public String getReportPrefix() {
                return reportPrefix;
            }
        }
        Stream(String name) {
            this.name    = name;
            fileTemplate = "";
            reportPrefix = "";
            refresh      = new Interval(1000 * fileRefreshRate, true);
        }

        public void setInterceptor(Interceptor interceptor, String openInfo, boolean override) {
            this.interceptor = interceptor;
            try {
                control = this.interceptor.open(name, openInfo);
                interceptorOverride = override;
            } catch (InterceptorException e) {
                error(e.toString() + " setting Interceptor for stream " + name);
                this.interceptor = null;
            }
        }

        public String getName() {
            return name;
        }
        public void setFileTemplate(String fileTemplate) {
            this.fileTemplate = fileTemplate;
        }
        public String getFileTemplate() {
            return fileTemplate;
        }
        public void setReportPrefix(String reportPrefix) {
            this.reportPrefix = reportPrefix;
        }
        public void setAllowReenter(boolean yes) {
            allowReenter = yes;
        }
        public String getReportPrefix() {
            return reportPrefix;
        }
        private void output(PrintWriter stream, String message, Exception exception, boolean stackTrace) {
            if (exception != null && !stackTrace) message += " exception " + exception.getClass().getSimpleName() + "-" + exception.getMessage();
            
            if (stream == null)
                System.err.println(message);
            else
                stream.println(message);
            
            if (exception != null && stackTrace)
                if (stream == null) exception.printStackTrace(); else exception.printStackTrace(stream);
        }
        private String resolveLocalParameters(String ref, String module, String text, Parameters params) {
            Parameters lParams = new Parameters();
            
            if (ref    != null) lParams.setValue("REF", ref);
            if (module != null) lParams.setValue("MOD", module);
            
            return params.substitute(text, lParams);
        }
        public String reportText(String ref, String module, String text, Parameters params) {            
            return resolveLocalParameters(ref, module, reportPrefix, params) + resolveLocalParameters(ref, module, text, params);
        }
        public void output(String ref, String module, String text, Parameters params, String duplicateKey, Exception exception, boolean stackTrace) {
            boolean    interceptorActioned = false;
            String     formattedPrefix;
            String     formattedText;
            boolean    toFile              = (interceptor == null || interceptorOverride);
            String     fName               = getFileName();
            boolean    duplicate           = false;
            boolean    check               = refresh.lapsed();
            
            if (file == null || check) {
                fName = params.substitute(fileTemplate);
            }
            if (!fName.equals(fileName) || (error && check)) {
                try {
                    if (file != null) {
                        file.close();
                    }
                    file  = OutputFile.open(reportingRoot, fName, true);
                    error = false;
                } catch (IOException e) {
                    if (!error) {
                        System.out.println("Opening file "
                                + fName + " exception "
                                + e.toString());
                    }
                    file  = null;
                    error = true;
                }
            }
            fileName        = fName;
            formattedPrefix = resolveLocalParameters(ref, module, reportPrefix, params);
            formattedText   = resolveLocalParameters(ref, module, text, params);
            String message  = formattedPrefix + text;

            if (duplicateKey != null) {
                duplicate = setDuplicate(name + duplicateKey, formattedText);
            }

            if (interceptor != null && (allowReenter || getEntryCount() == 0)) {
                try {
                    entryCount          += 1;
                    interceptorActioned = interceptor.output(control, getEntryCount() != 1, text, duplicateKey);
                } catch (InterceptorException e) {
                    System.err.println("On stream " + name + " Interceptor error " + e.toString());
                    toFile = true;
                } finally {
                    entryCount -= 1;
                }
            }
            if ((toFile && !duplicate) || !interceptorActioned) {
                if (file != null) {
                    PrintWriter ps = file.getOut();
                    
                    output(ps, message, exception, stackTrace);                    
                    ps.flush();
                } else if (this.name.equals("ERROR")) {                    
                    output(null, message, exception, stackTrace);
                } else {
                    System.out.println(message);
                }
            }
        }
        public void output(String ref, String module, String text, Parameters params, String duplicateKey, Exception exception) {
            output(ref, module, text, params, duplicateKey, exception, true);
        }
        public void output(String ref, String module, String text, Parameters params, String duplicateKey) {
            output(ref, module, text, params, duplicateKey, null);
        }
        public void close() {
            if (file == null) {
                return;
            }
            file.close();
        }
        public Summary getSummary() {
            return new Summary();
        }

        /**
         * @return the fileName
         */
        public String getFileName() {
            return fileName;
        }
    }
}