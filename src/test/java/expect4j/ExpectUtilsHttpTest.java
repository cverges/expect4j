/*
 * ExpectUtilsHttpTest.java
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
public class ExpectUtilsHttpTest extends TestCase {
    
    public ExpectUtilsHttpTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
    /**
     * Test of Http method, of class expect4j.ExpectUtils.
     */
    public void testHttp() throws Exception {
        System.out.println("Http");
        
        System.setProperty("expect4j.level", "400");
        java.util.logging.LogManager.getLogManager().readConfiguration();
 
        String remotehost = "seas.harvard.edu";
        String url = "/";
        String expResult = "Harvard School of Engineering and Applied Sciences";
 
        String result = ExpectUtils.Http(remotehost, url);
 
        assertNotNull(result);
 
        assertTrue( result.indexOf(expResult) != -1 );
 
    }
}
