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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for absorbing everything from stream and to maintain a buffer.
 *
 * TODO Rewrite with NIO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class PollingConsumer extends ConsumerImpl {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(PollingConsumer.class);

    boolean dirtyBuffer;
    Boolean callerProcessing = Boolean.FALSE;

    boolean foundMore = false;

    public PollingConsumer(IOPair pair) {
        super(pair);
        dirtyBuffer = false;
    }

    /**
     * TODO Handle timeout of zero to expect, that shouldn't wait
     */
    public void run() {
        int length;
        char cs[] = new char[256];
        int ioErrorCount = 0;
        Reader reader = pair.getReader();

        logger.trace("Starting primary loop");
        while ( !stopRequested && !foundEOF && ioErrorCount < 4) {
            try {
                logger.trace("Checking ready");
                boolean ready = false;
                try {
                    ready = reader.ready(); // will not block
                    if( reader.markSupported() ) logger.trace("Mark Supported");
                } catch(Exception ioe) {
                    // The Pipe most likely closed on us.
                    logger.debug("Exception in PollingConsumer: " + ioe);
                    foundEOF = true;
                    break;
                }

                //if( !ready ) { ready = true; log.fine("Faking ready"); }

                if ( ready ) {
                    logger.trace("Is Ready");


                    // don't modify the buffer while processing is happening
                    // written as while loop to prevent spurious interrupts
                    synchronized(this) {
                        while( callerProcessing.booleanValue() ) {
                            logger.trace("Waiting for caller to finish");
                            try {
                                wait();
                            } catch(InterruptedException ie) {
                                logger.trace("Woken up early");
                                continue;
                            }
                        }

                        logger.trace("About to wait for buffer lock");
                        synchronized(buffer) {
                            length = reader.read(cs);

                            if( length == -1 ) { //EOF
                                logger.trace("Found the EOF");
                                logger.trace("Current buffer: " + buffer.toString() );
                                foundEOF = true;
                                dirtyBuffer = true;
                                break;
                            }

                            String print = new String(cs, 0, length);
                            print = print.replaceAll("\n", "\\\\n");
                            print = print.replaceAll("\r", "\\\\r");
                            logger.trace("Appending >>>" + print + "<<<");
                            buffer.append( cs, 0, length ); // thread safe

                            logger.trace("Current Buffer: " + buffer.toString() );
                            dirtyBuffer = true;

                            /**
                             * TODO Trim down buffer.  This current method won't work if a resume comes in, since it's offset
                             * will be invalid once this delete method runs
                             *
                             * // since we only added one char, we should only have to remove one
                             * if( buffer.length() > BUFFERMAX )
                             * buffer.delete(0, BUFFERMAX - buffer.length() );
                             */

                            logger.trace("Waking up who ever if listening");
                            buffer.notify(); // seeing that we read something, wait people up

                            // We explicitly call this after appending to the
                            // buffer, just in case one of the BufferChangeLoggers
                            // accidentally modifies the character buffer.
                            notifyBufferChange(cs, length);
                        }
                    }

                } else {
                    logger.trace("Not Ready, sleeping");
                    try { Thread.sleep(500); } catch(InterruptedException ie) { }
                    logger.trace("Done sleeping");
                    //continue;
                }
            }catch(IOException ioe) {
                logger.warn("Exception in loop body: " + ioe);
                ioErrorCount++;
                //continue;
            }
        } // end while loop


        synchronized(buffer) {
            buffer.notify();
        }
        if( stopRequested ) {
            logger.debug("Stop Requested");
            pair.close();
        }
        if ( foundEOF )
            logger.debug("Found EOF to stop while loop");
        if( ioErrorCount >= 4 )
            logger.debug("ioErrorCount at " + ioErrorCount );
        logger.trace("Leaving primary loop");
    }

    /**
     * What is something came in between when we last checked and when this method is called
     */
    public void waitForBuffer(long timeoutMilli) {
        //assert(callerProcessing.booleanValue() == false);

        synchronized(buffer) {
            if( dirtyBuffer )
                return;
            if( !foundEOF() ) {
                logger.trace("Waiting for things to come in, or until timeout");
                try {
                    if( timeoutMilli > 0 )
                        buffer.wait(timeoutMilli);
                    else
                        buffer.wait();
                } catch(InterruptedException ie) {
                    logger.trace("Woken up, while waiting for buffer");
                }
                // this might went early, but running the processing again isn't a big deal
                logger.trace("Waited");
            }
        }
    }

    public String pause() {

        return pause(Integer.MAX_VALUE);
    }

    public String pause(int maxLength) {
        String currentBuffer;
        synchronized(this) { // stop consumer from continuing
            currentBuffer = buffer.substring(0, Math.min(maxLength, buffer.length()));
            dirtyBuffer = false;
            callerProcessing = Boolean.TRUE;
        }
        return currentBuffer;
    }

    public void resume(int offset) {

        synchronized(this) {
            // if pause was called, then the main loop should be blocked callerProcessing,
            // and the buffer is safe.
            if( offset >= 0 ) {
                logger.trace("Moving buffer up by " + offset);
                StringBuffer smaller = buffer.delete(0, offset + 1);
                logger.trace("New size: " + buffer.length() + " vs " + smaller.length() );
            }

            callerProcessing = Boolean.FALSE; // should allow consumer to continue
            notify();
        }
    }

    /**
     * We have more input since wait started
     */

    public static void main(String args[]) throws Exception {
        final StringBuffer buffer = new StringBuffer("The lazy fox");
        Thread t1 = new Thread() {
            public void run() {
                synchronized(buffer) {
                    buffer.delete(0,4);
                    buffer.append(" in the middle");
                    System.err.println("Middle");
                    try { Thread.sleep(4000); } catch(Exception e) {}
                    buffer.append(" of fall");
                    System.err.println("Fall");
                }
            }
        };
        Thread t2 = new Thread() {
            public void run() {
                try { Thread.sleep(1000); } catch(Exception e) {}
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
