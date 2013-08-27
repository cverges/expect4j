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

import java.io.IOException;

/**
 *
 * @author justin
 */
public interface Consumer extends Runnable {
    public void run();
    public void waitForBuffer(long timeoutMilli);
    public void send(String str) throws IOException;
    public String pause();
    public void resume();
    public void resume(int offset);
    public void stop();
    public boolean foundEOF();    
}
