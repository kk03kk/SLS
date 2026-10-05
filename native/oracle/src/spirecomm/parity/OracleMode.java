package spirecomm.parity;

/** One immutable mode per JVM; invalid settings fail instead of changing rules silently. */
public final class OracleMode {
    public static final String CONTRACT = "sls-oracle-mode-v1";
    public static final String MODE = readMode();

    private OracleMode() {}

    private static String readMode() {
        String mode = System.getProperty("sls.oracle.mode", "production");
        if (!mode.equals("production") && !mode.equals("validation")) {
            throw new IllegalArgumentException("sls.oracle.mode must be production or validation");
        }
        return mode;
    }

    public static boolean validation() {
        return MODE.equals("validation");
    }
}
