/*
 * StringPairTest.java
 * JUnit based test
 *
 * Created on March 7, 2007, 11:42 PM
 */

package expect4j;

import junit.framework.*;
import java.io.*;
import tcl.lang.Interp;

/**
 *
 * @author justin
 */
public class ExpectEmulationTest extends TestCase {
    
    public ExpectEmulationTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Load interpreter
     * 
     * TODO Figure out a system for testing.
     */
    public void testInterp() throws IOException {
        Interp interp = new Interp();
        
    }
    
}
