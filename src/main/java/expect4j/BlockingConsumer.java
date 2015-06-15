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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for absorbing everything from stream and to maintain a
 * buffer.  This subclass of {@link Consumer} blocks while performing
 * these operations and needs to be asynchronously managed.
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class BlockingConsumer extends ConsumerImpl {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(BlockingConsumer.class);

    /**
     * TODO
     */
    Boolean callerProcessing = Boolean.FALSE;

    /**
     * TODO
     */
    boolean foundMore = false;

    /**
     * Creates a <code>BlockingConsumer</code> instance based on an
     * {@link IOPair} concrete instance.
     *
     * @param pair the <code>IOPair</code> concrete instance that
     *             provides access to the reader/writer streams
     */
    public BlockingConsumer(IOPair pair) {
        super(pair);
        logger.trace("Created new BlockingConsumer instance " + this + "using IOPair " + pair);
    }

    /**
     * Starts the <code>BlockingConsumer</code> in processing the reader
     * stream.
     * <p>
     * TODO: Handle timeout of zero to expect, that shouldn't wait
     */
    public void run() {
        logger.trace("BlockingConsumer " + this + " starting data processing");

        int length;
        char cs[] = new char[256];
        Reader reader = pair.getReader();

        while (!stopRequested && !foundEOF) {
            try {
                logger.trace("BlockingConsumer " + this + " reading from reader");
                length = reader.read(cs); // blocking
            } catch (IOException ioe) {
                // The reader most likely closed on us.
                logger.warn("Caught an exception while reading: " + ioe);
                logger.debug("Assuming EOF");
                foundEOF = true;
                break;
            }

            if (length == -1) { //EOF
                logger.debug("BlockingConsumer " + this + " detected EOF");
                logger.trace("Remaining buffer: " + buffer.toString());
                foundEOF = true;
                break;
            }

            // don't modify the buffer while processing is happening
            // written as while loop to prevent spurious interrupts
            logger.trace("BlockingConsumer " + this + " waiting for synchronized access before appending");
            synchronized(this) { // this could be just before notify, I think
                if (logger.isDebugEnabled()) {
                    logger.debug("BlockingConsumer " + this + " appending " + length + " characters to the buffer");

                    // TODO: this replace operation is done a few places, perhaps utility class?
                    String print = new String(cs, 0, length);
                    print = print.replaceAll("\n", "\\\\n");
                    print = print.replaceAll("\r", "\\\\r");
                    logger.debug("Adding to buffer: " + print);

                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < length; i++)
                        sb.append("," + ((int) cs[i]) );
                    logger.trace("Codes: " + sb.toString());
                }
                buffer.append(cs, 0, length); // thread safe

                /**
                 * TODO Trim down buffer.  This current method won't work if a resume comes in, since it's offset
                 * will be invalid once this delete method runs
                 *
                 * // since we only added one char, we should only have to remove one
                 * if (buffer.length() > BUFFERMAX)
                 *     buffer.delete(0, BUFFERMAX - buffer.length());
                 */

                logger.trace("BlockingConsumer " + this + " notifying listeners of buffer change");
                notify(); // seeing that we read something, wake people up

                // We explicitly call this after appending to the
                // buffer, just in case one of the BufferChangeLoggers
                // accidentally modifies the character buffer.
                notifyBufferChange(cs, length);
            } // end synchronized(this)
        } // end while loop

        logger.trace("BlockingConsumer " + this + " notifying listeners upon ceasing");
        synchronized(this) {
            notify();
        }

        if (stopRequested) {
            logger.debug("BlockingConsumer " + this + " stop requested");
            pair.close();
        }

        if (foundEOF)
            logger.debug("BlockingConsumer " + this + " found EOF to stop while loop");
    }

    /**
     * TODO: what if something came in between when we last checked and
     *       when this method is called
     *
     * @param timeout timeout in milliseconds
     */
    public void waitForBuffer(long timeout) {
        if (foundEOF) {
            logger.trace("BlockingConsumer " + this + " wanted to wait for buffer but found EOF");
            return;
        }

        logger.trace("BlockingConsumer " + this + " ynching on this to wait");
        logger.trace("BlockingConsumer " + this + " waiting for synchronized access before waiting");
        synchronized(this) {
            try {
                if (timeout > 0) {
                    logger.trace("BlockingConsumer " + this + " waiting for " + timeout + " msec for some additional event");
                    wait(timeout);
                } else {
                    logger.trace("BlockingConsumer " + this + " waiting forever for some additional event");
                    wait();
                }
            } catch (InterruptedException ie) {
                logger.trace("BlockingConsumer " + this + " woken up while waiting for buffer");
            }
        }
    }

    /**
     * TODO
     *
     * @return TODO
     */
    public String pause() {
        // TODO mark offset, so that it can be trimmed by resume coming in later
        String currentBuffer;
        currentBuffer = buffer.toString();
        return currentBuffer;
    }

    /**
     * Resume processing from the specified offset.
     *
     * @param offset the offset into the buffer (1-indexed) from which
     *               to start
     */
    public void resume(int offset) {
        if (offset < 0)
            return;

        synchronized(this) {
            logger.trace("BlockingConsumer " + this + " moving buffer up by " + offset);
            StringBuffer smaller = buffer.delete(0, offset); // + 1
        }
    }

    /**
     * A method to test the <code>BlockingConsumer</code> class independently.
     *
     * @param args command line arguments (none used)
     */
    public static void main(String args[]) throws Exception {
        final StringBuffer buffer = new StringBuffer("The lazy fox");

        Thread t1 = new Thread() {
            public void run() {
                synchronized(buffer) {
                    buffer.delete(0,4);
                    buffer.append(" in the middle");
                    System.err.println("Middle");
                    try {
                        Thread.sleep(4000);
                    } catch (Exception e) {}
                    buffer.append(" of fall");
                    System.err.println("Fall");
                }
            }
        };

        Thread t2 = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
                buffer.append(" jump over the fence");
                System.err.println("Fence");
            }
        };

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.err.println(buffer);
    }
}
