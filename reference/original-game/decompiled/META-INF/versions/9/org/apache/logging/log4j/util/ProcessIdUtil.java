/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.util;

/*
 * Multiple versions of this class in jar - see https://www.benf.org/other/cfr/multi-version-jar.html
 */
public class ProcessIdUtil {
    public static final String DEFAULT_PROCESSID = "-";

    public static String getProcessId() {
        try {
            return Long.toString(ProcessHandle.current().pid());
        }
        catch (Exception ex) {
            return DEFAULT_PROCESSID;
        }
    }
}

