/*
 * TimeoutMatchTest.java
 * JUnit based test
 *
 * Created on December 30, 2006, 4:41 PM
 */

package expect4j.matches;

import junit.framework.*;
import expect4j.*;

/**
 *
 * @author justin
 */
public class TimeoutMatchTest extends TestCase {
    
    public TimeoutMatchTest(String testName) {
        super(testName);
    }

    Closure closure;
    public void setUp() {
        closure = new Closure() {
            public void run(ExpectState state) throws Exception {
                
            }
        };
    }
    /**
     * Test of getTimeout method, of class expect4j.matches.TimeoutMatch.
     */
    public void testGetTimeout() {
        System.out.println("getTimeout");
        
        TimeoutMatch instance = null;
        long result;
        
        // Default
        instance = new TimeoutMatch(closure);
        result = instance.getTimeout();
        assertEquals(Expect4j.TIMEOUT_NOTSET, result);
        
        // Custom
        long expected = 555L;
        instance = new TimeoutMatch(expected, closure);
        result = instance.getTimeout();
        assertEquals(expected, result);
    }
}
