/*
 * DelayedPairTest.java
 *
 * Created on March 13, 2007, 7:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package expect4j;

import junit.framework.*;

/**
 *
 * @author justin
 */
public class DelayedPairTest extends TestCase {
   
    /** Creates a new instance of DelayedPairTest */
    public DelayedPairTest(String testName) {
        super(testName);
    }
    
    public void testCtor() throws Exception {
        DelayedPair pair = new DelayedPair("The quick", 50, 1);
        
        Thread.sleep( 2000 );
        pair.log.fine("Checking for EOF");
        assertTrue(pair.ended);
    }
    
    public void testClose() throws Exception {
        DelayedPair pair = new DelayedPair("The quick brown fox jumped over the moon", 500, 1000);
        
        Thread.sleep( 500 );
        pair.close();
        Thread.sleep( 100 );
        
        pair.log.fine("Make sure pair ends early");
        assertTrue(pair.ended);
    }
    
    public void testWrite() throws Exception {
        DelayedPair pair = new DelayedPair("The quick brown fox jumped over the moon", 50, 100);

	// Before it has time to complete
	char ch[] = new char[1024];

	int length = pair.getReader().read(ch);
	StringBuffer result = new StringBuffer( new String(ch, 0, length) );
	System.out.println(result);
	assertFalse(result.toString().equals("The quick brown fox jumped over the moon"));
	
        Thread.sleep( 1000 );

	length = pair.getReader().read(ch);
	result.append(ch, 0, length);
	System.out.println(result);
	assertEquals(result.toString(), "The quick brown fox jumped over the moon");
	
	pair.close();        
    }
}