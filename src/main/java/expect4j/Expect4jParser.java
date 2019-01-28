package expect4j;

import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import org.apache.oro.text.regex.MalformedPatternException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author Emrah Soytekin (emrah.soytekin@gmail.com)
 * <p>
 * Created on Jan, 2019
 */
public class Expect4jParser extends Expect4j{
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(Expect4j.class);

    private static final String COMMAND_LIST = "commandList";
    private static final String EXPECT_KEYWORD = "expect";

    public Expect4jParser(IOPair pair) {
        super(pair);
    }

    public Expect4jParser(Socket socket) throws IOException {
        super(socket);
    }

    public Expect4jParser(InputStream is, OutputStream os) {
        this( new StreamPair(is, os) );
        logger.trace("Created Expect4J instance {} based on InputStream {} and OutputStream {}",this,is,os);
    }

    public Expect4jParser(Process process) {
        super(process);
    }

    public void runScript(String cmd) throws Exception {


        List<Map> parsedCommandList = parse(cmd);

        if(parsedCommandList.isEmpty()){
            throw new Exception("faulty script definition!");
        }

        for(Map m: parsedCommandList){
            if(m.get(EXPECT_KEYWORD) == null) {
                final List<String> sendCommandList = (List<String>) m.get(COMMAND_LIST);
                if(sendCommandList != null){
                    for(String sendCommand: sendCommandList){
                        send(sendCommand);
                    }
                }
            }else if (m.get(EXPECT_KEYWORD) instanceof String){
                final String expCmd = (String) m.get(EXPECT_KEYWORD);
                final List<String> sendCommandList = (List<String>) m.get(COMMAND_LIST);

                expect(expCmd, new Closure() {
                    @Override
                    public void run(ExpectState expectState) throws Exception {
                        logger.info("found a match with buffer '{}' ", expectState.getBuffer());
                        logger.info("last match '{}'", expectState.getMatch());

                        if(sendCommandList != null && !sendCommandList.isEmpty()){
                            for(String sendCommand: sendCommandList){
                                logger.info("sending command: '{}'", sendCommand);
                                if(sendCommand.contains("sleep")){
                                    String t = sendCommand.split(" ")[1];
                                    Thread.sleep(Long.valueOf(t));
                                } else {
                                    send(sendCommand);
                                }
                            }
                        }

                    }
                });

            } else if (m.get(EXPECT_KEYWORD) instanceof List){
                List<Match> exp = (List<Match>) m.get(EXPECT_KEYWORD);
                expect(exp);
            }

            if(getLastState().getMatch() == null){
                logger.error("last state is null");
            }
        }

    }

    private List<Map> parse(String script){
        String[] l = script.split("\n");

        List<Map> mapList = new ArrayList<Map>();
        List<String> commandList = new ArrayList<String>();
        Map<String,Object> expMap = new HashMap<String, Object>();
        boolean isIf = false;
        for(int i=0; i<l.length; i++){
            String c = l[i];

            if(c.matches("#.*")){
                continue;
            }
            if(c.contains(EXPECT_KEYWORD)){

                if(!commandList.isEmpty()){
                    expMap.put(COMMAND_LIST, commandList);
                    mapList.add(expMap);
                    expMap = new HashMap<String, Object>();
                    commandList = new ArrayList<String>();
                }

                Matcher m = java.util.regex.Pattern.compile("expect \"(.*)\".*").matcher(c);
                if (m.find()) {
                    String expCmd = m.group(1);


                    expMap = new HashMap<String, Object>();
                    expMap.put(EXPECT_KEYWORD, expCmd);

                } else if (c.matches("expect \\{.*")) {
                    isIf = true;
                }



            } else {

                if(isIf){
                    String endIfPattern = ".*}.*";
                    List<Match> matchList = new ArrayList<Match>();
                    while(!c.matches(endIfPattern)){
                        String ifPattern = "\\s*\"(.*)\".*\\{\\s*";
                        String sendPattern = ".*send\\s*\"(.*)\"\\s*";
                        Matcher m = java.util.regex.Pattern.compile(ifPattern).matcher(c);
                        if(m.find()){
                            String expression = m.group(1);

                            c = l[++i];
                            final List<String> matchCommandList = new ArrayList<String>();
                            while(!c.matches(endIfPattern)){
                                Matcher m2 = java.util.regex.Pattern.compile(sendPattern).matcher(c);
                                if (m2.find()) {
                                    String sendExpresson = m2.group(1);
                                    matchCommandList.add(sendExpresson.replaceAll("\\\\r","\r" ));

                                } else {
                                    matchCommandList.add(c.trim());
                                }

                                c = l[++i];
                            }

                            try {
                                matchList.add(new RegExpMatch(expression, new Closure() {
                                    @Override
                                    public void run(ExpectState expectState) throws Exception {
                                        for(String s: matchCommandList){
                                            if(s.equalsIgnoreCase("exit 1")){
                                                throw new Exception("bad situation. terminating!");
                                            } else if (s.equalsIgnoreCase("exit")){
                                                logger.info("terminating script");

                                            }else if(s.contains("sleep")){
                                                String t = s.split(" ")[1];
                                                Thread.sleep(Long.valueOf(t));
                                            } else if(s.contains("exp_continue")){
                                                expectState.exp_continue();
                                            } else {
                                                send(s);
                                            }

                                        }

                                    }
                                }));
                            } catch (MalformedPatternException e1) {
                                logger.error("Error", e1);
                            }

                        }

                        c = l[++i];
                    }

                    expMap.put(EXPECT_KEYWORD, matchList);
                    mapList.add(expMap);
                    isIf = false;

                } else {
                    Matcher m = java.util.regex.Pattern.compile("send \"(.*)\".*").matcher(c);
                    if(m.find()){
                        String cmd = m.group(1);
                        commandList.add(cmd.replaceAll("\\\\r","\r" ));
                    } else {
                        if(c.trim().length()>0)
                        {
                            commandList.add(c.trim());
                        }
                    }

                }


            }
        }

        if(!commandList.isEmpty()){
            expMap.put(COMMAND_LIST, commandList);
        }
        if(!mapList.contains(expMap)){
            mapList.add(expMap);
        }

        return mapList;
    }
}
