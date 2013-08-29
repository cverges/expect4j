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
 * A utility class to help with operating system detection.
 *
 * @author Chris Verges
 */
public class OperatingSystem {
    public static final int WINDOWS = 1;
    public static final int UNIX = 2;
    public static final int MAC = 3;
    public static final int UNKNOWN = 4;

    private static int osType = 0;

    private OperatingSystem() {}

    protected static void setup() {
        if (osType == 0) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.indexOf("win") >= 0)
                osType = OperatingSystem.WINDOWS;
            else if (osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0)
                osType = OperatingSystem.UNIX;
            else if (osName.indexOf("mac") >= 0)
                osType = OperatingSystem.MAC;
            else
                osType = OperatingSystem.UNKNOWN;
        }
    }

    public static boolean isWindows() {
        setup();
        return osType == OperatingSystem.WINDOWS;
    }

    public static boolean isUnix() {
        setup();
        return osType == OperatingSystem.UNIX;
    }

    public static boolean isMac() {
        setup();
        return osType == OperatingSystem.MAC;
    }

    public static boolean isUnknown() {
        setup();
        return osType == OperatingSystem.UNKNOWN;
    }
}
