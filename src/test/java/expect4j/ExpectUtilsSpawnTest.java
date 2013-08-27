/*
 * ExpectUtilsTest.java
 * JUnit based test
 *
 * Created on November 26, 2006, 8:49 PM
 */

package expect4j;

import junit.framework.*;
import expect4j.matches.*;
import java.util.*;

import java.util.logging.*;

/**
 *
 * @author justin
 */
public class ExpectUtilsSpawnTest extends TestCase {
    
    public ExpectUtilsSpawnTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
    /**
     * Test of spawn method, of class expect4j.ExpectUtils.
     */
    public void testSpawn() throws Exception {
        System.out.println("spawn");
     
        String executable = "cmd /c net statistics Workstation";
        String expResult = "Workstation Statistics";
        Expect4j expect = ExpectUtils.spawn(executable);
        expect.setDefaultTimeout( 5000 );
     
        // Don't have to do anything, since the match will be saved
        int index = expect.expect( "Workstation Statistics");
     
        assertEquals(0, index);
     
        String result = expect.getLastState().getMatch();
     
        assertEquals(expResult, result);
     
        final StringBuffer receivedStr = new StringBuffer();
        final StringBuffer transmittedStr = new StringBuffer();
     
        ArrayList matches = new ArrayList();
        matches.add( new RegExpMatch("Bytes received\\s*(\\d+)\\r\\n", new Closure() {
            public void run(ExpectState state) {
                receivedStr.append( state.getMatch(1) );
                state.exp_continue();
            }
        }) );
        matches.add( new RegExpMatch("Bytes transmitted\\s*(\\d+)\\r\\n", new Closure() {
            public void run(ExpectState state) {
                transmittedStr.append( state.getMatch(1) );
                state.exp_continue();
            }
        }) );
        index = expect.expect( matches );
     
        System.out.println( receivedStr );
        long received = 0;
        try {
            received = Long.valueOf( receivedStr.toString() ).longValue();
        } catch(Exception e) {
            Expect4j.log.log(Level.FINE, "Received", e);
        }
        assertTrue( received > 0 );
     
        System.out.println( transmittedStr );
        long transmitted = 0;
        try {
            transmitted = Long.valueOf( transmittedStr.toString() ).longValue();
        } catch(Exception e) {
            Expect4j.log.log(Level.FINE, "Received", e);
        }
        assertTrue( transmitted > 0 );
    }
}
