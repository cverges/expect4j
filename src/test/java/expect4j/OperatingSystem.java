package expect4j;

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
