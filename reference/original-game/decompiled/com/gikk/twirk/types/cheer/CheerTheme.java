/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.cheer;

public enum CheerTheme {
    LIGHT("light"),
    DARK("dark");

    private final String value;

    private CheerTheme(String s) {
        this.value = s;
    }

    String getValue() {
        return this.value;
    }
}

