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
import expect4j.matches.*;
import java.util.*;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class ExpectUtilsHttpTest extends TestCase {
    
    public ExpectUtilsHttpTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
    /**
     * Test of Http method, of class expect4j.ExpectUtils.
     */
    public void testHttp() throws Exception {
        System.out.println("Http");
        
        System.setProperty("expect4j.level", "400");
        //java.util.logging.LogManager.getLogManager().readConfiguration();
 
        String remotehost = "www.seas.harvard.edu";
        String url = "/";
        String expResult = "Harvard School of Engineering and Applied Sciences";
 
        String result = ExpectUtils.Http(remotehost, url);

        assertNotNull(result);
 
        assertTrue( result.indexOf(expResult) != -1 );
    }
}
