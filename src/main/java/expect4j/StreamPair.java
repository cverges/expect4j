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
public class StreamPair implements IOPair {
    Reader is;
    Writer os;
    
    /** Creates a new instance of ReaderConsumer */
    public StreamPair(InputStream is, OutputStream os ) {
        this.is = new InputStreamReader(is );
        this.os = new OutputStreamWriter( os );
    }
    
    public Reader getReader() {
        return is;
    }
    
    public Writer getWriter() {
        return os;
    }
    
    /**
     * TODO evaluate if this is even needed
     */
    public void reset() {
        try {
            is.reset();
        }catch(IOException ioe) {
        }
    }
    
    public void close() {
        try { is.close(); } catch(Exception e) { }
        try { os.close(); } catch(Exception e) { }
    }
}
