/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.cheer;

public enum CheerType {
    ANIMATED("animated"),
    STATIC("static");

    private final String value;

    private CheerType(String s) {
        this.value = s;
    }

    String getValue() {
        return this.value;
    }
}

