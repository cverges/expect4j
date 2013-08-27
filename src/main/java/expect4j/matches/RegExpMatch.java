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

package expect4j.matches;

import expect4j.*;
import org.apache.oro.text.regex.*;

/**
 * Simulates "expect { -regexp {..*} { code } }
 *
 * @author justin
 */
public class RegExpMatch extends PatternPair {

    /** Creates a new instance of RegExpMatch */
    public RegExpMatch(String patternStr, Closure closure) throws MalformedPatternException {
        super(patternStr, closure);
    }
    
    //TODO: removed static keyword, since Perl5Compiler is not threadsafe
    //
    protected Perl5Compiler compiler;
    public Perl5Compiler getCompiler() {
        if( compiler == null)
            compiler = new Perl5Compiler();
        return compiler;
    }
    
    public Pattern compilePattern(String patternStr) throws MalformedPatternException  {
        Perl5Compiler compiler = getCompiler();
        return compiler.compile(patternStr, Perl5Compiler.DEFAULT_MASK|Perl5Compiler.SINGLELINE_MASK); // |Perl5Compiler.MULTILINE_MASK
    }
}
