/*
 * Copyright 2007 Justin Ryan
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package expect4j;

import java.util.logging.*;
import java.io.Writer;
import java.io.IOException;

/**
 *
 * @author justin
 */
public abstract class ConsumerImpl implements Consumer {
    static final public Logger log = Logger.getLogger(ConsumerImpl.class.getName());
    
    public static final int BUFFERMAX = 16 * 1024;
    
    StringBuffer buffer;
    
    IOPair pair;
    boolean stopRequested = false;
    boolean foundEOF = false;
    
    /** Creates a new instance of ConsumerImpl */
    public ConsumerImpl(IOPair pair) {
        this.pair = pair;
        buffer = new StringBuffer();
    }
    
    /**
     * A few easy functions that a generic Consumer can do for its children
     */
    // TODO add some synch
    public void send(String str) throws IOException {
        String printStr = str;
        printStr = printStr.replaceAll("\\r", "\\\\r");
        printStr = printStr.replaceAll("\\n", "\\\\n");
        
        log.fine("Sending: >>>" + printStr + "<<<");
        Writer writer = pair.getWriter();
        writer.write( str );
        writer.flush();
    }
    
    public void resume() {
        resume(-1);
    }
    
    public void stop() {
        log.fine("Requesting stop");
        stopRequested = true;
    }
    
    public boolean foundEOF() {
        return foundEOF;
    }
}
