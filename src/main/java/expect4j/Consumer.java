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

import java.io.IOException;

/**
 * Responsible for absorbing everything from stream and to maintain a
 * buffer.
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public interface Consumer extends Runnable {
    /**
     * Starts the <code>Consumer</code> in processing the reader
     * stream.
     */
    public void run();

    /**
     * TODO: what if something came in between when we last checked and
     *       when this method is called
     *
     * @param timeout timeout in milliseconds
     */
    public void waitForBuffer(long timeout);

    /**
     * Passes data to the writer stream for remote execution from the
     * <code>Consumer</code>.  I'm not really sure why this is useful or
     * desired, but the ability exists.  Use with caution.
     *
     * @param data the data to pass to the writer
     * @throws IOException if an error occurs with the writer stream
     * @see Expect4j#send(String)
     */
    public void send(String data) throws IOException;

    /**
     * TODO
     *
     * @return TODO
     */
    public String pause();

    /**
     * Resume processing from the beginning of the buffer.
     */
    public void resume();

    /**
     * Resume processing from the specified offset.
     *
     * @param offset the offset into the buffer (1-indexed) from which
     *               to start
     */
    public void resume(int offset);

    /**
     * Requests the <code>Consumer</code> to stop processing data at its
     * next convenient time.
     */
    public void stop();

    /**
     * Returns a boolean flag on whether the EOF marker has been detected.
     *
     * @return <code>true</code> if EOF has been detected,
     *         <code>false</code> otherwise
     */
    public boolean foundEOF();

    /**
     * Registers a change logger that is called whenever an input change
     * is recorded to the buffer.
     *
     * @param logger the logger that is called
     */
    public void registerBufferChangeLogger(final BufferChangeLogger logger);

    /**
     * Unregisters the change logger specified.  Once called, this
     * change logger will not receive any further updates.
     *
     * @param logger the logger that is called
     */
    public void unregisterBufferChangeLogger(final BufferChangeLogger logger);
}
