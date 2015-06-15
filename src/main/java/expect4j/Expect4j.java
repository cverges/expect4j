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

// TODO: replace these with specific class imports
import java.io.*;
import java.util.*;
import org.apache.oro.text.regex.*;

import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an API for interacting with the reader/writer streams to be
 * managed.  <code>Expect4j</code> provides both <code>send</code> and
 * <code>expect</code> functionality, like is found with the canonical
 * Expect implementation.  A variety of {@link Match}ers with associated
 * {@link Closure}s may be used to implement complex Expect parsers.
 * <p>
 * <pre>
 * import expect4j.Expect4j;
 * import java.io.InputStream;
 * import java.io.OutputStream;
 *
 * InputStream is = ...;
 * OutputStream os = ...;
 *
 * Expect4j expect = new Expect4j(is, os);
 * expect.setDefaultTimeout(10 * 1000);
 *
 * final StringBuffer someTextBuffer = new StringBuffer();
 *
 * expect.expect(new Match[] {
 *     new GlobMatch("text defined as a glob pattern", new Closure() {
 *         public void run(ExpectState state) {
 *             state.addVar("found-glob", "true");
 *             state.exp_continue();
 *         }
 *     }),
 *     new RegExpMatch("(uses)? PERL(\\d+) regular expressions", new Closure() {
 *         public void run(ExpectState state) {
 *             someTextBuffer.append(state.getMatch(2));
 *             state.exp_continue_reset_timer();
 *         }
 *     }),
 *     new EofMatch(new Closure() {
 *         public void run(ExpectState state) {
 *             state.addVar("eof-found", "true");
 *         }
 *     }),
 *     new TimeoutMatch(new Closure() {
 *         public void run(ExpectState state) {
 *             state.addVar("timed-out", "true");
 *         }
 *     })
 * });
 *
 * Boolean eofFound = new Boolean( (String)expect.getLastState().getVar("eof-found") );
 * String someText = someTextBuffer.toString();</pre>
 *
 * This implementation currently does not provide matching
 * <code>interact</code> functionality.
 * <p>
 * TODO: "expect eof"
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class Expect4j {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(Expect4j.class);

    /**
     * TODO
     */
    IOPair pair;

    /**
     * TODO
     */
    Consumer consumer;

    /**
     * TODO
     */
    Thread consumerThread;

    /**
     * TODO
     */
    Perl5Matcher matcher;

    /**
     * TODO
     */
    PatternMatcherInput input;

    /**
     * Create an <code>Expect4j</code> instance based on an {@link
     * IOPair} concrete instance.
     *
     * @param pair the <code>IOPair</code> concrete instance that
     *             provides access to the reader/writer streams
     */
    public Expect4j(IOPair pair) {
        logger.trace("Creating new Expect4J instance " + this + " using IOPair " + pair);

        // Matching
        matcher = new Perl5Matcher();
        matcher.setMultiline(true);
        input = new PatternMatcherInput("");

        // IO
        this.pair = pair;
        if (true) {
            consumer = new BlockingConsumer(pair);
        } else {
            // TODO: this section is never hit due to the "if (true)" above
            consumer = new PollingConsumer(pair);
        }

        consumerThread = new Thread(consumer);
        consumerThread.setDaemon(true);
        logger.trace("Starting consumer thread " + consumerThread + " for Expect4J instance " + this);
        consumerThread.start();

        mode = STATE_INITIALIZED;
    }

    /**
     * Creates an <code>Expect4j</code> instance based on a {@link
     * java.net.Socket}.
     *
     * @param socket the <code>Socket</code> to manage
     */
    public Expect4j(Socket socket) throws IOException {
        this(socket.getInputStream(), socket.getOutputStream());
        logger.trace("Created Expect4J instance " + this + " based on Socket " + socket);
    }

    /**
     * Creates an <code>Expect4j</code> instance based on an {@link
     * java.io.InputStream} and {@link java.io.OutputStream}.
     *
     * @param is the <code>InputStream</code> to use for reading data
     * @param os the <code>OutputStream</code> to use for writing data
     */
    public Expect4j(InputStream is, OutputStream os) {
        this( new StreamPair(is, os) );
        logger.trace("Created Expect4J instance " + this + " based on InputStream " + is + " and OutputStream " + os);
    }

    /**
     * Creates an <code>Expect4j</code> instance based on a spawned
     * {@link java.lang.Process}.
     *
     * @param process the spawned <code>Process</code> that will be managed
     */
    public Expect4j(Process process) {
        this( process.getInputStream(), process.getOutputStream() );
        logger.trace("Created Expect4J instance " + this + " based on Process " + process);
    }

    /**
     * Class has not had its core variables set yet.
     */
    static final int STATE_UNINIT = 1;

    /**
     * Class has been given the appropriate arguments.
     */
    static final int STATE_INITIALIZED = 2;

    /**
     * Class has been given patterns, and it is currently parsing the streams.
     */
    static final int STATE_EXPECTING = 3;

    /**
     * Class found a match to a pattern, and it can receive new patterns.
     */
    static final int STATE_EXPECTED = 4;

    /**
     * State of processing.
     */
    int mode = STATE_UNINIT;

    /**
     * Passes data to the writer stream for remote execution.  This
     * function simulates the Expect <code>exp_send</code> function.
     * <p>
     * TODO: might have to strip <code>\n</code> and replace with
     *       <code>\r</code>
     *
     * @param data the data to pass to the writer
     * @throws IOException if an error occurs with the writer stream
     * @see <a href="http://wiki.tcl.tk/14317">http://wiki.tcl.tk/14317</a>
     */
    public void send(String data) throws IOException {
        consumer.send(data);
    }

    /**
     * An error code returned by <code>Expect4j.expect</code> to
     * indicate that no match was found and no re-attempt was made.
     */
    public static final int RET_TRIED_ONCE = -4;

    /**
     * An error code returned by <code>Expect4j.expect</code> to
     * indicate that the end of file marker was encountered when
     * accessing the reader stream.
     */
    public static final int RET_EOF = -3;

    /**
     * An error code returned by <code>Expect4j.expect</code> to
     * indicate that the timeout value expired prior to finding a
     * concluding match.  It should be noted that matches may have been
     * found prior to this being returned, but their associated {@link
     * Closure}s may have continued processing via {@link
     * ExpectState#exp_continue}.
     */
    public static final int RET_TIMEOUT = -2;

    /**
     * An error code returned by <code>Expect4j.expect</code> to
     * indicate that some unforeseen condition occurred.
     */
    public static final int RET_UNKNOWN = -1;

    /**
     * Attempts to detect the provided pattern as an exact match.
     * @param pattern the pattern to find in the reader stream
     * @return the number of times the pattern is found,
     *         or an error code
     * @throws MalformedPatternException if the pattern is invalid
     * @throws Exception if a generic error is encountered
     */
    public int expect(String pattern) throws MalformedPatternException, Exception {
        logger.trace("Searching for '" + pattern + "' in the reader stream");
        return expect(pattern, null);
    }

    /**
     * Attempts to detect the provided pattern and executes the provided
     * {@link Closure} if it is detected.
     * @param pattern the pattern to find in the reader stream
     * @param handler the handler to execute if the pattern is found
     * @return the number of times the pattern is found,
     *         or an error code
     * @throws MalformedPatternException if the pattern is invalid
     * @throws Exception if a generic error is encountered while
     *                   processing the {@link Closure}
     */
    public int expect(String pattern, Closure handler) throws MalformedPatternException, Exception {
        logger.trace("Searching for '" + pattern + "' in the reader stream and executing Closure " + handler + " if found");
        PatternPair match = new GlobMatch(pattern, handler);
        List<Match> list = new ArrayList<>();
        list.add(match);
        return expect(list);
    }

    /**
     * Attempts to detect the patterns contained in the {@link Match}
     * list and executes their associated {@link Closure}s if any are
     * detected.
     * @param args the list of patterns and associated {@link Closure}s
     *             to execute
     * @return the number of times the pattern is found,
     *         or an error code
     * @throws MalformedPatternException if the pattern(s) is/are invalid
     * @throws Exception if a generic error is encountered while
     *                   processing the {@link Closure}
     */
    public int expect(Match args[]) throws MalformedPatternException, Exception {
        logger.trace("Searching for " + args.length + " patterns in the reader stream");
        List<Match> pairs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            Match match = args[i];
            pairs.add(match);
        }
        return expect(pairs);
    }

    /**
     * Attempts to detect the patterns contained in the {@link
     * java.util.List} list and executes their associated {@link
     * Closure}s if any are detected.
     * @param pairs the list of patterns and associated {@link Closure}s
     *              to execute
     * @return the number of times the pattern is found,
     *         or an error code
     * @throws Exception if a generic error is encountered while
     *                   processing the {@link Closure}
     */
    public int expect(final List<Match> pairs) throws Exception {
        logger.trace("Searching for " + pairs.size() + " patterns in the reader stream");

        // Buckets
        EofMatch eofMatch = null;
        TimeoutMatch timeoutMatch = null;
        List<Match> patternMatches = new ArrayList<>();

        // Fill buckets in one swoop
        Iterator<Match> iter = pairs.iterator();
        while (iter.hasNext()) {
            Match match = iter.next();
            if (!(match instanceof Match)) {
                logger.debug("Object " + match + " is not of type expect4j.matches.Match, cannot use as a pattern");
                continue;
            } else if (match instanceof PatternPair) {
                logger.trace("Searching for PatternPair " + match + " in the reader stream");
                patternMatches.add( match );
            } else if (match instanceof TimeoutMatch) {
                logger.trace("Registering custom TimeoutMatch handler " + match);
                timeoutMatch = (TimeoutMatch) match;
            } else if (match instanceof EofMatch) {
                logger.trace("Registering custom EofMatch handler " + match);
                eofMatch = (EofMatch) match;
            } else {
                logger.debug("Unexpected match object " + match + " found in the pattern list, ignoring");
            }
        }

        // if( eofMatch == null ) eofMatch = new EofMatch();
        // if( timeoutMatch == null ) // that's ok
        long timeout;
        if (timeoutMatch != null && timeoutMatch.getTimeout() != TIMEOUT_NOTSET) {
            timeout = timeoutMatch.getTimeout();
        } else {
            timeout = defaultTimeout;
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeout;

        logger.debug("Timeout set to " + timeout + " milliseconds, expires at " + endTime);

        boolean foundTimeout = false;
        boolean foundEof = false;
        int index = RET_UNKNOWN;

        // New states are based on g_state and they need to see a null
        // g_state on the first match
        g_state = null;

        // Primary loop, which only really continues if
        // State.exp_continue() is called or no match was found
        String toMatch = null; // from last pause call
        while (true) {
            if (timeout != TIMEOUT_FOREVER && System.currentTimeMillis() >= endTime) {
                logger.debug("Detection timeout expired");
                foundTimeout = true;
                break;
            }

            synchronized(consumer) { // while matching
                toMatch = consumer.pause();
                logger.trace("Size of toMatch is " + toMatch.length());
                // make sure to resume before any break/continue
                foundEof = consumer.foundEOF(); // has to be called before resume

                input.setInput(toMatch);

                logger.debug("Finding first match using >>>" + printBuffer() + "<<< as the haystack");

                boolean foundMatch = false;
                try {
                    foundMatch = runFirstMatch(patternMatches);
                } catch (Exception e) {
                    logger.warn("Forwarding an exception that occurred in a Closure: " + e);
                    consumer.resume();
                    throw e;
                }

                // Both paths call resume.
                if (foundMatch) {
                    int matchedWhere = g_state.getMatchedWhere();
                    int matchedLength = g_state.getMatch().length();
                    logger.debug("Matched @ " + matchedWhere + " with a length of " + matchedLength);

                    // resume consumer, at a later offset
                    consumer.resume(matchedWhere + matchedLength);

                    // find index to return
                    PatternPair singlepair = (PatternPair) patternMatches.get( g_state.getPairIndex() );
                    logger.trace("Pair found " + singlepair.getPattern().getPattern() );
                    index = pairs.indexOf( singlepair );
                    logger.trace("Index found " + index);

                    if (!g_state.shouldContinue()) {
                        logger.trace("NOT Continuing");
                        break;
                    } else {
                        logger.trace("Continuing");
                    }

                    if (g_state.shouldResetTimer()) {
                        // keep start time where it is
                        endTime = System.currentTimeMillis() + timeout;
                    }
                    continue; // skips waitForBuffer since buffer might already have what we're looking for

                } else {
                    logger.trace("Nothing found, resuming consumer");
                    consumer.resume();
                    if (timeout == TIMEOUT_NEVER) {
                        // The timeout variables tells us that we shouldn't try again
                        // TODO Find out if this triggers the Timeout match
                        index = RET_TRIED_ONCE;
                        break;
                    }
                }
                if (foundEof) {
                    logger.debug("Found EOF");
                    break;
                }

                if (timeout == TIMEOUT_FOREVER) {
                    consumer.waitForBuffer(TIMEOUT_FOREVER);
                } else {
                    long singleTimeout = endTime - System.currentTimeMillis();
                    logger.trace("singleTimeout: " + singleTimeout);
                    if (timeout != TIMEOUT_FOREVER && singleTimeout <= 0) {
                        // we might have gone over the timeout already
                        // restart while loop, that the typical logic takes hold
                        continue;
                    }
                    logger.debug("Waiting for more input");
                    consumer.waitForBuffer(singleTimeout);
                }
            }
        } // end while
        logger.trace("Leaving main while loop");

        Match lastmile = null;
        String lastmileBuffer = null;
        if (foundTimeout) { //removed index == -1
            logger.trace("Dealing with Timeout");
            if (timeoutMatch == null)
                index = RET_TIMEOUT; // Timeout with a Timeout match
            else
                lastmile = timeoutMatch;
        }

        if (foundEof)  {
            logger.info("Dealing with EOF " + eofMatch);
            if (eofMatch != null) {
                lastmileBuffer = toMatch;
                lastmile = eofMatch;
            } else if (index == -1) {
                index = RET_EOF;
            }
        }

        // We're in the final stretch, but we might have one last closure to run.
        if (lastmile != null) {
            logger.trace("Running last mile");

            //TODO provide buffer vars for EOF
            Closure closure = lastmile.getClosure();
            index = pairs.indexOf(lastmile);

            ExpectState state = prepareClosure(index, lastmileBuffer);
            try {
                if (closure != null)
                    closure.run(state);
            } catch (Exception e) {
                logger.warn("Forwarding an exception that occurred in a Closure: " + e);
                logger.trace("Closure body: " + closure.toString());
                throw e;
            } finally {
                g_state = state;
            }
        }
        return index;
    }

    /**
     * An internal helper function that converts the input buffer into a
     * printable <code>String</code>.
     *
     * @return the input buffer as a printable <code>String</code>
     */
    protected String printBuffer() {
        String javaStr = new String(input.getBuffer());
        javaStr = javaStr.replaceAll("\\r", "\\\\r");
        javaStr = javaStr.replaceAll("\\n", "\\\\n");
        return javaStr;
    }

    /**
     * TODO
     *
     * @param pairIndex TODO
     * @param buffer TODO
     * @return TODO
     */
    protected ExpectState prepareClosure(int pairIndex, String buffer) {
        ExpectState state;
        Map<String, Object> prevMap = null;
        if (g_state != null) {
            prevMap = g_state.getVars();
        }

        state = new ExpectState(pairIndex, buffer, prevMap);

        return state;
    }

    /**
     * Don't use input, it's match values might have been reset in the
     * loop that looks for the first possible match.
     *
     * @param pairIndex TODO
     * @param result TODO
     * @return TODO
     */
    protected ExpectState prepareClosure(int pairIndex, MatchResult result) {
        /* TODO: potentially remove this?
        {
            System.out.println( "Begin: " + result.beginOffset(0) );
            System.out.println( "Length: " + result.length() );
            System.out.println( "Current: " + input.getCurrentOffset() );
            System.out.println( "Begin: " + input.getMatchBeginOffset() );
            System.out.println( "End: " + input.getMatchEndOffset() );
            //System.out.println( "Match: " + input.match() );
            //System.out.println( "Pre: >" + input.preMatch() + "<");
            //System.out.println( "Post: " + input.postMatch() );
        }
         */

        // Prepare Closure environment
        ExpectState state;
        Map<String, Object> prevMap = null;
        if (g_state != null) {
            prevMap = g_state.getVars();
        }

        int matchedWhere = result.beginOffset(0);
        String matchedText = result.toString(); // expect_out(0,string)

        // Unmatched upto end of match
        // expect_out(buffer)
        char[] chBuffer = input.getBuffer();
        String copyBuffer = new String(chBuffer, 0, result.endOffset(0) );

        List<String> groups = new ArrayList<>();
        for (int j = 1; j <= result.groups(); j++) {
            String group = result.group(j);
            groups.add( group );
        }
        state = new ExpectState(copyBuffer.toString(), matchedWhere, matchedText, pairIndex, groups, prevMap);

        return state;
    }

    /**
     * TODO
     *
     * @param pairs TODO
     * @return found something, and ran it. Calling function should use g_state to figure out what to do next
     */
    protected boolean runFirstMatch(List /* <PatternPair> */ pairs) throws Exception {
        MatchResult firstResult = null;
        PatternPair firstPair = null;
        int pairIndex = -1;

        ListIterator iter = pairs.listIterator();
        while (iter.hasNext()) {
            PatternPair pair = (PatternPair) iter.next();
            Pattern pattern = pair.getPattern();

            // reset input to begining
            input.setCurrentOffset(input.getBeginOffset());

            if (matcher.contains(input, pattern)) {
                MatchResult result = matcher.getMatch();
                if (firstResult == null || result.beginOffset(0) < firstResult.beginOffset(0)) {
                    firstResult = result;
                    firstPair = pair;
                    pairIndex = iter.previousIndex(); // TODO confirm this
                }
            }
        }

        if (firstResult == null)
            return false; // didn't find anything

        // Found something
        //input's offset are illegal at this phase
        //input.setCurrentOffset(input.getBeginOffset());
        logger.trace("Using a result for " + firstPair.getPattern().getPattern());
        ExpectState state = prepareClosure(pairIndex, firstResult);
        Closure closure = firstPair.getClosure();

        try {
            if (closure != null)
                closure.run(state);
        } catch (Exception e) {
            throw e;
        } finally {
            g_state = state;
        }
        return true;
    }

    /**
     * A snapshot of the <code>Expect4j</code> state machine in its
     * current context.
     */
    private ExpectState g_state = null;

    /**
     * Returns the last match of the <code>Expect4j</code> state
     * machine.  If no match is found, an empty {@link ExpectState}
     * class is returned.
     *
     * @return the last match found by <code>Expect4j</code>
     */
    public ExpectState getLastState() {
        // Very possibly null from an ONCE_CHANCE or initial state
        // But since the expected use is to call expect4j.getLastState().getMatch(), we want to help protect against NPE
        return (g_state != null) ? g_state : new ExpectState();
    }

    /**
     * Provide no default timeout.
     */
    public static final long TIMEOUT_NOTSET = -2;

    /**
     * Never timeout, wait forever.
     */
    public static final long TIMEOUT_FOREVER = -1;

    /**
     * Don't give timeout a chance. If nothing is found right away, stop checking.
     */
    public static final long TIMEOUT_NEVER = 0;

    /**
     * A default match timeout of 10 seconds.
     */
    public final static long TIMEOUT_DEFAULT = 10 * 1000;

    /**
     * The default match timeout, initialized to TIMEOUT_DEFAULT.
     */
    long defaultTimeout = TIMEOUT_DEFAULT;

    /**
     * Changes the default timeout value for the Expect4j instance.
     * <p>
     * TODO: Check for valid values
     *
     * @param timeout the new timeout value in milliseconds
     */
    public void setDefaultTimeout(long timeout) {
        logger.debug("Setting default timeout to " + timeout);
        defaultTimeout = timeout;
    }

    /**
     * TODO
     *
     * @param matches TODO
     * @return TODO
     */
    protected TimeoutMatch findTimeout(Match matches[]) {
        TimeoutMatch ourTimeout = null;
        for (int i = 0; i < matches.length; i++) {
            if (matches[i] instanceof TimeoutMatch)
                ourTimeout = (TimeoutMatch) matches[i];
        }
        /* TODO: candidate for removal?
        if( ourTimeout == null ) {
            // Have to create our own
            ourTimeout = new TimeoutMatch(null);
        }
         */
        return ourTimeout;
    }

    /**
     * TODO
     *
     * @param matches TODO
     * @return TODO
     */
    protected EofMatch findEof(Match matches[]) {
        EofMatch ourEof = null;
        for (int i = 0; i < matches.length; i++) {
            if (matches[i] instanceof EofMatch)
                ourEof = (EofMatch) matches[i];
        }

        if (ourEof == null) {
            // Have to create our own
            ourEof = new EofMatch();
        }

        return ourEof;
    }

    /**
     * Stops processing the reader/writer streams being managed by the
     * Expect4j instance.
     */
    public void close() {
        logger.debug("Stopping processing of the reader/writer streams by the Expect4j instance" + this);
        consumer.stop();
    }

    /**
     * Registers a change logger that is called whenever an input change
     * is recorded to the buffer.
     *
     * @param logger the logger that is called
     */
    public void registerBufferChangeLogger(final BufferChangeLogger logger) {
        consumer.registerBufferChangeLogger(logger);
    }

    /**
     * Unregisters the change logger specified.  Once called, this
     * change logger will not receive any further updates.
     *
     * @param logger the logger that is called
     */
    public void unregisterBufferChangeLogger(final BufferChangeLogger logger) {
        consumer.unregisterBufferChangeLogger(logger);
    }
}
