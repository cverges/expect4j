/*
 * ExpectUtilsTelnetTest.java
 * JUnit based test
 *
 * Created on March 16, 2007, 9:42 AM
 */

package expect4j;

import junit.framework.*;
import expect4j.matches.*;

/**
 *
 * @author justin
 */
public class ExpectUtilsTelnetTest extends TestCase {
    
    public ExpectUtilsTelnetTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testTelnetSwitch() throws Exception {
        
    }
    public void testTelnet() throws Exception {
        System.out.println("telnet");
        
        String hostname = "hostname";
        final String username = "username";
        final String password = "password";
        
        if( hostname.equals("hostname") ) return; // fill in hostname, username,password.
        
        final Expect4j expect = ExpectUtils.telnet(hostname, 23);
        expect.setDefaultTimeout(Expect4j.TIMEOUT_FOREVER);
        
        expect.expect( new Match[] {
            new GlobMatch("login: ", new Closure() {
                public void run(ExpectState state) {
                    try { expect.send(username + "\r"); } catch(Exception e) { Expect4j.log.warning(e.getMessage() ); }
                    state.addVar("sentUsername", Boolean.TRUE);
                    state.exp_continue();
                }
            }),
            new GlobMatch("Last login: ", new Closure() {
                public void run(ExpectState state) {
                    // This match should prevent Last login: from being recognized by the above match
                    state.addVar("gotLogin", Boolean.TRUE);
                    state.exp_continue();
                }
            }),
            new GlobMatch("Password:", new Closure() {
                public void run(ExpectState state) {
                    try { expect.send(password + "\r");} catch(Exception e) { Expect4j.log.warning(e.getMessage() ); }
                    state.addVar("sentPassword", Boolean.TRUE);
                    state.exp_continue();
                }
            }),
            new RegExpMatch("@" + hostname + "\\]", new Closure() {
                public void run(ExpectState state) {
                    Expect4j.log.warning("Holy crap, this actually worked");
                    state.addVar("sentExit", Boolean.TRUE);
                    try { expect.send("exit\r"); } catch(Exception e) { }
                }
            }),
            /*
            new EofMatch(new Closure() {
                public void run(ExpectState state) {
                    // suck up everything until EOF
                    state.addVar("gotEOF", Boolean.TRUE);
                    expect.log.warning("EOF");
                }
            }),
            */
            new TimeoutMatch(new Closure() {
                public void run(ExpectState state) {
                    Expect4j.log.warning(":-( Timeout");
                }
            })
        });
        
        expect.close();
        
        ExpectState lastState = expect.getLastState();
        
        Boolean result = (Boolean) lastState.getVar("sentUsername");
        assertNotNull( result );
        
        result = (Boolean) lastState.getVar("sentPassword");
        assertNotNull( result );
        
        result = (Boolean) lastState.getVar("gotLogin");
        assertNotNull( result );

        result = (Boolean) lastState.getVar("sentExit");
        assertNotNull( result );
        
        //result = (Boolean) lastState.getVar("gotEOF");
        //assertNotNull( result );
    }    
}
