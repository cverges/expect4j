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

/**
 *
 * @author justin
 */
public class TimeoutMatch extends Match {
        
    long timeout;
    
    /** 
     * Creates a new instance of TimeoutMatch with a default timeout of 
     * ten seconds
     */
    public TimeoutMatch(Closure closure) {
        this(Expect4j.TIMEOUT_NOTSET, closure);
    }

    public TimeoutMatch(long milli, Closure closure) {
        super(closure);
        this.timeout = milli;
    }
    
    public long getTimeout() {
        return timeout;
    }

}
