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

import java.io.Reader;
import java.io.Writer;

/**
 * TODO
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public interface IOPair {
    /**
     * Returns a {@link java.io.Reader} that accesses the input stream.
     *
     * @return a <code>Reader</code> for the input stream
     */
    public Reader getReader();

    /**
     * Returns a {@link java.io.Writer} that accesses the output stream.
     *
     * @return a <code>Writer</code> for the output stream
     */
    public Writer getWriter();

    /**
     * TODO
     */
    public void reset();

    /**
     * TODO
     */
    public void close();
}
