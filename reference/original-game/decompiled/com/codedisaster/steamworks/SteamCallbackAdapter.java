/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

abstract class SteamCallbackAdapter<T> {
    protected final T callback;

    SteamCallbackAdapter(T callback) {
        this.callback = callback;
    }
}

