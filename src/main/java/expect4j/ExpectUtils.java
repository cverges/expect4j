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

import com.jcraft.jsch.*;
import expect4j.matches.*;
import java.io.*;
import java.net.Socket;
import java.util.Hashtable;
import java.util.logging.*;
import org.apache.commons.net.io.*;
import org.apache.commons.net.telnet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities functions to help access the Expect4J library in the most
 * common ways. This functions can be used directly or used as a model
 * to copy for your own uses.
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public abstract class ExpectUtils {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(ExpectUtils.class);

    /**
     * Creates an HTTP client connection to a specified HTTP server and
     * returns the entire response.  This function simulates <code>curl
     * http://remotehost/url</code>.
     *
     * @param remotehost the DNS or IP address of the HTTP server
     * @param url the path/file of the resource to look up on the HTTP
     *        server
     * @return the response from the HTTP server
     * @throws Exception upon a variety of error conditions
     */
    public static String Http(String remotehost, String url) throws Exception {
        return Http(remotehost, 80, url);
    }

    /**
     * Creates an HTTP client connection to a specified HTTP server and
     * returns the entire response.  This function simulates <code>curl
     * http://remotehost/url</code>.
     *
     * @param remotehost the DNS or IP address of the HTTP server
     * @param url the path/file of the resource to look up on the HTTP
     *        server
     * @return the response from the HTTP server
     * @throws Exception upon a variety of error conditions
     */
    public static String Http(String remotehost, int port, String url) throws Exception {
        Socket s = null;
        s = new Socket(remotehost, port);
        logger.debug("Connected to " + s.getInetAddress().toString() );

        if (false) {
            // for serious connection-oriented debugging only
            PrintWriter out = new PrintWriter(s.getOutputStream(), false);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            System.out.println("Sending request");
            out.print("GET " + url + " HTTP/1.1\r\n");
            out.print("Host: " + remotehost + "\r\n");
            out.print("Connection: close\r\n");
            out.print("User-Agent: Expect4j\r\n");
            out.print("\r\n");
            out.flush();
            System.out.println("Request sent");

            System.out.println("Receiving response");
            String line;
            while ((line = in.readLine()) != null)
                System.out.println(line);
            System.out.println("Received response");
            if (line == null)
                System.exit(0);
        }

        Expect4j expect = new Expect4j(s);

        logger.debug("Sending HTTP request for " + url);
        expect.send("GET " + url + " HTTP/1.1\r\n");
        expect.send("Host: " + remotehost + "\r\n");
        expect.send("Connection: close\r\n");
        expect.send("User-Agent: Expect4j\r\n");
        expect.send("\r\n");

        logger.debug("Waiting for HTTP response");
        String remaining = null;
        expect.expect(new Match[] {
            new RegExpMatch("HTTP/1.[01] \\d{3} (.*)\n?\r", new Closure() {
                public void run(ExpectState state) {
                    logger.trace("Detected HTTP Response Header");

                    // save http code
                    String match = state.getMatch();
                    String parts[] = match.split(" ");

                    state.addVar("httpCode", parts[1]);
                    state.exp_continue();
                }
            }),
            new RegExpMatch("Content-Type: (.*\\/.*)\r\n", new Closure() {
                public void run(ExpectState state) {
                    logger.trace("Detected Content-Type header");
                    state.addVar("contentType", state.getMatch() );
                    state.exp_continue();
                }
            }),
            new EofMatch( new Closure() { // should cause entire page to be collected
                public void run(ExpectState state) {
                    logger.trace("Found EOF, done receiving HTTP response");
                }
            }), // Will cause buffer to be filled up till end
            new TimeoutMatch(10000, new Closure() {
                public void run(ExpectState state) {
                    logger.trace("Timeout waiting for HTTP response");
                }
            })
        });

        remaining = expect.getLastState().getBuffer(); // from EOF matching

        String httpCode = (String) expect.getLastState().getVar("httpCode");

        String contentType = (String) expect.getLastState().getVar("contentType");

        s.close();

        return remaining;
    }

    /**
     * Creates an SSH session to the given server on TCP port 22 using
     * the provided credentials.  This is equivalent to Expect's
     * <code>spawn ssh $hostname</code>.
     *
     * @param hostname the DNS or IP address of the remote server
     * @param username the account name to use when authenticating
     * @param password the account password to use when authenticating
     * @return the controlling Expect4j instance
     * @throws Exception on a variety of errors
     */
    public static Expect4j SSH(String hostname, String username, String password) throws Exception {
        return SSH(hostname, username, password, 22);
    }

    /**
     * Creates an SSH session to the given server on a custom TCP port
     * using the provided credentials.  This is equivalent to Expect's
     * <code>spawn ssh $hostname</code>.
     *
     * @param hostname the DNS or IP address of the remote server
     * @param username the account name to use when authenticating
     * @param password the account password to use when authenticating
     * @param port the TCP port for the SSH service
     * @return the controlling Expect4j instance
     * @throws Exception on a variety of errors
     */
    public static Expect4j SSH(String hostname, String username, String password, int port) throws Exception {
        logger.debug("Creating SSH session with " + hostname + ":" + port + " as " + username);

        JSch jsch = new JSch();

        //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");

        final Session session = jsch.getSession(username, hostname, port);
        if (password != null) {
            logger.trace("Setting the Jsch password to the one provided (not shown)");
            session.setPassword(password);
        }

        java.util.Hashtable<String, String> config = new java.util.Hashtable<>();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setDaemonThread(true);
        session.connect(3 * 1000);   // making a connection with timeout.

        ChannelShell channel = (ChannelShell) session.openChannel("shell");

        //channel.setInputStream(System.in);
        //channel.setOutputStream(System.out);

        channel.setPtyType("vt102");

        //channel.setEnv("LANG", "ja_JP.eucJP");

        Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream()) {
            public void close() {
                super.close();
                session.disconnect();
            }
        };

        channel.connect(5 * 1000);

        return expect;
    }

    /**
     * TODO Simulate "Could not open connection to the host, on port...."
     * TODO Simulate "Connection refused"
     */
    public static Expect4j telnet(String hostname, int port) throws Exception {
        // This library has trouble with EOF
        final TelnetClient client = new TelnetClient();

        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, true);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(false, false, false, false);
        client.addOptionHandler( ttopt );
        client.addOptionHandler( echoopt );
        client.addOptionHandler( gaopt );

        client.connect(hostname, port);
        InputStream is =  new FromNetASCIIInputStream( client.getInputStream() ); // null until client connected
        OutputStream os = new ToNetASCIIOutputStream( client.getOutputStream() );

        /*
        URL url=new URL("telnet", hostname, port, "",  new thor.net.URLStreamHandler());
        final URLConnection urlConnection=url.openConnection();
        urlConnection.connect();
        if (urlConnection instanceof TelnetURLConnection) {
            ((TelnetURLConnection)urlConnection).setTelnetTerminalHandler(new SimpleTelnetTerminalHandler());
        }
        OutputStream os=urlConnection.getOutputStream();
        InputStream is=urlConnection.getInputStream();

        StreamPair pair = new StreamPair(is, os) {
            public void close() {
                try { ((TelnetURLConnection)urlConnection).disconnect(); }catch(Exception e) { }
            }
        };
         */

        return new Expect4j(is, os) {
            public void close() {
                super.close();
                try {
                    client.disconnect();
                } catch (IOException e) {
                    logger.error("Failed to close telnet session", e);
                }
            }
        };
    }

    /*
    class SimpleTelnetTerminalHandler extends DefaultTelnetTerminalHandler implements TelnetConstants {
        public void LineFeed() {
            System.out.print('\n');
            System.out.flush();
        }
        public void CarriageReturn() {
            System.out.print('\r');
            System.out.flush();
        }
        public void BackSpace() {
            System.out.print((char)BS);
            System.out.flush();
        }
        public void HorizontalTab() {
            System.out.print((char)HT);
            System.out.flush();
        }
    }
    */

    /**
     * Spawns a local process with the input/output streams controlled
     * via Expect4J.  The command line provided is broken into multiple
     * arguments by whitespace.
     *
     * @param cmdLine a specified system command
     * @return the controlling Expect4j instance
     * @see Runtime#exec(String)
     */
    public static Expect4j spawn(String cmdLine) throws Exception {
        return spawn(cmdLine, null);
    }

    /**
     * Spawns a local process with the input/output streams controlled
     * via Expect4J.  The command line provided is broken into multiple
     * arguments by whitespace.
     *
     * @param cmdLine a specified system command
     * @param envParams array of strings, each element of which has
     *                  environment variable settings in the format
     *                  <i>name=value</i>, or {@code null} if the
     *                  subprocess should inherit the environment of the
     *                  current process.
     * @return the controlling Expect4j instance
     * @see Runtime#exec(String, String[])
     */
    public static Expect4j spawn(String cmdLine, String[] envParams) throws Exception {
        String[] cmdArgs = cmdLine.split(" ");
        return spawn(cmdArgs, envParams);
    }

    /**
     * Spawns a local process with the input/output streams controlled
     * via Expect4J.  The command line arguments are provided in a
     * pre-split manner.
     *
     * @param cmdArgs array containing the command to call and its
     *                arguments.
     * @return the controlling Expect4j instance
     * @see Runtime#exec(String[])
     */
    public static Expect4j spawn(String cmdArgs[]) throws Exception {
        return spawn(cmdArgs, null);
    }

    /**
     * Spawns a local process with the input/output streams controlled
     * via Expect4J.  The command line arguments are provided in a
     * pre-split manner.
     *
     * @param cmdArgs array containing the command to call and its
     *                arguments.
     * @param envParams array of strings, each element of which has
     *                  environment variable settings in the format
     *                  <i>name=value</i>, or {@code null} if the
     *                  subprocess should inherit the environment of the
     *                  current process.
     * @return the controlling Expect4j instance
     * @see Runtime#exec(String[], String[])
     */
    public static Expect4j spawn(String cmdArgs[], String[] envParams) throws Exception {
        Process process = Runtime.getRuntime().exec(cmdArgs, envParams);
        Expect4j expect = new Expect4j(process);
        return expect;
    }
}
