/*
 * DelayedPair.java
 *
 * Created on March 13, 2007, 6:06 PM
 *
 */

package expect4j;

import java.io.*;
import java.util.logging.*;

/**
 * Fake the processing of stream, by adding delays.
 * 
 * @author justin
 */
public class DelayedPair implements IOPair {
    static final public Logger log = Logger.getLogger(DelayedPair.class.getName());
    
    Reader is;
    StringWriter os;
    Thread delayedWriter = null;
    boolean ended = false;
    boolean ending = false;
    
    public DelayedPair(final String baseStr, final int delay, final int endDelay) throws Exception {
        final PipedWriter writer = new PipedWriter();
        is = new PipedReader( writer );
        
        final String parts[] = baseStr.split(" ");
        
        delayedWriter = new Thread() {
            public void run() {
                log.fine("Running Delayed Writer");
                for( int i=0; !ending && i < parts.length; i++) {
                    try {
                        Thread.sleep( delay );
                        log.fine("Writing: <" + parts[i] + ">");
                        writer.write(parts[i]);
                        if( i != parts.length - 1 )
                            writer.write(" ");
			writer.flush();
                    } catch(Exception e) {
			log.warning(e.getMessage());
                    }
                }
                
                if( !ending ) {
                    try {
                        Thread.sleep(endDelay * 1000);
                    }catch(Exception e) {
                    }
                }
                
                try {
		    writer.close();
		    is.close();
                    os.close();
                }catch(Exception e) {
		    log.warning(e.getMessage());
                }
                log.fine("Sending EOF");
                ending = true;
                ended = true;
            }
        };
        delayedWriter.start();
        
        os = new StringWriter();
    }
    
    public Reader getReader() {
        return (ending)?null:is;
    }
    
    public Writer getWriter() { return os; }
    
    public String getResult() {
        os.flush();
        return os.getBuffer().toString();
    }
    
    /**
     * TODO evaluate if this is even needed
     */
    public void reset() {
        try {
            is.reset();
        }catch(IOException ioe) {
        }
    }
    public void close() {
        try { os.close(); } catch(Exception e) { }
        
        ending = true;
        delayedWriter.interrupt(); // thread will close is
        try { delayedWriter.join(1000); } catch(Exception e) { }
    }
    
}
