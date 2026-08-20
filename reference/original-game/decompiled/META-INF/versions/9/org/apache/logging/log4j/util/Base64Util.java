/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.util;

import java.util.Base64;

/*
 * Multiple versions of this class in jar - see https://www.benf.org/other/cfr/multi-version-jar.html
 */
public final class Base64Util {
    private static final Base64.Encoder encoder = Base64.getEncoder();

    private Base64Util() {
    }

    public static String encode(String str) {
        return str != null ? encoder.encodeToString(str.getBytes()) : null;
    }
}

