/*
 * ExpectUtilsSSHTest.java
 * JUnit based test
 *
 * Created on March 16, 2007, 9:43 AM
 */

package expect4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import junit.framework.*;
import expect4j.matches.*;
import java.util.Date;

/**
 *
 * @author justin
 */
public class ExpectUtilsSSHTest extends TestCase {
    
    public ExpectUtilsSSHTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
    /**
     * Test of SSH method, of class expect4j.ExpectUtils.
     * First response should be something like:
     * Last login: Wed Mar 14 12:13:29 2007 from pool-71-126-249-188.bstnma.fios.verizon.net
     */
    public void testSSH() throws Exception {
        System.out.println("SSH");
        
        String hostname = "hostname";
        String username = "username";
        String password = "password";
        
        if( hostname.equals("hostname") ) return;
        
        Expect4j expect = ExpectUtils.SSH(hostname, username, password, 2222);
        //expect.setDefaultTimeout(Expect4j.TIMEOUT_FOREVER);
        
        // Mar 15 17:42:02 2007
        final DateFormat format = new SimpleDateFormat("MMM dd HH:mm:ss yyy z");
        expect.expect( new Match[] {
            new RegExpMatch("Last login: \\w{3} (.*) from", new Closure() {
                public void run(ExpectState state) throws Exception {
                    String time = state.getMatch(1);
                    Date date = format.parse( time + " UTC");
                    state.addVar("timestamp", date );
                }
            })
        });
        
        expect.close();
        
        Date result = (Date) expect.getLastState().getVar("timestamp");
        assertNotNull( result );
        Expect4j.log.fine("Timestamp: " + result);
        
        Date expResult = new Date();
        Expect4j.log.fine("Timestamp: " + expResult);
        assertTrue( result.before(expResult) );
        
        expect.close();
    }
    
}
