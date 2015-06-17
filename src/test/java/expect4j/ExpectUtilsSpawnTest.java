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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import expect4j.matches.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import junit.framework.*;

import org.apache.commons.lang3.SystemUtils;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class ExpectUtilsSpawnTest {
    private static final Logger logger = LoggerFactory.getLogger(ExpectUtilsSpawnTest.class);

    @Test
    public void testSpawnOnUnixLinux() throws Exception {
        System.out.println("spawn (unix/linux test variant)");

        Assume.assumeThat("This test variant is only intended for Unix/Linux systems", SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX, is(true));

        final String executable = "ifconfig lo";
        final String expResult = "Local Loopback";

        final Expect4j expect = ExpectUtils.spawn(executable);
        expect.setDefaultTimeout( 5000 );

        // Don't have to do anything, since the match will be saved
        int index = expect.expect(expResult);
        assertEquals(0, index);

        String result = expect.getLastState().getMatch();
        assertEquals(expResult, result);

        final StringBuffer rxStr = new StringBuffer();
        final StringBuffer txStr = new StringBuffer();

        List<Match> matches = new ArrayList<>();
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

        assertTrue(rxStr.length() > 0);
        try {
            assertTrue(Long.valueOf(rxStr.toString()) != null);
        } catch (Exception e) {
            logger.warn("Received " + e);
            assertTrue(false);
        }

        assertTrue(txStr.length() > 0);
        try {
            assertTrue(Long.valueOf(txStr.toString()) != null);
        } catch (Exception e) {
            logger.warn("Transmitted " + e);
            assertTrue(false);
        }
    }

    @Test
    public void testSpawnOnWindows() throws Exception {
        System.out.println("spawn (windows test variant)");

        Assume.assumeThat("This test variant is only intended for Windows systems", SystemUtils.IS_OS_WINDOWS, is(true));

        final String executable = "cmd /c net statistics Workstation";
        final String expResult = "Workstation Statistics";

        final Expect4j expect = ExpectUtils.spawn(executable);
        expect.setDefaultTimeout( 5000 );

        // Don't have to do anything, since the match will be saved
        int index = expect.expect(expResult);
        assertEquals(0, index);

        String result = expect.getLastState().getMatch();
        assertEquals(expResult, result);

        final StringBuffer receivedStr = new StringBuffer();
        final StringBuffer transmittedStr = new StringBuffer();

        List<Match> matches = new ArrayList<>();
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

        assertTrue(receivedStr.length() > 0);
        try {
            assertTrue(Long.valueOf(receivedStr.toString()) != null);
        } catch (Exception e) {
            logger.warn("Received " + e);
            assertTrue(false);
        }

        assertTrue(transmittedStr.length() > 0);
        try {
            assertTrue(Long.valueOf(transmittedStr.toString()) != null);
        } catch (Exception e) {
            logger.warn("Transmitted " + e);
            assertTrue(false);
        }
    }

    @Test
    public void testSpawnOnMacOSX() throws Exception {
        System.out.println("spawn (mac os x test variant)");

        Assume.assumeThat("This test variant is only intended for Mac OS X systems", SystemUtils.IS_OS_MAC_OSX, is(true));

        final String executable = "ifconfig lo0";
        final String expResult = "LOOPBACK";

        final Expect4j expect = ExpectUtils.spawn(executable);
        expect.setDefaultTimeout( 5000 );

        // Don't have to do anything, since the match will be saved
        int index = expect.expect(expResult);
        assertEquals(0, index);

        String result = expect.getLastState().getMatch();
        assertEquals(expResult, result);

        final StringBuffer mtuStr = new StringBuffer();
        final StringBuffer flagsStr = new StringBuffer();

        List<Match> matches = new ArrayList<>();
        matches.add( new RegExpMatch("mtu (\\d+)", new Closure() {
            public void run(ExpectState state) {
                mtuStr.append( state.getMatch(1) );
                state.exp_continue();
            }
        }) );
        matches.add( new RegExpMatch("flags=(\\d+)", new Closure() {
            public void run(ExpectState state) {
                flagsStr.append( state.getMatch(1) );
                state.exp_continue();
            }
        }) );
        index = expect.expect( matches );

        long mtu = 0;
        try {
            mtu = Long.valueOf( mtuStr.toString() ).longValue();
        } catch (Exception e) {
            logger.warn("MTU " + e);
        }
        assertTrue( mtu > 0 );

        long flags = 0;
        try {
            flags = Long.valueOf( flagsStr.toString() ).longValue();
        } catch(Exception e) {
            logger.warn("Flags " + e);
        }
        assertTrue( flags > 0 );
    }
}
