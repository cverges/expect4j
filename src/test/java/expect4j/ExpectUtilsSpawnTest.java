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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class ExpectUtilsSpawnTest extends TestCase {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(ExpectUtilsSpawnTest.class);
    
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

        String executable;
        String expResult;
        Expect4j expect;

        if (OperatingSystem.isWindows()) {
            System.out.println("Using Windows test");
            executable = "cmd /c net statistics Workstation";
            expResult = "Workstation Statistics";
        } else if (OperatingSystem.isUnix()) {
            System.out.println("Using Unix/Linux test");
            executable = "ifconfig";
            expResult = "Local Loopback";
        } else {
            System.err.println("Your OS is not supported for this test!");
            assertEquals(0, 1);
            return;
        }
        
        expect = ExpectUtils.spawn(executable);
        expect.setDefaultTimeout( 5000 );
     
        // Don't have to do anything, since the match will be saved
        int index = expect.expect(expResult);
        assertEquals(0, index);
     
        String result = expect.getLastState().getMatch();
        assertEquals(expResult, result);

        if (OperatingSystem.isWindows()) {
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
         
            long received = 0;
            try {
                received = Long.valueOf( receivedStr.toString() ).longValue();
            } catch (Exception e) {
                logger.warn("Received " + e);
            }
            assertTrue( received > 0 );
         
            long transmitted = 0;
            try {
                transmitted = Long.valueOf( transmittedStr.toString() ).longValue();
            } catch(Exception e) {
                logger.warn("Transmitted" + e);
            }
            assertTrue( transmitted > 0 );
        } else if (OperatingSystem.isUnix()) {
            final StringBuffer rxStr = new StringBuffer();
            final StringBuffer txStr = new StringBuffer();

            ArrayList matches = new ArrayList();
            matches.add(new RegExpMatch("RX bytes:(\\d+)", new Closure() {
                public void run(ExpectState state) {
                    rxStr.append(state.getMatch(1));
                    state.exp_continue();
                }
            }));
            matches.add(new RegExpMatch("TX bytes:(\\d+)", new Closure() {
                public void run(ExpectState state) {
                    txStr.append(state.getMatch(1));
                    state.exp_continue();
                }
            }));
            index = expect.expect(matches);

            long received = 0;
            try {
                received = Long.valueOf(rxStr.toString()).longValue();
            } catch (Exception e) {
                logger.warn("Received " + e);
            }
            assertTrue(received > 0);

            long transmitted = 0;
            try {
                transmitted = Long.valueOf(txStr.toString()).longValue();
            } catch (Exception e) {
                logger.warn("Transmitted" + e);
            }
            assertTrue(transmitted > 0);
        }
    }
}
