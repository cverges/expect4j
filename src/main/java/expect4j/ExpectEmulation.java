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

import java.text.StringCharacterIterator;
import java.util.logging.Level;
import tcl.lang.*;
import expect4j.matches.*;
import org.apache.oro.text.regex.MalformedPatternException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Register commands to support the Expect API
 *
 * Commands:
 * exp_continue
 * exp_internal 0;
 * expect
 * log_user 0;
 * send " ";
 * send -- "$command\r";
 * sleep
 * spawn
 * stty -echo;
 * stty echo;
 * timestamp
 *
 * Variables:
 * expect_out(1,string)
 *  through
 * expect_out(5,string)
 * expect_out(buffer)
 * spawn_id
 */
public class ExpectEmulation extends Extension {
    static final public Logger log = Logger.getLogger(ExpectEmulation.class.getName());
    
    //@Overrides
    public void init(Interp interp) {
        interp.createCommand("exp_continue", new ExpContinueCommand());
        interp.createCommand("exp_internal", new ExpInternalCommand());
        interp.createCommand("expect", new ExpectCommand());
        interp.createCommand("log_user", new LogUserCommand());
        interp.createCommand("send", new SendCommand());
        interp.createCommand("sleep", new SleepCommand());
        interp.createCommand("spawn", new SpawnCommand());
        interp.createCommand("stty", new SttyCommand());
        interp.createCommand("timestamp", new TimestampCommand());
        
        //interp.eval("rename subst ::tcl::subst");
        //interp.createCommand("subst", new tcl.lang.SubstCrCommand() );
        interp.createCommand("substcr", new tcl.lang.SubstCrCommand() );
        
        try {
            interp.pkgProvide("expect", "2.0"); // closest equivalent
        }catch(TclException te) {
            log.log( Level.WARNING, te.getMessage(), te);
        }
    }
    
    /**
     * expect [[-opts] pat1 body1] ... [-opts] patn [bodyn]
     *
     * TODO Fully integrate with Expect4j
     * TODO upvar the whole closure when running
     * TODO set expect_out array
     */
    public class ExpectCommand implements Command {
        void releaseClosures(Interp interp, Collection preserved) {
            Iterator iter = preserved.iterator();
            while( iter.hasNext() ) {
                TclObject tclCode = (TclObject) iter.next();
                tclCode.release();
            }
        }
        
        public void cmdProc(Interp interp, TclObject args[]) throws TclException {
            // Do not allow empty expect statement
            if (args.length != 2)
                throw new TclNumArgsException(interp, 0, args, "expect {[-opts] pat1 body1] ... [-opts] patn [bodyn]}");
            
            TclObject argArr = args[1];
            TclObject argv[] = TclList.getElements(interp, argArr);
            
            List /* <Pair> */ pairs = new ArrayList();
            int i=0;
            String arg;
            log.info("Looking at expect args");
            Match pair;
            Collection preserved = new ArrayList(argv.length - i);
            
            while( i < argv.length ) {
                arg = argv[i].toString();
                //log.info(i + " Looking at " + arg);
                
                if( arg.equals("timeout") ) {
                    if( i + 1 >= argv.length )
                        throw new TclNumArgsException(interp, i, argv, "expect [[-opts] pat1 body1] ... [-opts] patn [bodyn]");
                    TclObject tclCode = argv[++i];
                    TclClosure closure = new TclClosure(interp, tclCode);
                    pair = new TimeoutMatch( closure );
                    
                    log.finer("Adding Timeout Match");
                    
                    pairs.add( pair );
                } else if( arg.equals("eof") ) {
                    if( i + 1 >= argv.length )
                        throw new TclNumArgsException(interp, i, argv, "expect [[-opts] pat1 body1] ... [-opts] patn [bodyn]");
                    TclObject tclCode = argv[++i];
                    TclClosure closure = new TclClosure(interp, tclCode);
                    pair = new EofMatch( closure );
                    
                    log.finer("Adding Eof Match");
                    
                    pairs.add( pair );
                } else {
                    TclObject patternObj;
                    if( arg.startsWith("-") ) {
                        // TODO do NumArgs check
                        patternObj = argv[++i];
                    } else {
                        patternObj = argv[i];
                        arg = "-gl"; // default to glob
                    }
                    
                    String javaStr = patternObj.toString();
                    /*
                    StringBuffer hexString = new StringBuffer();
                    for(int j=0; j < javaStr.length(); j++ ) {
                        hexString.append( ((int) javaStr.charAt(j)) + ",");
                    }
                    log.info(i + " Characters " + hexString);
                     */
                    javaStr = javaStr.replaceAll("\\r", "\\\\r");
                    javaStr = javaStr.replaceAll("\\n", "\\\\n");
                    //interp.eval("subst -nobackslashes -nocommands {" + patternObj.toString() + "}", 0);
                    
                    Command substCmd = interp.getCommand("substcr");
                    TclObject substArgv[] = new TclObject[] {
                        TclString.newInstance("substcr"),
                        TclString.newInstance("-nocommands"),
                        TclString.newInstance("-nobackslashes"),
                        patternObj
                    };
                    substCmd.cmdProc(interp, substArgv);
                    
                    TclObject substPatternObj = interp.getResult();
                    String pattern = substPatternObj.toString();
                    
                    TclClosure closure = null;
                    TclObject tclCode = null;
                    if( i + 1 < argv.length ) {
                        TclObject tclCodeDirect = argv[++i];
                        tclCodeDirect.preserve();
                        preserved.add(tclCodeDirect);
                        tclCode = tclCodeDirect;
                    }
                    closure = new TclClosure(interp, tclCode);
                    
                    log.info(i + " Pattern Obj is >>" + javaStr + "<< and pattern is >>>" + pattern + "<<<");
                    try {
                        if( arg.startsWith("-re") ) {
                            // In TCL \[ is used to tell TCL that this is not a nested command
                            /* Need to support:
                             * -re "User=(.+) Date=(.+) Time=(\[^\r]+)\r"
                             * -re "\[Pp]assword: "
                             * -re "/(\[a-z]+)/(\[0-9]+)/(\[a-z]+)\[\\$|>]"
                             * -re "/(\[a-z]+)/(\[0-9]+)/(\[a-z]+)>"
                             * -re "\\$ |....> "
                             * -re "(\[^\n]*)\r\n"
                             * -re "(\[^\r]*)\r\n"
                             */
                            pair = new RegExpMatch(pattern, closure);
                        } else if ( arg.startsWith("-ex") )
                            throw new TclException(interp, "Exact matches not supported yet");
                        else if( arg.startsWith("-gl") ) {
                            pair = new GlobMatch(pattern, closure);
                            log.info(i + " Glob at regexp " + ((GlobMatch) pair).getPattern().getPattern() );
                        } else {
                            throw new TclException(interp, "Unknown type of pattern");
                        }
                    } catch(TclException te) {
                        releaseClosures(interp, preserved);
                        throw te;
                    } catch(MalformedPatternException mpe) {
                        releaseClosures(interp, preserved);
                        throw new TclException(interp, "Invalid pattern: " + pattern);
                    }
                    pairs.add( pair );
                }
                i++;
                
            } // end while
            
            // Lookup Expect
            Expect4j expect4j = expStateCurrent(interp);
            
            // Timeout
            try {
                TclObject timeoutObj = interp.getVar("timeout", null, 0);
                int timeout = TclInteger.get(interp, timeoutObj);
                expect4j.setDefaultTimeout(timeout * 1000);
            }catch(Exception e) {
                expect4j.setDefaultTimeout( Expect4j.TIMEOUT_DEFAULT );
            }
            
            boolean isDebug = isExpDebug(interp);
            boolean isEcho = isEcho(interp);
            boolean isLogUser = isLogUser(interp);
            // TODO
            //expect4j.setDebugLevels(isDebug, isEcho, isLogUser);
            
            // Run Expect
            int ret;
            try {
                ret = expect4j.expect(pairs);
            } catch(TclException te) {
                throw te;
            } catch(Exception e) {
                throw new TclException(interp, e.getMessage() );
            } finally {
                releaseClosures(interp, preserved);
            }
            
            interp.setResult(ret);
        }
    }
    
    /* Commands in alphabetical order*/
    
    /**
     * exp_continue
     *
     * TODO Make calling Closure check for continue var
     */
    public class ExpContinueCommand implements Command {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            if (argv.length != 1)
                throw new TclNumArgsException(interp, 0, argv, "");
            
            setExpContinue(interp, true);
            // TODO immeadiately set debug level on Expect4j object
        }
    }
    
    /**
     * exp_internal [-f file] [-info] [0|1]
     *
     * TODO find out how isExpDebug would be called
     */
    public class ExpInternalCommand implements Command {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            if (argv.length != 2)
                throw new TclNumArgsException(interp, 0, argv, "[0|1]");
            
            String arg = argv[1].toString();
            if( arg.equals("1") )
                setExpDebug(interp, true);
            else if( arg.equals("0") )
                setExpDebug(interp, false);
            else
                throw new TclNumArgsException(interp, 0, argv, "[0|1]");
        }
    }
    
    /**
     * log_user [0|1]
     *
     * TODO find out how isLogUser would be called
     */
    public class LogUserCommand implements Command {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            if (argv.length != 2)
                throw new TclNumArgsException(interp, 0, argv, "[0|1]");
            
            String arg = argv[1].toString();
            if( arg.equals("1") )
                setLogUser(interp, true);
            else if( arg.equals("0") )
                setLogUser(interp, false);
            else
                throw new TclNumArgsException(interp, 0, argv, "[0|1]");
            
            // TODO immeadiately set debug level on Expect4j object
        }
    }
    
    /**
     * Send
     */
    public class SendCommand implements Command {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            log.finest("Send Command arg # " + argv.length);
            if (argv.length == 1)
                throw new TclNumArgsException(interp, 1, argv, "[-flags] [--] string");
            
            boolean checkingFlags = true;
            int i=1;
            String arg = null;
            for(; i < argv.length; i++) {
                arg = argv[i].toString();
                if( checkingFlags && arg.equals("--") ) {
                    checkingFlags = false;
                    continue;
                }
                // skip flags
                if( checkingFlags && arg.startsWith("-") )
                    continue;
            }
            
            // Compensate for the added i++
            if (i - 1 == argv.length)
                throw new TclNumArgsException(interp, 1, argv, "[-flags] [--] string");
            
            //arg = interp.convertStringCRLF(arg); // TODO confirm is CRLF removal is necessary
            String pattern1 = "\r(?=[^\n])";
            String pattern2 = "\r$";
            arg = arg.replaceAll(pattern1, org.apache.commons.net.SocketClient.NETASCII_EOL);
            arg = arg.replaceAll(pattern2, org.apache.commons.net.SocketClient.NETASCII_EOL);
        
            Expect4j expect4j = expStateCurrent(interp);
            
            
            try {
                expect4j.send(arg);
            }catch(Exception ioe) {
                throw new TclException(interp, "Unable to send " + arg);
            }
            
            if( isEcho(interp) )
                System.out.println(arg);
        }
    }
    
    /**
     * sleep seconds
     */
    public class SleepCommand implements Command {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            if (argv.length > 2)
                throw new TclNumArgsException(interp, 0, argv, "seconds");
            
            int seconds = TclInteger.get(interp, argv[1]);
            try {
                Thread.sleep( seconds * 1000 );
            } catch(InterruptedException e) {
            }
        }
    }
    
    /**
     * spawn [args] program [args]
     * Called with possible local paths, like C:\Windows\ssh.exe.
     */
    public class SpawnCommand implements Command, VarTrace {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            if (argv.length == 1)
                throw new TclNumArgsException(interp, 0, argv, "[args] program [args]");
            
            // Skip the -args, e.g. -console, -ignore, -leaveopen, etc
            int i = 1;
            while( i < argv.length && argv[i].toString().indexOf('-') == 0 ) {
                i++;
            }
            
            if( i == argv.length ) // missing program arg
                throw new TclNumArgsException(interp, i, argv, "[args] program [args]");
            String program = argv[i++].toString().trim();
            
            // Determine if we're running ssh or telnet and create Pair
            Expect4j expect4j;
            if( program.indexOf("ssh") != -1 ) {
                // SSH
                // Command will look like:
                // spawn $path(ssh) -l fieldsvc $chassis(scpAddr)
                String username = null;
                String password = null;
                while( argv[i].toString().indexOf('-') == 0) {
                    // process arg
                    log.fine("ssh arg: " + argv[i].toString() );
                    if( argv[i].toString().equals("-l") && i + 1 < argv.length - 1) {
                        username = argv[++i].toString();
                    }
                    if( argv[i].toString().equals("-P") && i + 1 < argv.length - 1) {
                        password = argv[++i].toString();
                    }
                    i++;
                }
                
                if( username == null)
                    throw new TclException(interp, "Username needs to be provided");
                else
                    log.finer("Username: " + username);
                
                if( i >= argv.length )
                    throw new TclNumArgsException(interp, i - 1, argv, "[-l username] [-P password] hostname");
                String hostname = argv[i].toString().trim();
                
                try {
                    expect4j = ExpectUtils.SSH( hostname, username, password );
                }catch(Exception e) {
                    e.printStackTrace();
                    throw new TclException(interp, "Unable to connect using SSH");
                }
            } else if( program.indexOf("telnet") == 0 ) {
                // Telnet
                // spawn telnet $chassis(scpAddr) 7653
                String hostname = null;
                String portStr = null;
                if( i >= argv.length )
                    throw new TclNumArgsException(interp, i, argv, "hostname port" );
                hostname = argv[i++].toString().trim();
                
                if( i < argv.length )
                    portStr = argv[i++].toString().trim();
                
                int port = 23;
                if( portStr != null ) {
                    try {
                        port = Integer.parseInt(portStr);
                    }catch(NumberFormatException e) {
                        throw new TclException(interp, "Unable to parse the port number");
                    }
                }
                
                try {
                    expect4j = ExpectUtils.telnet( hostname, port);
                }catch(Exception e) {
                    throw new TclException(interp, "Unable to connect using Telnet");
                }
            } else {
                // Unsupported
                String[] cmdarray;
                int cmdidx = 0;
                int addlargs = argv.length - i + 1; 
                
                // Guess OS
                String OS = System.getProperty("os.name").toLowerCase();
                if (OS.indexOf("windows 9") > -1) {
                    // Windows 98
                    cmdarray = new String[ addlargs + 2];
                    cmdarray[cmdidx++] = "command.com";
                    cmdarray[cmdidx++] = "/c";
                } else if ( OS.indexOf("windows") > -1 ) {
                    // NT
                    cmdarray = new String[ addlargs + 2];
                    cmdarray[cmdidx++] = "cmd.exe";
                    cmdarray[cmdidx++] = "/c";
                } else {
                    // Unix
                    cmdarray = new String[ addlargs];
                }
                
                // Append actual command and its args
                cmdarray[cmdidx++] = program;
                for(;i < argv.length; i++) {
                    cmdarray[cmdidx++] = argv[i].toString().trim();
                }
                
                try {
                    Process process = Runtime.getRuntime().exec(cmdarray);
                    expect4j = new Expect4j(process);
                }catch(Exception e) {
                    throw new TclException(interp, "Unable to start arbitary process");
                }
            }
            
            // Load and store lastSpawnId
            int nextId = 1;
            IntegerAssocData lastSpawnId = (IntegerAssocData) interp.getAssocData("lastSpawnId");
            if( lastSpawnId != null )
                nextId = lastSpawnId._i.intValue() + 1;
            lastSpawnId = new IntegerAssocData(nextId);
            interp.setAssocData("lastSpawnId", lastSpawnId);
            
            // register spawn_id with list
            MapAssocData spawnIds = (MapAssocData) interp.getAssocData("spawnIds");
            if( spawnIds == null ) {
                spawnIds = new MapAssocData();
            }
            
            String spawnId = lastSpawnId._i.toString();
            log.finer("Putting id in " + spawnId);
            spawnIds.put( spawnId, expect4j );
            interp.setAssocData("spawnIds", spawnIds);
            
            // register spawn_id with interp
            TclObject spawnIdObj = TclInteger.newInstance( nextId );
            interp.setVar("spawn_id", spawnIdObj, 0);
            interp.traceVar("spawn_id", this, 0);
            
            interp.setResult(spawnIdObj);
        }
        
        public void traceProc(Interp interp, String name1, String name2, int flags) {
            log.info("Tracing");
            if ( (flags & TCL.TRACE_DESTROYED) != 0 )
                log.warning("Trace Destroyed");
            if ( (flags & TCL.INTERP_DESTROYED) != 0 )
                log.warning("Interp Destroyed");
        }
    }
    
    /**
     * timestamp [-seconds NNN] [-gmt] [-format format]
     *
     * TODO support formattings
     */
    public class TimestampCommand implements Command {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            if (argv.length > 1)
                throw new TclNumArgsException(interp, 0, argv, "");
            
            long epoch = new Date().getTime();
            if( epoch >= Double.MAX_VALUE )
                throw new TclException(interp, "Epoch is too large to convert to double");
            
            double epochd = new Long(epoch).doubleValue();
            TclObject result = TclDouble.newInstance(epochd);
            interp.setResult( result );
        }
    }
    
    /**
     * stty [-echo|echo]
     *
     * Only called in ask, which shouldn't be called in automated script mode
     */
    public class SttyCommand implements Command {
        public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
            if (argv.length > 2)
                throw new TclNumArgsException(interp, 0, argv, "[echo|-echo]");
            
            String cmd = argv[1].toString();
            log.info("stty cmd is " + cmd);
            if( cmd.equals("echo") )
                setEcho(interp, true);
            else if( cmd.equals("-echo") )
                setEcho(interp, false);
            else
                throw new TclException(interp, "Only echo is supported");
            
            // TODO immeadiately set debug level on Expect4j object
            
        }
    }
    
    public class IntegerAssocData implements AssocData {
        public Integer _i;
        public IntegerAssocData(int value) {
            _i = new Integer(value);
        }
        
        public IntegerAssocData(String s) {
            _i = new Integer(s);
        }
        
        public void disposeAssocData(Interp interp) {
        }
    }
    
    public class MapAssocData extends HashMap implements AssocData {
        public void disposeAssocData(Interp interp) {
        }
    }
    
    /* TODO change return type to Expect4j */
    public static Expect4j expStateCurrent(Interp interp) throws TclException {
        TclObject spawnObj = interp.getVar("spawn_id", 0); // confirm that this works and we don't need TCL.NAMESPACE_ONLY
        
        Map spawnIds = (Map) interp.getAssocData("spawnIds");
        if( spawnIds == null )
            throw new TclException(interp, "spawn not called yet");
        
        String spawnId = spawnObj.toString();
        if( !spawnIds.containsKey(spawnId) )
            throw new TclException(interp, "Unable to find spawn_id of " + spawnId);
        
        Expect4j expect4j = (Expect4j) spawnIds.get(spawnId);
        if( expect4j == null )
            throw new TclException(interp, "Unable to find Expect context from " + spawnId);
        
        return expect4j;
    }
    
    public static boolean isVar(Interp interp, String varname) throws TclException {
        TclObject obj = null;
        try {
            obj = interp.getVar(varname, null, TCL.GLOBAL_ONLY);
        }catch(Exception e) {
            return false;
        }
        
        if ( obj == null) {
            return false;
        }
        
        return TclBoolean.get(interp, obj);
    }
    public static void setBooleanVar(Interp interp, String varname, boolean value) throws TclException {
        if( isVar(interp, varname) == value)
            return;
        
        TclObject obj = TclBoolean.newInstance(value);
        log.finest("Setting " + varname + " to " + Boolean.toString(value) );
        interp.setVar(varname, obj, TCL.GLOBAL_ONLY);
    }
    
    public static boolean isEcho(Interp interp) throws TclException {
        return isVar(interp, "exp_tty_echo");
    }
    public static void setEcho(Interp interp, boolean setEcho) throws TclException {
        setBooleanVar(interp, "exp_tty_echo", setEcho);
    }
    
    public static boolean isLogUser(Interp interp) throws TclException {
        return isVar(interp, "exp_log_user");
    }
    public static void setLogUser(Interp interp, boolean setLogUser) throws TclException {
        setBooleanVar(interp, "exp_log_user", setLogUser);
    }
    
    public static boolean isExpDebug(Interp interp) throws TclException {
        return isVar(interp, "exp_debug");
    }
    public static void setExpDebug(Interp interp, boolean setExpDebug) throws TclException {
        setBooleanVar(interp, "exp_debug", setExpDebug);
    }
    
    public static boolean isExpContinue(Interp interp) throws TclException {
        return isVar(interp, "exp_continue_set");
    }
    public static void setExpContinue(Interp interp, boolean setExpContinue) throws TclException {
        setBooleanVar(interp, "exp_continue_set", setExpContinue);
    }
    
    public static String escape(final String value) {
        String raw = value;
        boolean isString = false;
        
        if( value.indexOf('"') == 0 && value.lastIndexOf('"') == value.length() - 1) {
            isString = true;
            raw = value.substring(1, value.length() - 1);
        }
        
        final StringBuffer result = new StringBuffer();
        
        StringCharacterIterator iterator = new StringCharacterIterator(raw);
        char character = iterator.current();
        while (character != StringCharacterIterator.DONE ){
            /*
             * All literals need to have backslashes doubled.
             * &;`'"|*?~<>^()[]{}$\
             */
            switch( character ) {
                case '&':
                case ';':
                case '\'':
                case '"':
                case '|':
                case '*':
                case '?':
                case '~':
                case '<':
                case '>':
                case '^':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '$':
                case '\\': result.append("\\");
                default: result.append(character);
            }
            character = iterator.next();
        }
        String clean = result.toString();
        if( isString )
            clean = '"' + clean + '"';
        
        return clean;
    }
    
}
