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
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.*;

/**
 * Simulates "expect { -gl {..*} { code } }
 *
 * @author justin
 */
public class GlobMatch extends RegExpMatch {
    
    /** Creates a new instance of RegExpMatch */
    public GlobMatch(String pattern, Closure closure) throws MalformedPatternException {
        super(pattern, closure);
    }
        
    public Pattern compilePattern(String patternStr) throws MalformedPatternException {                
        int globOptions = GlobCompiler.DEFAULT_MASK | GlobCompiler.QUESTION_MATCHES_ZERO_OR_ONE_MASK;
        char [] patternCh = patternStr.toCharArray();
        String perl5PatternStr = GlobCompiler.globToPerl5(patternCh, globOptions);
        
        return super.compilePattern(perl5PatternStr);
    }
}
