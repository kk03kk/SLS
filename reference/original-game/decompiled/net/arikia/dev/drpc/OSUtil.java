/*
 * Decompiled with CFR 0.152.
 */
package net.arikia.dev.drpc;

public final class OSUtil {
    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }
}

