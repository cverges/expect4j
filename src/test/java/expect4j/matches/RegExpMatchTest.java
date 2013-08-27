/*
 * RegExpMatchTest.java
 * JUnit based test
 *
 * Created on December 30, 2006, 5:18 PM
 */

package expect4j.matches;

import junit.framework.*;
import expect4j.*;
import org.apache.oro.text.regex.*;

/**
 *
 * @author justin
 */
public class RegExpMatchTest extends TestCase {
    
    public RegExpMatchTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testPattern() throws Exception {
        System.out.println("pattern");
        
        String buffer = "The quick brown fox jumps over the lazy dog";
        RegExpMatch instance = null;
        
        instance = new RegExpMatch(buffer, null); // Basic String
        
        Pattern result = instance.getPattern();
        assertEquals(buffer, result.getPattern());        
    }

    public void testPatterns() throws Exception {
        System.out.println("patterns");
        
        // Original string: "User=(.+) Date=(.+) Time=(\[^\r]+)\r"
        // Obj:             "User=(.+) Date=(.+) Time=([^\r]+)"
        // Pattern:         "User=(.+) Date=(.+) Time=([^]+)"
        // Invalid          "User=(.+) Date=(.+) Time=([^]+)"
        String pattern = "User=(.+) Date=(.+) Time=([^\r]+)\r";
        RegExpMatch instance = null;
        
        instance = new RegExpMatch(pattern, null); // Basic String
        
        Pattern result = instance.getPattern();
        System.out.println(result);
        assertEquals(pattern, result.getPattern());        
    }
        
    public void testRegexp() throws Exception {
        System.out.println("match");
        Perl5Matcher matcher = new Perl5Matcher();
        
        String buffer = "The quick brown fox jumps over the lazy dog";
        RegExpMatch instance = null;
        
        Perl5Compiler compiler = new Perl5Compiler();
        Pattern pattern = compiler.compile("ju.ps");
        assertTrue( matcher.contains(buffer, pattern) );
        
    }
    /**
     * Test of match method, of class expect4j.matches.RegExpMatch.
     */
    public void testMatch() throws Exception {
        System.out.println("match");
        Perl5Matcher matcher = new Perl5Matcher();
        
        String buffer = "The quick brown fox jumps over the lazy dog";
        RegExpMatch instance = null;
        
        instance = new RegExpMatch("(.*) jumps over the lazy dog", null); // Basic String
        Pattern pattern = instance.getPattern();
        assertTrue( matcher.contains(buffer, pattern) );
        
        MatchResult result = matcher.getMatch();
        String expResult = "The quick brown fox";
        assertEquals(buffer, result.group(0) );

        pattern = new RegExpMatch("jumx?ps", null).getPattern(); // Basic RegExp
        assertNotNull(pattern);
        assertTrue( matcher.contains(buffer, pattern) );
        result = matcher.getMatch();
        expResult = "jumps";
        assertEquals(expResult, result.group(0) );
        
        pattern = new RegExpMatch("qui[ck]*\\sb.*n", null).getPattern(); // Basic RegExp
        assertTrue( matcher.contains(buffer, pattern) );
        result = matcher.getMatch();        
        expResult = "quick brown";
        System.out.println( "Group 0: [" + result.group(0) + "]");
        assertEquals(expResult, result.group(0));
        
        pattern = new RegExpMatch(".*", null).getPattern(); // Basic RegExp
        assertTrue( matcher.contains(buffer, pattern) );
        result = matcher.getMatch();        
        System.out.println( "Group 0: [" + result.group(0) + "]");
        assertEquals(buffer, result.group(0));

        // "/(\[a-z]+)/(\[0-9]+)/(\[a-z]+)\[\\$|>]"
        // "(\[^\r]*)\n\r"
    }
    public void testMultiLine() throws Exception {
        System.out.println(" test Multiline ");
        
        String buffer = "The quick brown\nfox jumps over\nthe lazy dog";
        
        RegExpMatch instance = new RegExpMatch(".*", null); // Basic String
        
        Pattern pattern = instance.getPattern();
        Perl5Matcher matcher = new Perl5Matcher();
        assertTrue( matcher.contains(buffer, pattern ) );
        
        MatchResult result = matcher.getMatch();        
        System.out.println( "Group 0: [" + result.group(0) + "]");
        assertTrue( result.group(0).length() > 20 );
    }
    
    public void testReplace() throws Exception {
        String pattern1 = "\r(?=[^\n])";
        String pattern2 = "\r$";
        String arg = "\rls\r";
        String result = arg.replaceAll(pattern1, org.apache.commons.net.SocketClient.NETASCII_EOL);
        result = result.replaceAll(pattern2, org.apache.commons.net.SocketClient.NETASCII_EOL);
        assertEquals("\r\nls\r\n", result);

        arg = "\rls\r\n";
        result = arg.replaceAll(pattern1, org.apache.commons.net.SocketClient.NETASCII_EOL);
        result = result.replaceAll(pattern2, org.apache.commons.net.SocketClient.NETASCII_EOL);
        assertEquals("\r\nls\r\n", result);
    }
    
}
