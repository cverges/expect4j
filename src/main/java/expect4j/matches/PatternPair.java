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
 *
 * @author justin
 */
public abstract class PatternPair extends Match {
    
    String patternStr;
    Pattern pattern;        
    
    /**
     * Creates a new instance of PatternPair
     */
    public PatternPair(String patternStr, Closure closure) throws MalformedPatternException {
        super(closure);
        this.patternStr = patternStr;
        pattern = compilePattern(patternStr);
    }        
    
    abstract public Pattern compilePattern(String patternStr) throws MalformedPatternException;
    
    public Pattern getPattern() {
        return pattern;
    }
}
