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
import java.io.*;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
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
