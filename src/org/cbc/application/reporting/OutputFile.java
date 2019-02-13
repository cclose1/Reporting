package org.cbc.application.reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;

/**
 * @version <b>v1.0, 11/Jun/01, C.B. Close:</b> Initial version.
 * @version <b>v1.1, 11/Jul/01, C.B. Close:</b> Implement Serializable.
 */
public class OutputFile implements Serializable {

    private static transient       HashMap<File, OutputFile> files = new HashMap<File, OutputFile>();
    private static transient final Object                    lock  = new Object();
    public  transient              PrintWriter               out   = null;
    private transient              int                       opens = 0;
    private                        File                      file  = null;

    private OutputFile(File file) throws IOException {
        out   = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
        this.file = file;
    }
    public static OutputFile open(File file, boolean share) throws IOException {
        OutputFile out;

        if (share) {
            synchronized(lock) {
                out = files.get(file);
                
                if (out == null) {
                    out = new OutputFile(file);
                    files.put(file, out);
                } 
                out.opens += 1;
            }
        } else {
            out       = new OutputFile(file);
            out.opens = -1;
        }
        return out;
    }

    public static OutputFile open(String file, boolean share) throws IOException {
        return open(new File(file), share);
    }

    public static OutputFile open(String path, String file, boolean share) throws IOException {
        
        return open(new File(path, file), share);
    }

    public String getFilename() {
        return file == null? null : file.getAbsolutePath();
    }

    public void close() {
        if (opens == -1) {
            out.close();
            opens = 0;
        }
        if (opens == 0) {
            return;
        }
        opens -= 1;

        if (opens == 0) {
            synchronized (lock) {
                out.close();
                files.remove(file);
            }
        }
    }
    /**
     * @return the out
     */
    public PrintWriter getOut() {
        return out;
    }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        /*
         * Shared files are not synchronized, so open file as not shared.
         */
        if (file != null) {
            out   = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
        }
    }
}
