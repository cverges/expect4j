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
import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class BlockingConsumerTest extends TestCase {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(BlockingConsumerTest.class);

    public BlockingConsumerTest(String testName) {
        super(testName);
    }

    StringPair pair;
    Consumer consumer;
    Thread consumerThread;
    protected void setUp() throws Exception {
        pair = new StringPair("The lazy fox");
        consumer = new BlockingConsumer(pair);
        consumerThread = new Thread(consumer);
    }

    protected void tearDown() throws Exception {
    }

    /**
	 * Test of run method, of class expect4j.PollingConsumer.
	 */
    public void testRun() {
        System.out.println("run");

        consumerThread.start();
        consumer.stop();

        boolean ableToJoin = false;
        try {
            consumerThread.join(1000l);
            ableToJoin = true;
        }catch(InterruptedException e) {
        }

        assertTrue(ableToJoin);
    }

    public void testRead() {
        final StringBuffer changeBuffer = new StringBuffer();

        BufferChangeLogger changeLogger = new BufferChangeLogger() {
            public void bufferChanged(char[] newData, int numChars) {
                changeBuffer.append(newData, 0, numChars);
            }
        };

        consumer.registerBufferChangeLogger(changeLogger);

        consumerThread.start();
        try { Thread.sleep(500); }catch(Exception e) { }

        // should be available by now
        String result = consumer.pause();

        consumer.stop();

        assertEquals("The lazy fox", result);
        assertEquals("The lazy fox", changeBuffer.toString());
    }

    public void testMatch() {
        logger.info("Entering " + getClass().getName() + ".testMatch");

        consumerThread.start();

        consumer.waitForBuffer(500);

        String result = consumer.pause();
        assertEquals("The lazy fox", result);

        consumer.resume(5);

        result = consumer.pause();
        assertEquals("azy fox", result);

        consumer.stop();

        logger.info("Exiting " + getClass().getName() + ".testMatch");
    }

    public void testWrite() throws IOException {
        consumerThread.start();

        consumer.send("Writing");

        consumer.stop();

        String result = pair.getResult();

        assertEquals("Writing", result);
    }

    public void testWait() throws IOException {
        System.out.println("run");

        consumerThread.start();

        consumer.waitForBuffer(1000L);

        consumer.stop();

    }
}
