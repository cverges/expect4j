/*
 * Copyright (c) 2007 Justin Ryan
 * Copyright (c) 2013 Chris Verges <chris.verges@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package expect4j;

/**
 * A logger that gets notified whenever the buffer changes.  This is
 * used to gracefully track partial progress.
 *
 * @author Chris Verges
 */
public interface BufferChangeLogger {

    /**
     * Called to track new data that goes into the buffer.  Data is
     * stored in a character array.  The number of characters is tracked
     * separately, as the array provided is the raw buffer used to read
     * the new data.  As such, the contents of the buffer should not be
     * modified in any way.
     *
     * This method should also be optimized to run very fast as this is
     * called in real time.  If it's possible to use an intermediate to
     * turn this into an async handler, that would be ideal to use here.
     *
     * @param newData the buffer that contains the new data being added
     * @param numChars the number of valid characters in the buffer
     */
    public void bufferChanged(char[] newData, int numChars);

}
