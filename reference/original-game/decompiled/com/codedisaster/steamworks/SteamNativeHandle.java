/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

public abstract class SteamNativeHandle {
    long handle;

    SteamNativeHandle(long handle) {
        this.handle = handle;
    }

    public static <T extends SteamNativeHandle> long getNativeHandle(T handle) {
        return handle.handle;
    }

    public int hashCode() {
        return Long.valueOf(this.handle).hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof SteamNativeHandle) {
            return this.handle == ((SteamNativeHandle)other).handle;
        }
        return false;
    }

    public String toString() {
        return Long.toHexString(this.handle);
    }
}

