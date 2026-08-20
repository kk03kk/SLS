/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

public abstract class SteamNativeIntHandle {
    int handle;

    SteamNativeIntHandle(int handle) {
        this.handle = handle;
    }

    public static <T extends SteamNativeIntHandle> int getNativeHandle(T handle) {
        return handle.handle;
    }

    public int hashCode() {
        return Integer.valueOf(this.handle).hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof SteamNativeIntHandle) {
            return this.handle == ((SteamNativeIntHandle)other).handle;
        }
        return false;
    }

    public String toString() {
        return Integer.toHexString(this.handle);
    }
}

