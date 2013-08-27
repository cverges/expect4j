/*
 * GlobMatchTest.java
 * JUnit based test
 *
 * Created on December 30, 2006, 5:14 PM
 */

package expect4j.matches;

import junit.framework.*;
import expect4j.*;
import org.apache.oro.text.regex.*;

/**
 *
 * @author justin
 */
public class GlobMatchTest extends TestCase {
    
    public GlobMatchTest(String testName) {
        super(testName);
    }
    
    /**
     * Test of match method, of class expect4j.matches.PatternPair.
     */
    public void testMatchGood() throws Exception {
        System.out.println("match good");
        
        String goodMatch = "fox jumps";
        String buffer = "The quick brown fox jumps over the lazy dog";
        PatternPair instance = null;
        
        Perl5Matcher matcher  = new Perl5Matcher();
        
        instance = new GlobMatch(goodMatch, null);
        Pattern pattern = instance.getPattern(); // already compiled
        
        System.out.println( pattern.getPattern() );
        System.out.println( buffer );
        boolean matched = matcher.contains(buffer, pattern);
        assertTrue( matched );
        
        String found = matcher.getMatch().group(0);
        assertEquals(goodMatch, found );
        
    }
    
    public void testMatchBad() throws Exception {
        System.out.println("match bad");
        
        String badMatch = "lamb sleeps";
        String buffer = "The quick brown fox jumps over the lazy dog";
        PatternPair instance = null;
        
        Perl5Matcher matcher  = new Perl5Matcher();
        
        instance = new GlobMatch(badMatch, null);
        Pattern pattern = instance.getPattern();
        assertFalse( matcher.contains(buffer, pattern) );
    }
    
    public void testMatchingFully() throws Exception {
        System.out.println("match fully");
        
        String goodMatch = "fox jumps";
        String buffer = "The quick brown fox jumps over the lazy dog";
        PatternPair instance = null;
        
        Perl5Matcher matcher  = new Perl5Matcher();
        
        instance = new GlobMatch(goodMatch, null);
        Pattern pattern = instance.getPattern(); // already compiled
        
        boolean matched = matcher.contains(buffer, pattern);
        assertTrue(matched);
        
        MatchResult result = matcher.getMatch();
        
        String match = result.group(0);
        assertEquals("fox jumps", match);
        
        int length = result.length();
        assertEquals(goodMatch.length(), length );
        
        int begin = result.beginOffset(0);
        assertEquals(16, begin);
        
        int end = result.endOffset(0);
        assertEquals(25, end);
    }
    
    public void testClosure() throws Exception {
        System.out.println("closure");
        
        GlobMatch instance = null;
        
        final StringBuffer buffer = new StringBuffer();
        
        instance = new GlobMatch( ".*", new Closure() {
            public void run(ExpectState state) throws Exception {
                buffer.append("Success");
            }
        });
        
        // before
        String result = buffer.toString();
        String expResult = "";
        assertEquals(expResult, result);
        
        Closure closure = instance.getClosure();
        closure.run(null);
        
        // after
        result = buffer.toString();
        expResult = "Success";
        System.out.println(result);
        assertEquals(expResult, result);
        
    }
    public void testMatchHi() throws Exception {
        System.out.println("match hi");
        
        String buffer = "philosophic";
        PatternPair instance = null;
        Perl5Matcher matcher  = new Perl5Matcher();
        
        
        String match = "hi*";
        Pattern pattern = new GlobMatch(match, null).getPattern();
        assertTrue( matcher.contains(buffer, pattern) );
        
        MatchResult result = matcher.getMatch();
        System.out.println( result.group(0) );
        assertEquals( "hilosophic", result.group(0) );
        
        match = "hi*hi";
        pattern = new GlobMatch(match, null).getPattern();
        assertTrue( matcher.contains(buffer, pattern) );
        
        result = matcher.getMatch();
        System.out.println( result.group(0) );
        assertEquals( "hilosophi", result.group(0) );
        
        match = "*hi*";
        pattern = new GlobMatch(match, null).getPattern();
        assertTrue( matcher.contains(buffer, pattern) );
        
        result = matcher.getMatch();
        System.out.println( result.group(0) );
        assertEquals( "philosophic", result.group(0) );
        
        buffer = "philosophic\nphilosophic";        
        match = "*";
        pattern = new GlobMatch(match, null).getPattern();
        System.out.println("RE Pattern " + pattern.getPattern());
        assertTrue( matcher.contains(buffer, pattern) );
        
        result = matcher.getMatch();
        System.out.println( result.group(0) );
        assertEquals( "philosophic\nphilosophic", result.group(0) );
    }
    
}
