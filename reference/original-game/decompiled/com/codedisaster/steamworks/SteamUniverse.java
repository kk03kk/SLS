/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

public enum SteamUniverse {
    Invalid(0),
    Public(1),
    Beta(2),
    Internal(3),
    Dev(4);

    private final int value;
    private static final SteamUniverse[] values;

    private SteamUniverse(int value) {
        this.value = value;
    }

    static SteamUniverse byValue(int value) {
        for (SteamUniverse type : values) {
            if (type.value != value) continue;
            return type;
        }
        return Invalid;
    }

    static {
        values = SteamUniverse.values();
    }
}

