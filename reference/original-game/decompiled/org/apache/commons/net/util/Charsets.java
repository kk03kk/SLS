/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.util;

import java.nio.charset.Charset;

public class Charsets {
    public static Charset toCharset(String charsetName) {
        return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

    public static Charset toCharset(String charsetName, String defaultCharsetName) {
        return charsetName == null ? Charset.forName(defaultCharsetName) : Charset.forName(charsetName);
    }
}

