/*
 * SubstCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: SubstCmd.java,v 1.4 2005/07/22 04:47:25 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class implements the built-in "subst" command in Tcl. But it doesn't ignore carriage returns.
 */
public class SubstCrCommand implements Command {
    static final private String validCmds[] = {
        "-nobackslashes",
        "-nocommands",
        "-novariables"
    };
    
    static final int OPT_NOBACKSLASHES		= 0;
    static final int OPT_NOCOMMANDS         	= 1;
    static final int OPT_NOVARS			= 2;
    
    /**
     * This procedure is invoked to process the "subst" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException if wrong # of args or invalid argument(s).
     */
    
    public void cmdProc(Interp interp, TclObject argv[])
    throws TclException {
        int currentObjIndex, len, i;
        int objc = argv.length - 1;
        boolean doBackslashes = true;
        boolean doCmds = true;
        boolean doVars = true;
        StringBuffer result = new StringBuffer();
        String s;
        char c;
        
        for (currentObjIndex = 1; currentObjIndex < objc; currentObjIndex++) {
            if (!argv[currentObjIndex].toString().startsWith("-")) {
                break;
            }
            int opt = TclIndex.get(interp, argv[currentObjIndex],
                            validCmds, "switch", 0);
            switch (opt) {
                case OPT_NOBACKSLASHES:
                    doBackslashes = false;
                    break;
                case OPT_NOCOMMANDS:
                    doCmds = false;
                    break;
                case OPT_NOVARS:
                    doVars = false;
                    break;
                default:
                    throw new TclException(interp,
                                    "SubstCrCmd.cmdProc: bad option " + opt
                                    + " index to cmds");
            }
        }
        if (currentObjIndex != objc) {
            throw new TclNumArgsException(interp, currentObjIndex, argv,
                            "?-nobackslashes? ?-nocommands? ?-novariables? string");
        }
        
        /*
         * Scan through the string one character at a time, performing
         * command, variable, and backslash substitutions.
         */
        
        s = argv[currentObjIndex].toString();
        len = s.length();
        i = 0;
        while (i < len) {
            c = s.charAt(i);
            
            if ((c == '[') && doCmds) {
                ParseResult res;
                try {
                    interp.evalFlags = Parser.TCL_BRACKET_TERM;
                    interp.eval(s.substring(i + 1, len));
                    TclObject interp_result = interp.getResult();
                    interp_result.preserve();
                    res = new ParseResult(interp_result,
                                    i + interp.termOffset);
                } catch (TclException e) {
                    i = e.errIndex + 1;
                    throw e;
                }
                i = res.nextIndex + 2;
                result.append( res.value.toString() );
                res.release();
                /**
                 * Removed
                 (ToDo) may not be portable on Mac
                } else if (c == '\r') {
                   i++;
                 */
            } else if ((c == '$') && doVars) {
                ParseResult vres = Parser.parseVar(interp,
                                s.substring(i, len));
                i += vres.nextIndex;
                result.append( vres.value.toString() );
                vres.release();
            } else if ((c == '\\') && doBackslashes) {
                BackSlashResult bs = Interp.backslash(s, i, len);
                i = bs.nextIndex;
                if (bs.isWordSep) {
                    break;
                } else {
                    result.append( bs.c );
                }
            } else {
                result.append( c );
                i++;
            }
        }
        
        interp.setResult(result.toString());
    }
}
