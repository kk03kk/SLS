/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.cheer;

public enum CheerSize {
    TINY(1),
    SMALL(2),
    MEDIUM(3),
    LARGE(4);

    private final int value;

    private CheerSize(int i) {
        this.value = i;
    }

    int getValue() {
        return this.value;
    }
}

