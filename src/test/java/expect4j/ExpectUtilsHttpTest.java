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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import expect4j.matches.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import junit.framework.*;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class ExpectUtilsHttpTest extends TestCase {

    protected HttpServer httpServer;
    protected String address = "127.0.0.1";
    protected int port;

    public ExpectUtilsHttpTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(address, 0), 0);
        httpServer.createContext("/", new TestHandler());
        httpServer.setExecutor(null);
        httpServer.start();

        port = httpServer.getAddress().getPort();
    }

    public void tearDown() throws Exception {
        try {
            httpServer.stop(0);
        } catch (Exception e) {
            // Don't worry 'bout it
        }
    }

    /**
     * Test of Http method, of class expect4j.ExpectUtils.
     */
    public void testHttp() throws Exception {
        System.out.println("Http");

        System.setProperty("expect4j.level", "400");
        //java.util.logging.LogManager.getLogManager().readConfiguration();

        String url = "/";
        String expResult = "Harvard School of Engineering and Applied Sciences";

        String result = ExpectUtils.Http(address, port, url);

        assertNotNull(result);

        assertTrue( result.indexOf(TestHandler.response) != -1 );
    }

    class TestHandler implements HttpHandler {
        public static final String response = "We cannot solve our problems with the same thinking we used when we created them.";

        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
