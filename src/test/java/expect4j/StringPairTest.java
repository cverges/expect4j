/*
 * StringPairTest.java
 * JUnit based test
 *
 * Created on March 7, 2007, 11:42 PM
 */

package expect4j;

import junit.framework.*;
import java.io.*;

/**
 *
 * @author justin
 */
public class StringPairTest extends TestCase {
    
    public StringPairTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Test of getReader method, of class expect4j.StringPair.
     */
    public void testGetReader() throws IOException {
        System.out.println("getReader");
        
        String expResult = "The lazy fox";        
        StringPair instance = new StringPair(expResult);
        
        Reader result = instance.getReader();
        assertNotNull( result );        
    }

    /**
     * Test of getWriter method, of class expect4j.StringPair.
     */
    public void testGetWriter() {
        System.out.println("getWriter");
        
        StringPair instance = new StringPair("The lazy fox");
        
        Writer result = instance.getWriter();
        assertTrue( (result instanceof StringWriter) );        
    }

    /**
     * Test of getResult method, of class expect4j.StringPair.
     */
    public void testGetResult() throws IOException {
        System.out.println("getResult");
        
        StringPair instance = new StringPair("The lazy fox");
        instance.getWriter().write("Awake Chicken");
        
        String expResult = "Awake Chicken";
        String result = instance.getResult();
        assertEquals(expResult, result);        
    }

    /**
     * Test of reset method, of class expect4j.StringPair.
     */
    public void testReset() throws IOException {
        System.out.println("reset");
        
        StringPair instance = new StringPair("The lazy fox");
        instance.getWriter().write("Awake Chicken");
        instance.reset();
        
        String expResult = "";
        String result = instance.getResult();
        assertEquals(expResult, result);        
    }
    
}
