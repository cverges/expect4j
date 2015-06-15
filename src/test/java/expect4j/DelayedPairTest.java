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

import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author justin
 */
public class DelayedPairTest extends TestCase {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(DelayedPairTest.class);

    /** Creates a new instance of DelayedPairTest */
    public DelayedPairTest(String testName) {
        super(testName);
    }

    public void testCtor() throws Exception {
        DelayedPair pair = new DelayedPair("The quick", 50, 1);

        Thread.sleep( 2000 );
        logger.debug("Checking for EOF");
        assertTrue(pair.ended);
    }

    public void testClose() throws Exception {
        DelayedPair pair = new DelayedPair("The quick brown fox jumped over the moon", 500, 1000);

        Thread.sleep( 500 );
        pair.close();
        Thread.sleep( 100 );

        logger.debug("Make sure pair ends early");
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
