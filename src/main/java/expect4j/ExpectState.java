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

import java.util.*;

/**
 *
 * @author justin
 */
public class ExpectState {
    
    /** Creates a new instance of ExpectState, for a successful match, mostly immutable */
    public ExpectState(String buffer, int matchedWhere, String match, int pairIndex, List groups, Map prevMap) {
        this(pairIndex, buffer, prevMap);
        this.matchedWhere = matchedWhere;
        this.match = match;
        this.groups = groups;
    }
    
    /** Default constructor for times when closure is being run and there isn't a corresponding match. */
    public ExpectState() {
        this(-1, null, null);
    }    
    
    public ExpectState(int pairIndex, String buffer, Map prevMap) {
        this.buffer = buffer;
        this.matchedWhere = -1;
        this.match = null;
        this.pairIndex = pairIndex;
        this.groups = null;
        shouldContinue = false;
        shouldResetTimer = false;

        vars = new HashMap();
        if( prevMap != null )
            vars.putAll(prevMap);
    }
    
    protected int matchedWhere;
    public int getMatchedWhere() {
        return matchedWhere;
    }
    
    /**
     * expect_out(0,string)
     */
    protected String match;
    public String getMatch() { return match; }
    
    /**
     * expect_out(x,string)
     */
    protected List /* <String> */ groups;
    public String getMatch(int groupnum) {
        if( groupnum == 0 )
            return getMatch();
        if( groupnum > groups.size() )
            return null;
        
        return (String) groups.get(groupnum-1);
    }
    

    protected int pairIndex;
    public int getPairIndex() { return pairIndex; }
    
    /**
     * expect_out(buffer)
     */
    protected String buffer;
    public String getBuffer() { return buffer; }
    public void setBuffer(String buffer) { this.buffer = buffer; }
    
    /**
     * exp_continue
     */
    protected boolean shouldContinue;
    protected boolean shouldResetTimer; // TODO prevent closure from setting
    public boolean shouldContinue() { return shouldContinue; }
    public boolean shouldResetTimer() { return shouldResetTimer; }
    
    public void exp_continue() { shouldContinue = true; }
    public void exp_continue_reset_timer() { shouldContinue = true; shouldResetTimer = true; }
    
    /**
     * Closure Variables
     */
    /**
     * Vars are used to set variables to be accessed outside of the expect
     * statement.  In Tcl the closures can reference outside variables, in
     * java they need to be final. Being final could be a problem if you want
     * to do multiple expect statement in the same block, because only the
     * first closure to set the variable would be allowed to.
     */
    protected Map vars;
    public void addVar(String key, Object value) { vars.put(key, value); }
    public Object getVar(String key) { return vars.get(key); }
    Map getVars() { return vars; } // really don't want closures to call this
}