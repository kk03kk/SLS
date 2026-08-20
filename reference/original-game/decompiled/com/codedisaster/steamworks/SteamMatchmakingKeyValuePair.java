/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

public class SteamMatchmakingKeyValuePair {
    private String key;
    private String value;

    public SteamMatchmakingKeyValuePair() {
    }

    public SteamMatchmakingKeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }
}

