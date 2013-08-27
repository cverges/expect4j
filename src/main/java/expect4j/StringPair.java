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

import java.io.*;

/**
 *
 * @author justin
 */
public class StringPair implements IOPair {
    StringBuffer outBuffer;
    StringReader is;
    StringWriter os;
    
    /** Creates a new instance of StringPair */
    public StringPair(String baseStr) {
        is = new StringReader(baseStr);
        os = new StringWriter();
    }
    
    public Reader getReader() { return is; }
    public Writer getWriter() { return os; }
    
    public String getResult() {
        os.flush();
        return os.getBuffer().toString();
    }
    
    /**
     * TODO evaluate if this is even needed
     */
    public void reset() {
        try {
            os.flush();
            os = new StringWriter();
            is.reset();
        }catch(IOException ioe) {
        }
    }
    
    public void close() {
        try { is.close(); } catch(Exception e) { }
        try { os.close(); } catch(Exception e) { }
    }
    
}
