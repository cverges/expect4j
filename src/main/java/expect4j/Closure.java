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
 * A <code>Closure</code> is a snippet of code that can access variables
 * from one context while running in another.  <code>Closure</code>s are
 * used with Expect4J to define handlers that are executed whenever a
 * corresponding {@link expect4j.matches.Match} is found.
 * <p>
 * Currently in Java, all variables accessed by the <code>Closure</code>
 * need to be marked as <code>final</code>.  To export information from
 * the <code>Closure</code> context to the context in which the
 * <code>Closure</code> is defined, consider using buffers such as
 * <code>final {@link java.lang.StringBuffer}</code>.
 * <pre>
 * TODO: examples
 * </pre>
 *
 * @author Chris Verges
 * @author Justin Ryan
 * @see <a href="http://en.wikipedia.org/wiki/Closure_(computer_science)">Wikipedia</a>
 */
public interface Closure {
    /**
     * The main execution of the <code>Closure</code>.  All care should
     * be taken to avoid throwing <code>Exception</code>s, though the
     * calling context is responsible for catching any thrown.
     * Information about the triggering condition, such as the match or
     * submatch strings, can be obtained through the {@link ExpectState}
     * instance provided.
     *
     * @param state the contextual information on why this
     *              <code>Closure</code> was executed
     * @throws Exception in an extreme case
     */
    public void run(ExpectState state) throws Exception;
}
