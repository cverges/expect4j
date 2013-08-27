/*
 * PollingConsumerTest.java
 * JUnit based test
 *
 * Created on January 16, 2007, 11:54 PM
 */

package expect4j;

import junit.framework.*;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

/**
 *
 * @author justin
 */
public class PollingConsumerTest extends TestCase {
    
    public PollingConsumerTest(String testName) {
        super(testName);
    }
    
    StringPair pair;
    Consumer consumer;
    Thread consumerThread;
    protected void setUp() throws Exception {
        pair = new StringPair("The lazy fox");
        consumer = new PollingConsumer(pair);
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
	System.out.println("testRead");
        consumerThread.start();
        try { Thread.sleep(500); }catch(Exception e) { }
        
        // should be available by now
        String result = consumer.pause();
        
        consumer.stop();
        
	System.out.println(result);
        assertEquals("The lazy fox", result);
    }
    
    public void testMatch() {
        Expect4j.log.entering(getClass().getName(), "testMatch");
        
        consumerThread.start();
        
        consumer.waitForBuffer(500);
        
        String result = consumer.pause();
        assertEquals("The lazy fox", result);
        
        consumer.resume(4);
        
        result = consumer.pause();        
        assertEquals("azy fox", result);
        
        consumer.stop();
        
        Expect4j.log.exiting(getClass().getName(), "testMatch");
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
