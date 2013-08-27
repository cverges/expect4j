/*
 * Expect4jTest.java
 * JUnit based test
 *
 * Created on March 10, 2007, 4:26 PM
 */

package expect4j;

import junit.framework.*;
import expect4j.matches.*;
import java.io.*;
import java.util.*;
import org.apache.oro.text.regex.*;

/**
 *
 * @author justin
 */
public class Expect4jTest extends TestCase {
    IOPair pair;
    final String testStr = "The quick brown fox jumps over the lazy dog";
    
    public Expect4jTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
        pair = new StringPair(testStr);
    }
    
    protected void tearDown() throws Exception {
        pair.close();
    }
    
    public void testSend() throws Exception {
        Expect4j.log.info("send");
        
        String str = "Sent";
        Expect4j instance = new Expect4j(pair);
        
        instance.send(str);
        
        if( pair instanceof StringPair ) {
            String result = ((StringPair) pair).getResult();
            assertEquals(str, result);
        }
    }
    
    public void testExpectPattern() throws Exception {
        Expect4j.log.info("expect pattern");
        
        Expect4j instance = new Expect4j(pair);
        
        int index = instance.expect("quick");
        assertEquals(0, index);
        
        String match = instance.getLastState().getMatch();
        assertEquals("quick", match);
        
        String buffer = instance.getLastState().getBuffer();
        System.out.println("Buffer: " + buffer);
        assertEquals("The quick", buffer);
        
        // Try buffer on second match
        index = instance.expect("lazy");
        assertEquals(0, index);
        
        match = instance.getLastState().getMatch();
        assertEquals("lazy", match);
        
        buffer = instance.getLastState().getBuffer();
        System.out.println("Buffer: " + buffer);
        assertEquals(" brown fox jumps over the lazy", buffer);
    }
                    
    public void testPatternExact() throws Exception {
        String matchStr = "\r\necho \"User=Unknown Date=Mar 29, 2007 Time=17:03PM EDT\"\r";
        Expect4j.log.info("expect pattern");
        
        Expect4j instance = new Expect4j( new StringPair(matchStr) );
        
        int index = instance.expect("echo \"User=Unknown Date=Mar 29, 2007 Time=17:03PM EDT\"\r");
        assertEquals(0, index);
        
        String match = instance.getLastState().getMatch();
        System.out.println("Match: " + match);
        //assertEquals("quick", match);
        
        String buffer = instance.getLastState().getBuffer();
        System.out.println("Buffer: " + buffer);
        //assertEquals("The quick", buffer);
    }

    public void testPatternCR() throws Exception {
        String matchStr = "the quick fox\njumps over ";
        Expect4j.log.info("\n\nexpect pattern");
        
        Expect4j instance = new Expect4j( new StringPair(matchStr) );
        
        Thread.sleep(1000);
        
        int index = instance.expect("*");
        assertEquals(0, index);
        
        String match = instance.getLastState().getMatch();
        Expect4j.log.warning("Match: " + match);
        assertTrue( match.length() > 5 );
        
        String buffer = instance.getLastState().getBuffer();
        Expect4j.log.warning("Buffer: " + buffer);
        assertTrue( buffer.length() == match.length() );
    }
    
    public void testExpectClosure() throws Exception {
        Expect4j.log.info("expect closure");
        
        Expect4j instance = new Expect4j(pair);
        
        final StringBuffer buffer = new StringBuffer();
        
        Closure closure = new Closure() {
            public void run(ExpectState state) {
                buffer.append("In Closure");
            }
        };
        
        instance.expect("quick", closure);
        
        assertEquals("In Closure", buffer.toString() );
    }
    
    public void testClosureVars() throws Exception {
        Expect4j.log.info("expect closur vars");
        
        Expect4j instance = new Expect4j(pair);
        
        Closure closure = new Closure() {
            public void run(ExpectState state) {
                state.addVar("inside", "out");
            }
        };
        
        List pairs = new ArrayList();
        pairs.add( new RegExpMatch("quick\\s(.{5}\\s)(f[oO]T?x)", closure) ); //"quick\\s(.{5}\\s)(f?x)"
        
        int index = instance.expect(pairs);
        
        assertEquals(0, index); // make sure we matched at all first
        
        ExpectState state = instance.getLastState();
        
        String result = state.getMatch();
        assertEquals("quick brown fox", result );
        
        result = state.getMatch(0);
        assertEquals( "quick brown fox", result );
        
        result = state.getMatch(1);
        assertEquals( "brown ", result );
        
        result = state.getMatch(2);
        assertEquals( "fox", result );
        
        result = (String) state.getVar("inside");
        assertEquals("out", result );
    }
    
    public void testPairIndex() throws Exception {
        Expect4j.log.info("expect pair index");
        
        Expect4j instance = new Expect4j(pair);
        instance.setDefaultTimeout(2000); // quicken our testing
        
        List pairs = new ArrayList();
        pairs.add( new GlobMatch("excited", null) );
        pairs.add( new RegExpMatch("lazy", null) );
        pairs.add( new GlobMatch("brown", null) );
        
        int index = instance.expect(pairs);
        
        assertEquals(2, index);
        
        index = instance.getLastState().getPairIndex();
        assertEquals(2, index);
    }
    
    /**
     * EOF
     */
    public void testEof() throws Exception {
        Expect4j.log.info("\n\n\n\nexpect eof");
        
        pair = new DelayedPair(testStr, 5, 0); // should hit EOF quickly
        
        Expect4j instance = new Expect4j(pair);
        instance.setDefaultTimeout(2000);
        
        int index = instance.expect("excited");
        
        assertEquals(Expect4j.RET_EOF, index );
        
    }
    
    /**
     * Grab multiple blocks and then grab until EOF
     * 
     * @throws java.lang.Exception
     */
    public void testEofMultiple() throws Exception {
        Expect4j.log.info("\n\n\n\nexpect eof multiple");
        
        pair = new DelayedPair(testStr, 5, 0); // should hit EOF quickly
        Expect4j instance = new Expect4j(pair);
        instance.setDefaultTimeout(2000);
                
        // "The quick brown fox jumps over the lazy dog"
        List pairs = new ArrayList();
        pairs.add( new GlobMatch("The quick", new Closure() {
            public void run(ExpectState state) {
                state.exp_continue();
            }
        }));
        pairs.add( new GlobMatch("brown fox",  new Closure() {
            public void run(ExpectState state) {
                state.exp_continue();
            }
        }));
        pairs.add( new GlobMatch("jumps over",  new Closure() {
            public void run(ExpectState state) {
                state.exp_continue();
            }
        }));
        pairs.add( new EofMatch() );
        
        int index = instance.expect(pairs);
        assertEquals(3, index );
        
        ExpectState state = instance.getLastState(); // From last match to EOF
        String buffer = state.getBuffer();
        String expResult = " the lazy dog";
        Expect4j.log.info("EOF leftovers " + buffer);
        assertEquals(expResult, buffer);
    }
    
    public void testTimeout() throws Exception {
        Expect4j.log.info("\n\n\n\nexpect timeout");
        
        pair = new DelayedPair(testStr, 1000, 5);
        
        Expect4j instance = new Expect4j(pair);
        
        instance.setDefaultTimeout(2000);
        
        long start = System.currentTimeMillis();
        int index = instance.expect("excited");
        long end = System.currentTimeMillis();
        
        assertEquals(Expect4j.RET_TIMEOUT, index );
        
        long duration = end - start;
        Expect4j.log.fine(" took " + duration + " seconds");
        assertTrue( duration < 3000 ); // less than 2 seconds plus 1 second of cruft
    }
    
    //Timeout Match
    public void testTimeoutMatch() throws Exception {
        Expect4j.log.info( "expect timeout match");
        
        pair = new DelayedPair(testStr, 500, 5); //way longer than 3
        Expect4j instance = new Expect4j(pair);
        instance.setDefaultTimeout(2);
        
        long start = System.currentTimeMillis();
        
        final StringBuffer buffer = new StringBuffer();
        
        List pairs = new ArrayList();
        pairs.add( new GlobMatch("hot", null) );
        pairs.add( new GlobMatch("air", null) );
        pairs.add( new TimeoutMatch(1000, new Closure() {
            public void run(ExpectState state) {
                state.addVar("in", "out");
                buffer.append("In Closure");
            }
        }));
        pairs.add( new GlobMatch("ballon", null) );
        
        int index = instance.expect(pairs);
        long end = System.currentTimeMillis();
        
        assertEquals(2, index);
        
        long duration = end - start;
        assertTrue( duration < 2000 ); // less than 1 seconds plus 1 second of cruft
        
        String result = (String) instance.getLastState().getVar("in");
        assertNotNull( result );
        assertEquals( "out", result);
        
        result = buffer.toString();
        assertEquals("In Closure", result);
    }
    
    /**
     * Serial matches
     */
    public void testSerialPatterns() throws Exception {
        Expect4j.log.info( "expect serial patterns");
        
        Expect4j instance = new Expect4j(pair);
        instance.setDefaultTimeout(2000); // quicken our testing
        
        int index = instance.expect("quick");
        assertEquals(0, index);
        
        instance.send("In the middle");
        
        index = instance.expect("brown");
        assertEquals(0, index);
        
        String match = instance.getLastState().getMatch();
        
        assertEquals("brown", match);
    }
    
    public void testArrayArgs() throws Exception {
        Expect4j.log.info( "expect serial patterns");
        Expect4j instance = new Expect4j(pair);
        instance.setDefaultTimeout(2000); // quicken our testing
        
        int index = instance.expect( new Match[] {
            new GlobMatch("quick", null),
            new RegExpMatch("brown", null),
        });
        assertEquals(0, index);        
    }
    
    /**
     * Tests to write:
     * EOF (with and without)
     * exp_continue
     */
}
