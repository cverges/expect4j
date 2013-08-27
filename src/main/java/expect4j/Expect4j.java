/*
 * Copyright 2007 Justin Ryan
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package expect4j;

import expect4j.matches.*;

import java.io.*;
import java.util.*;
import java.net.Socket;
import java.util.logging.*;
import org.apache.oro.text.regex.*;


/**
 * Limitation: no interact command
 * @TODO "expect eof"
 * @author justin
 */
public class Expect4j {
    static final public Logger log = Logger.getLogger(Expect4j.class.getName());
    
    IOPair pair;
    Consumer consumer;
    Thread consumerThread;
    Perl5Matcher matcher;
    PatternMatcherInput input;
    
    public Expect4j(IOPair pair) {
        // Matching
        matcher  = new Perl5Matcher();
        matcher.setMultiline(true);
        input   = new PatternMatcherInput("");
        
        // IO
        this.pair = pair;
        if( true )
            consumer = new BlockingConsumer(pair);
        else
            consumer = new PollingConsumer(pair);
        consumerThread = new Thread(consumer);
        consumerThread.setDaemon(true);
        consumerThread.start();
        
        mode = STATE_INITIALIZED;
    }
    
    /**
     * Creates a new instance of Expect4j based on a Socket
     */
    public Expect4j(Socket socket) throws IOException {
        this(socket.getInputStream(), socket.getOutputStream());
    }
    
    public Expect4j(InputStream is, OutputStream os) {
        this( new StreamPair(is, os) );
    }
    
    /**
     * process = Runtime.getRuntime().exec( cmdArgs );
     * @param process
     */
    public Expect4j(Process process) {
        this( process.getInputStream(), process.getOutputStream() );
    }
    
    /** Class has had core variables set yet. */
    static final int STATE_UNINIT = 1;
    
    /** Class has been given the appropriate arguments */
    static final int STATE_INITIALIZED = 2;
    
    /** Class has been given patterns, and it is currently parsing the streams */
    static final int STATE_EXPECTING = 3;
    
    /** Class found a match to a pattern, and it can receive new patterns */
    static final int STATE_EXPECTED = 4;
    
    /** State of processing */
    int mode = STATE_UNINIT;
    
    /**
     * @TODO might have to strip \n and replace with \r
     * simulates: exp_send
     */
    public void send(String str) throws IOException {
        log.fine("Sending from expect: " + str);
        consumer.send(str);
    }
    
    /**
     * Really simple case, seen in pexpect and expectj
     */
    public int expect(String pattern) throws MalformedPatternException, Exception {
        return expect(pattern, null);
    }
    
    /**
     * Simple case, just one pattern
     */
    public int expect(String pattern, Closure single) throws MalformedPatternException, Exception {
        PatternPair match = new GlobMatch(pattern, single);
        List /* <PatternMatch> */ list = new ArrayList();
        list.add(match);
        return expect(list);
    }
    
    public int expect(Match args[]) throws MalformedPatternException, Exception {
        List pairs = new ArrayList();
        for( int i=0; i < args.length; i++ ) {
            Match match = args[i];
            pairs.add(match);
        }
        return expect(pairs);
    }
    /*
    public int expect(Object args[]) throws MalformedPatternException {
        List pairs = new ArrayList();
        for( int i=0; i < args.length; i++ ) {
            Object arg = args[i];
            if( arg instanceof String ) {
                String argStr = (String) arg;
                if( argStr.equals("-re") ) {
                    Object firstArg = args[i++];
                    String pattern = firstArg.toString();
                    Closure closure = null;
                    if ( args[i+1] instanceof Closure )
                        closure = (Closure) args[i++];
                    // Look ahead and grab pattern and closure
                    new RegExpMatch(pattern, closure);
                } else {
                    if( argStr.equals("-glob") ) // consume default value
                        i++;
     
                    // Look ahead and grab pattern and closure
                    Object firstArg = args[i++];
                    String pattern = firstArg.toString();
                    Closure closure = null;
                    if ( args[i+1] instanceof Closure )
                        closure = (Closure) args[i++];
                    // Look ahead and grab pattern and closure
                    new GlobMatch(pattern, closure);
                }
            }
        }
        return expect(pairs);
    }
     */
    // For when the index can't be found
    public static final int RET_TRIED_ONCE = -4;
    public static final int RET_EOF = -3;
    public static final int RET_TIMEOUT = -2;
    public static final int RET_UNKNOWN = -1;
    
    /**
     * simulates expect { }
     *
     * A pair is a pattern and a closure
     * 
     * @returns index that was last matched or a RET_ error code
     */
    public int expect(final List /* <Match> */ pairs) throws Exception { // from Closure
        
        // Buckets
        EofMatch eofMatch = null;
        TimeoutMatch timeoutMatch = null;
        List /* <PatternPair> */ patternMatches = new ArrayList();
        
        // Fill buckets in one swoop
        Iterator iter = pairs.iterator();
        while( iter.hasNext() ) {
            Object match = iter.next();
            if( !(match instanceof Match) ) {
                log.warning("Passed in a non-Match");
                continue;
            }
            if( match instanceof PatternPair )
                patternMatches.add( match );
            else if( match instanceof TimeoutMatch )
                timeoutMatch = (TimeoutMatch) match;
            else if( match instanceof EofMatch )
                eofMatch = (EofMatch) match;
            
        }
        
        // if( eofMatch == null ) eofMatch = new EofMatch();
        // if( timeoutMatch == null ) // that's ok
        long timeout;
        if( timeoutMatch != null && timeoutMatch.getTimeout() != TIMEOUT_NOTSET )
            timeout = timeoutMatch.getTimeout();
        else
            timeout = defaultTimeout;
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeout;
        
        boolean foundTimeout = false;
        boolean foundEof = false;
        int index = RET_UNKNOWN;
        
        // New states are based on g_state and they need to see a null g_state on the first match
        g_state = null;
        
        //
        // Primary loop, which only really continue is exp_continue is called or we didn't match
        //
        String toMatch = null; // from last pause call
        while( true ) {
            
            if( timeout != TIMEOUT_FOREVER && System.currentTimeMillis() >= endTime ) {
                log.fine("Timeout " + endTime + " when it is " + System.currentTimeMillis() );
                foundTimeout = true;
                break;
            }

            synchronized(consumer) { // while matching
                toMatch = consumer.pause();
                log.finest("Size of toMatch: " + toMatch.length() );
                // make sure to resume before any break/continue
                foundEof = consumer.foundEOF(); // has to be called before resume
                
                input.setInput(toMatch);
                
                log.finer("Finding first match using >>>" +  printBuffer() + "<<<");
                
                
                boolean foundMatch = false;
                try {
                    foundMatch = runFirstMatch(patternMatches);
                } catch(Exception e) {
                    log.log(Level.WARNING, "In Closure", e);
                    consumer.resume(); 
                    throw e;
                }
                
                // Both paths call resume.
                if ( foundMatch ) {
                    int matchedWhere = g_state.getMatchedWhere();
                    int matchedLength = g_state.getMatch().length();
                    log.fine("Matched @ " + matchedWhere + " with a length of " + matchedLength);
                    
                    // resume consumer, at a later offset
                    consumer.resume(matchedWhere + matchedLength);
                    
                    // find index to return
                    PatternPair singlepair = (PatternPair) patternMatches.get( g_state.getPairIndex() );
                    log.finer("Pair found " + singlepair.getPattern().getPattern() );
                    index = pairs.indexOf( singlepair );
                    log.finer("Index found " + index);
                    
                    if( !g_state.shouldContinue() ) {
                        log.fine("NOT Continuing");
                        break;
                    }
                    log.fine("Continuing");
                    
                    if( g_state.shouldResetTimer() ) {
                        // keep start time where it is
                        endTime = System.currentTimeMillis() + timeout;
                    }
                    continue; // skips waitForBuffer since buffer might already have what we're looking for
                    
                } else {
                    log.fine("Nothing found, resuming consumer");
                    consumer.resume();
                    if( timeout == TIMEOUT_NEVER ) {
                        // The timeout variables tells us that we shouldn't try again
                        // TODO Find out if this triggers the Timeout match
                        index = RET_TRIED_ONCE;
                        break;
                    }
                }
                if ( foundEof ) {
                    log.fine("Found EOF");
                    break;
                }
                
                if( timeout == TIMEOUT_FOREVER ) {
                    consumer.waitForBuffer(TIMEOUT_FOREVER);
                } else {
                    long singleTimeout = endTime - System.currentTimeMillis();
                    log.fine("singleTimeout: " + singleTimeout);
                    if( timeout != TIMEOUT_FOREVER && singleTimeout <= 0 ) {
                        // we might have gone over the timeout already
                        // restart while loop, that the typical logic takes hold
                        continue;
                    }
                    log.fine("Waiting for more input");
                    consumer.waitForBuffer(singleTimeout);
                }
            }
        } // end while
        log.fine("Leaving main while loop");
        
        Match lastmile = null;
        String lastmileBuffer = null;
        if( foundTimeout ) { //removed index == -1
            log.finer("Dealing with Timeout");
            if( timeoutMatch == null )
                index = RET_TIMEOUT; // Timeout with a Timeout match
            else
                lastmile = timeoutMatch;
        }
        
        if( foundEof )  {
            log.finer("Dealing with EOF " + eofMatch);
            if ( eofMatch != null ) {
                lastmileBuffer = toMatch;
                lastmile = eofMatch;
            } else if ( index == -1 )
                index = RET_EOF;
        }
        
        // We're in the final stretch, but we might have one last closure to run.
        if( lastmile != null ) {
            log.fine("Running last mile");
            
            //TODO provide buffer vars for EOF
            Closure closure = lastmile.getClosure();
            index = pairs.indexOf(lastmile);
            
            ExpectState state = prepareClosure(index, lastmileBuffer);
            try {
                if( closure != null )
                    closure.run(state);
            }catch(Exception e) {
                log.log(Level.WARNING, "In Last Mile Closure", e);
                log.finer("Closure body: " + closure.toString() );
                throw e;
            } finally {
                g_state = state;
            }
        }
        return index;
    }
    
    protected String printBuffer() {
        String javaStr = new String(input.getBuffer());
        javaStr = javaStr.replaceAll("\\r", "\\\\r");
        javaStr = javaStr.replaceAll("\\n", "\\\\n");
        return javaStr;
        
    }
    protected ExpectState prepareClosure(int pairIndex, String buffer) {
        ExpectState state;
        Map prevMap = null;
        if( g_state != null )
            prevMap = g_state.getVars();
        
        state = new ExpectState(pairIndex, buffer, prevMap);
        
        return state;
    }
    
    /**
     * Don't use input, it's match values might have been reset in the loop that looks for the first possible match
     */
    protected ExpectState prepareClosure(int pairIndex, MatchResult result) {
        /*
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
        Map prevMap = null;
        if( g_state != null )
            prevMap = g_state.getVars();
        
        int matchedWhere = result.beginOffset(0);
        String matchedText = result.toString(); // expect_out(0,string)
        
        // Unmatched upto end of match
        // expect_out(buffer)
        char[] chBuffer = input.getBuffer();
        String copyBuffer = new String(chBuffer, 0, result.endOffset(0) );
        
        List /* <String> */ groups = new ArrayList();
        for( int j=1; j <= result.groups(); j++ ) {
            String group = result.group(j);
            groups.add( group );
        }
        state = new ExpectState(copyBuffer.toString(), matchedWhere, matchedText, pairIndex, groups, prevMap);
        
        return state;
    }
    
    /**
     * @returns found something, and ran it. Calling function should use g_state to figure out what to do next
     */
    protected boolean runFirstMatch(List /* <PatternPair> */ pairs) throws Exception {
        MatchResult firstResult = null;
        PatternPair firstPair = null;
        int pairIndex = -1;
        
        ListIterator iter = pairs.listIterator();
        while( iter.hasNext() ) {
            PatternPair pair = (PatternPair) iter.next();
            Pattern pattern = pair.getPattern();
            
            // reset input to begining
            input.setCurrentOffset(input.getBeginOffset());
            
            if( matcher.contains(input, pattern) ) {
                MatchResult result = matcher.getMatch();
                if( firstResult == null || result.beginOffset(0) < firstResult.beginOffset(0) ) {
                    firstResult = result;
                    firstPair = pair;
                    pairIndex = iter.previousIndex(); // TODO confirm this
                }
            }
        }
        
        if( firstResult == null ) {
            return false; // didn't find anything
        }
        
        // Found something
        //input's offset are illegal at this phase
        //input.setCurrentOffset(input.getBeginOffset());
        log.fine("Using a result for " + firstPair.getPattern().getPattern() );
        ExpectState state = prepareClosure(pairIndex, firstResult);
        Closure closure = firstPair.getClosure();
        
        try {
            if( closure != null ) {
                closure.run(state);
            }
        }catch(Exception e) {
            throw e;
        } finally {
            g_state = state;
        }
        return true;
    }
    
    private ExpectState g_state = null;
    public ExpectState getLastState() {
        // Very possibly null from an ONCE_CHANCE or initial state
        // But since the expected use is to call expect4j.getLastState().getMatch(), we want to help protect against NPE
        return ( g_state != null )?g_state:new ExpectState();
    }
    
    /**
     * Provide no default timeout
     */
    public static final long TIMEOUT_NOTSET = -2;
    
    /**
     * Never timeout, wait forever
     */
    public static final long TIMEOUT_FOREVER = -1;
    
    /**
     * Don't give timeout a chance. If nothing is found right away, stop checking.
     */
    public static final long TIMEOUT_NEVER = 0;
    
    public final static long TIMEOUT_DEFAULT = 10 * 1000;
    long defaultTimeout = TIMEOUT_DEFAULT;
    
    /**
     * Default Timeout.
     * @arg timeout milliseconds
     * TODO Check for valid values
     */
    public void setDefaultTimeout(long timeout) {
        log.finer("Setting Default Timeout to " + timeout);
        defaultTimeout = timeout;
    }
    
    protected TimeoutMatch findTimeout(Match matches[]) {
        TimeoutMatch ourTimeout = null;
        for(int i=0; i < matches.length; i++) {
            if( matches[i] instanceof TimeoutMatch )
                ourTimeout = (TimeoutMatch) matches[i];
        }
        /*
        if( ourTimeout == null ) {
            // Have to create our own
            ourTimeout = new TimeoutMatch(null);
        }
         */
        return ourTimeout;
    }
    
    protected EofMatch findEof(Match matches[]) {
        EofMatch ourEof = null;
        for(int i=0; i < matches.length; i++) {
            if( matches[i] instanceof EofMatch )
                ourEof = (EofMatch) matches[i];
        }
        
        if( ourEof == null ) {
            // Have to create our own
            ourEof = new EofMatch();
        }
        
        return ourEof;
    }
    
    public void close() {
        log.fine("Closing");
        consumer.stop();
    }
}
