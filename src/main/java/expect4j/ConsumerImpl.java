/*
 * Copyright (c) 2007 Justin Ryan
 * Copyright (c) 2013 Chris Verges <chris.verges@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package expect4j;

import java.io.Writer;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author justin
 */
public abstract class ConsumerImpl implements Consumer {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConsumerImpl.class);
    
    /**
     * The maximum size of the buffer, currently set to 16 KB.
     */
    public static final int BUFFERMAX = 16 * 1024;
    
    /**
     * A buffer containing the unprocessed data received over the reader
     * stream.
     */
    StringBuffer buffer;
    
    /**
     * The reader and writer streams being managed by this
     * <code>Consumer</code>.
     */
    IOPair pair;

    /**
     * A flag to indicate whether processing should continue.
     */
    boolean stopRequested = false;

    /**
     * A flag to indicate whether the EOF marker was detected on the
     * reader stream.
     */
    boolean foundEOF = false;
    
    /**
     * Creates a <code>ConsumerImpl</code> instance based on an
     * {@link IOPair} concrete instance.
     *
     * @param pair the <code>IOPair</code> concrete instance that
     *             provides access to the reader/writer streams
     */
    public ConsumerImpl(IOPair pair) {
        this.pair = pair;
        buffer = new StringBuffer();
    }
    
    /**
     * Passes data to the writer stream for remote execution from the
     * <code>Consumer</code>.  I'm not really sure why this is useful or
     * desired, but the ability exists.  Use with caution.
     *
     * @param data the data to pass to the writer
     * @throws IOException if an error occurs with the writer stream
     * @see Expect4j#send(String)
     */
    public void send(String data) throws IOException {
        String printStr = null;
        if (logger.isDebugEnabled()) {
            printStr = data;
            printStr = printStr.replaceAll("\\r", "\\\\r");
            printStr = printStr.replaceAll("\\n", "\\\\n");
        }

        synchronized(this) {
            logger.debug("Sending to writer: " + printStr);
            Writer writer = pair.getWriter();
            writer.write(data);
            writer.flush();
        }
    }
    
    /**
     * Resume processing from the beginning of the buffer.
     */
    public void resume() {
        resume(-1);
    }
    
    /**
     * Requests the <code>Consumer</code> to stop processing data at its
     * next convenient time.
     */
    public void stop() {
        logger.trace("Requesting the Consumer to stop processing data");
        stopRequested = true;
    }

    /**
     * Returns a boolean flag on whether the EOF marker has been detected.
     *
     * @return <code>true</code> if EOF has been detected,
     *         <code>false</code> otherwise
     */
    public boolean foundEOF() {
        return foundEOF;
    }
}
