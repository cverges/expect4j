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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class ExpectUtilsSSHTest extends TestCase {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(ExpectUtilsSSHTest.class);

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
        logger.info("Timestamp: " + result);

        Date expResult = new Date();
        logger.info("Timestamp: " + expResult);
        assertTrue( result.before(expResult) );

        expect.close();
    }
}
