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

import expect4j.matches.*;
import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class ExpectUtilsTelnetTest extends TestCase {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(ExpectUtilsTelnetTest.class);

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
                    try {
                        expect.send(username + "\r");
                    } catch (Exception e) {
                        logger.warn(e.getMessage());
                    }
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
                    try {
                        expect.send(password + "\r");
                    } catch (Exception e) {
                        logger.warn(e.getMessage());
                    }
                    state.addVar("sentPassword", Boolean.TRUE);
                    state.exp_continue();
                }
            }),
            new RegExpMatch("@" + hostname + "\\]", new Closure() {
                public void run(ExpectState state) {
                    logger.warn("Wow, this actually worked");
                    state.addVar("sentExit", Boolean.TRUE);
                    try { expect.send("exit\r"); } catch(Exception e) { }
                }
            }),
            /*
            new EofMatch(new Closure() {
                public void run(ExpectState state) {
                    // suck up everything until EOF
                    state.addVar("gotEOF", Boolean.TRUE);
                    logger.warn("EOF");
                }
            }),
            */
            new TimeoutMatch(new Closure() {
                public void run(ExpectState state) {
                    logger.warn(":-( Timeout");
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
