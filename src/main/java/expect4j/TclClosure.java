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

import tcl.lang.*;
import java.util.logging.Level;

/**
 *
 * @author justin
 */
public class TclClosure implements Closure {
    static final public java.util.logging.Logger log = java.util.logging.Logger.getLogger(TclClosure.class.getName());
    
    Interp interp;
    TclObject tclCode;
    
    /** Creates a new instance of TclClosure */
    public TclClosure(Interp interp, TclObject tclCode) {
        this.interp = interp;
        this.tclCode = tclCode;
    }
    
    /**
     * Establish certain variables in the TCL interp. These include:
     *
     * expect_out([1-5], string)
     * expect_out(buffer)
     */
    public void run(ExpectState state) throws Exception {
        int flags = 0; // TCL.NAMESPACE_ONLY
        
        // TODO Inject expect object, so that expect wrapper can access it
        // clear previous expect_out
        //interp.unsetVar("expect_out", flags);
        
        String buffer = state.getBuffer();
        log.finer("Setting var expect_out(buffer) to " + buffer);
        interp.setVar("expect_out", "buffer", buffer, flags);
        
        int group = 0;
        while(true) {
            String match = state.getMatch(group);
            String index = group + ",string";
            group++;
            if( match == null )
                break;
            log.finer("Setting var expect_out(" + index +") to " + match);
            interp.setVar("expect_out", index , match, flags);
        }
        
        ExpectEmulation.setExpContinue(interp, false);
        
        if( tclCode != null && tclCode.toString().length() > 0 ) {
            log.info("Running a tcl bit of code: " + tclCode.toString());
            
            try {
                interp.eval(tclCode, 0);
            } catch(TclException e) {
                log.log(Level.INFO, e.toString(), e);
                throw new Exception( interp.getResult().toString(), e);
            }
            if( ExpectEmulation.isExpContinue(interp) ) {
                log.info("Asked to continue");
                state.exp_continue();
            }
        }
        //interp.unsetVar("expect_out", flags);
        
    }
    
    public String toString() {
        if( tclCode != null)
            return tclCode.toString();
        return null;
    }
}
