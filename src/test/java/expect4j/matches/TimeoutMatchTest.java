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

package expect4j.matches;

import junit.framework.*;
import expect4j.*;

/**
 *
 * @author justin
 */
public class TimeoutMatchTest extends TestCase {

    public TimeoutMatchTest(String testName) {
        super(testName);
    }

    Closure closure;
    public void setUp() {
        closure = new Closure() {
            public void run(ExpectState state) throws Exception {

            }
        };
    }
    /**
     * Test of getTimeout method, of class expect4j.matches.TimeoutMatch.
     */
    public void testGetTimeout() {
        System.out.println("getTimeout");

        TimeoutMatch instance = null;
        long result;

        // Default
        instance = new TimeoutMatch(closure);
        result = instance.getTimeout();
        assertEquals(Expect4j.TIMEOUT_NOTSET, result);

        // Custom
        long expected = 555L;
        instance = new TimeoutMatch(expected, closure);
        result = instance.getTimeout();
        assertEquals(expected, result);
    }
}
