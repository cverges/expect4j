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

import java.io.*;
import java.util.logging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fake the processing of stream, by adding delays.
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class DelayedPair implements IOPair {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(DelayedPair.class);

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
                logger.debug("Running Delayed Writer");
                for (int i = 0; !ending && i < parts.length; i++) {
                    try {
                        Thread.sleep( delay );
                        logger.debug("Writing: <" + parts[i] + ">");
                        writer.write(parts[i]);
                        if (i != (parts.length - 1))
                            writer.write(" ");
                        writer.flush();
                    } catch(Exception e) {
                        logger.warn(e.getMessage());
                    }
                }

                if (!ending) {
                    try {
                        Thread.sleep(endDelay * 1000);
                    } catch (Exception e) {}
                }

                try {
                    writer.close();
                    is.close();
                    os.close();
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }
                logger.debug("Sending EOF");
                ending = true;
                ended = true;
            }
        };
        delayedWriter.start();

        os = new StringWriter();
    }

    public Reader getReader() {
        return (ending) ? null : is;
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
