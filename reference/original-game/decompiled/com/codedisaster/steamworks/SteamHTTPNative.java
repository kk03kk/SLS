/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamHTTPCallbackAdapter;
import java.nio.ByteBuffer;

final class SteamHTTPNative {
    SteamHTTPNative() {
    }

    static native long createCallback(SteamHTTPCallbackAdapter var0);

    static native long createHTTPRequest(boolean var0, int var1, String var2);

    static native boolean setHTTPRequestContextValue(boolean var0, long var1, long var3);

    static native boolean setHTTPRequestNetworkActivityTimeout(boolean var0, long var1, int var3);

    static native boolean setHTTPRequestHeaderValue(boolean var0, long var1, String var3, String var4);

    static native boolean setHTTPRequestGetOrPostParameter(boolean var0, long var1, String var3, String var4);

    static native long sendHTTPRequest(boolean var0, long var1, long var3);

    static native long sendHTTPRequestAndStreamResponse(boolean var0, long var1);

    static native int getHTTPResponseHeaderSize(boolean var0, long var1, String var3);

    static native boolean getHTTPResponseHeaderValue(boolean var0, long var1, String var3, ByteBuffer var4, int var5, int var6);

    static native int getHTTPResponseBodySize(boolean var0, long var1);

    static native boolean getHTTPResponseBodyData(boolean var0, long var1, ByteBuffer var3, int var4, int var5);

    static native boolean getHTTPStreamingResponseBodyData(boolean var0, long var1, int var3, ByteBuffer var4, int var5, int var6);

    static native boolean releaseHTTPRequest(boolean var0, long var1);
}

