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

import expect4j.matches.EofMatch;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A container that represents a snapshot of the {@link Expect4j} state
 * machine for a given match.  This container is typically used in
 * conjunction with {@link Closure}s, but may also be used with {@link
 * Expect4j#getLastState}.
 *
 * @author Chris Verges
 * @author Justin Ryan
 */
public class ExpectState {
    /**
     * Interface to the Java 2 platform's core logging facilities.
     */
    private static final Logger logger = LoggerFactory.getLogger(ExpectState.class);

    /**
     * Creates an <code>ExpectState</code> instance for a successful
     * match.  This instance is mostly immutable once created.
     *
     * @param buffer the entire reader buffer at the time of the match
     * @param matchedWhere the offset of the beginning of the match in
     *                     the buffer
     * @param match the exact match found
     * @param pairIndex TODO
     * @param groups a list of submatches, if defined in the match
     *               pattern
     * @param prevMap a <code>Map</code> of the variables previously set
     *                by {@link Closure}s
     */
    public ExpectState(String buffer, int matchedWhere, String match, int pairIndex, List groups, Map<String, Object> prevMap) {
        this(pairIndex, buffer, prevMap);
        this.matchedWhere = matchedWhere;
        this.match = match;
        this.groups = groups;
    }

    /**
     * Creates an <code>ExpectState</code> instance for times when a
     * {@link Closure} is being run but there isn't a corresponding
     * match.  This instance is mostly immutable once created.
     */
    public ExpectState() {
        this(-1, null, null);
    }

    /**
     * Creates an <code>ExpectState</code> instance for a successful
     * match.  This instance is mostly immutable once created.
     *
     * @param pairIndex TODO
     * @param buffer the entire reader buffer at the time of the match
     * @param prevMap a <code>Map</code> of the variables previously set
     *                by {@link Closure}s
     */
    public ExpectState(int pairIndex, String buffer, Map<String, Object> prevMap) {
        this.buffer = buffer;
        this.matchedWhere = -1;
        this.match = null;
        this.pairIndex = pairIndex;
        this.groups = null;
        shouldContinue = false;
        shouldResetTimer = false;

        vars = new HashMap<>();
        if (prevMap != null)
            vars.putAll(prevMap);
    }

    /**
     * The offset, relative to the beginning of {@link
     * ExpectState#buffer}, where the match begins.
     */
    protected int matchedWhere;

    /**
     * Returns the offset into the buffer where the match begins.
     *
     * @return the offset of the match in the buffer
     */
    public int getMatchedWhere() {
        return matchedWhere;
    }

    /**
     * All text that generated a match while processing the buffer.
     */
    protected String match;

    /**
     * Returns all text found in the buffer that generated the match.
     * This is equivalent to Expect's <code>expect_out(0,string)</code>.
     *
     * @return all matching text in the buffer
     */
    public String getMatch() {
        return match;
    }

    /**
     * A list of submatches based on the entire match.
     */
    protected List /* <String> */ groups;

    /**
     * Returns the specific submatch based on index (1-based).  This is
     * equivalent to Expect's <code>expect_out(x,string)</code>.
     */
    public String getMatch(int groupnum) {
        if (groupnum == 0)
            return getMatch();
        else if (groupnum > groups.size())
            return null;

        return (String) groups.get(groupnum - 1);
    }

    /**
     * TODO
     */
    protected int pairIndex;

    /**
     * TODO
     *
     * @return TODO
     */
    public int getPairIndex() {
        return pairIndex;
    }

    /**
     * The entire reader buffer at the time of the match.
     */
    protected String buffer;

    /**
     * Returns all of the matched characters plus the characters that
     * came earlier but did not match.
     * <p>
     * This is equivalent to Expect's {@code expect_out(buffer)}, as
     * documented in the <i>Exploring Expect</i> book from O'Reilly:
     * <blockquote>
     *     All of the matched characters plus the characters that came
     *     earlier but did not match are stored in a variable called
     *     <i>expect_out(buffer)</i>.
     * </blockquote>
     * <p>
     * This can be used in conjunction with
     * {@link Expect4j#getLastState} to get all the output in the buffer
     * between the last match and {@link EofMatch} after Expect4j has
     * finished processing a stream.
     *
     * @return the entire reader buffer.
     * @see <a href="http://oreilly.com/catalog/expect/chapter/ch03.html">Exploring Expect - Getting Started with Expect - The expect command</a>
     */
    public String getBuffer() {
        return buffer;
    }

    /**
     * Sets the reader buffer.
     *
     * @param buffer the contents of the buffer
     */
    public void setBuffer(String buffer) {
        this.buffer = buffer;
    }

    /**
     * A flag to indicate whether the {@link Expect4j} state machine
     * should continue to seek additional matches.
     */
    protected boolean shouldContinue;

    /**
     * Returns whether the {@link Expect4j} state machine should
     * continue to seek additional matches after this one.
     *
     * @return <code>true</code> if the state machine should seek
     *         additional matches, <code>false</code> if it should stop
     *         with this one
     */
    public boolean shouldContinue() { return shouldContinue; }

    /**
     * Instructs the {@link Expect4j} state machine to continue to seek
     * additional matches after this one.  This is equivalent to
     * Expect's <code>exp_continue -continue_timer</code>.
     *
     * @see ExpectState#exp_continue_reset_timer
     */
    public void exp_continue() {
        shouldContinue = true;
    }

    /**
     * A flag to indicate whether the {@link Expect4j} state machine
     * should reset its timer to avoid a timeout.
     */
    protected boolean shouldResetTimer; // TODO prevent closure from setting

    /**
     * Returns whether the {@link Expect4j} state machine should reset
     * its timeout timer after processing this match.
     *
     * @return <code>true</code> if the timer should be reset,
     *         <code>false</code> if the timer should be allowed to
     *         continue as-is
     */
    public boolean shouldResetTimer() {
        return shouldResetTimer;
    }

    /**
     * Instructs the {@link Expect4j} state machine to continue to seek
     * additional matches after this one <i>and</i> to reset the timeout
     * timer.  This is equivalent to Expect's <code>exp_continue</code>.
     *
     * @see ExpectState#exp_continue
     */
    public void exp_continue_reset_timer() {
        shouldContinue = true;
        shouldResetTimer = true;
    }

    /* Closure Variables */

    /**
     * A map of key/value pairs that can be set during execution of a
     * {@link Closure} and accessed from outside the {@link Expect4j}
     * context.  This list is constantly appended by each
     * <code>Closure</code> executed, so the final
     * <code>ExpectState</code> instance that is retrieved using {@link
     * Expect4j#getLastState} has the whole list.
     * <p>
     * This mechanism is provided as a way of emulating Tcl's natural
     * closure/scope behavior, which Java doesn't allow.  In Tcl,
     * closures can reference outside variables directly.  In Java,
     * closures can only reference outside variables if the variables
     * have been declared <code>final</code>.  This use of
     * <code>final</code> presents certain challenges in performing
     * multiple matches with the same <code>Closure</code> block,
     * because only the first execution would alow the variable to be
     * set.  Through this alternate mechanism, each execution can simply
     * add or manipulate the key/value pairs as desired.
     * <p>
     * Note that this mechanism is <i>in addition to</i> the use of a
     * <code>final</code> buffer (such as <code>StringBuffer</code>),
     * which may be more appropriate for certain cases.
     */
    protected Map<String, Object> vars;

    /**
     * Adds a key/value pair from a {@link Closure} context.
     *
     * @param key the name used to reference this variable
     * @param value the value stored in the variable for the key
     */
    public void addVar(String key, Object value) {
        vars.put(key, value);
    }

    /**
     * Returns the value for the given key.
     *
     * @param key the name used to reference the variable desired
     * @return the value stored in the variable for the key
     */
    public Object getVar(String key) {
        return vars.get(key);
    }

    /**
     * Returns the whole list of variables.
     *
     * @return the whole list of variables
     */
    Map<String, Object> getVars() {
        return vars;
    }
}
