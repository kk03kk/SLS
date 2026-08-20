/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamHTTP;
import com.codedisaster.steamworks.SteamHTTPCallback;
import com.codedisaster.steamworks.SteamHTTPRequestHandle;

class SteamHTTPCallbackAdapter
extends SteamCallbackAdapter<SteamHTTPCallback> {
    SteamHTTPCallbackAdapter(SteamHTTPCallback callback) {
        super(callback);
    }

    void onHTTPRequestCompleted(long request, long contextValue, boolean requestSuccessful, int statusCode, int bodySize) {
        ((SteamHTTPCallback)this.callback).onHTTPRequestCompleted(new SteamHTTPRequestHandle(request), contextValue, requestSuccessful, SteamHTTP.HTTPStatusCode.byValue(statusCode), bodySize);
    }

    void onHTTPRequestHeadersReceived(long request, long contextValue) {
        ((SteamHTTPCallback)this.callback).onHTTPRequestHeadersReceived(new SteamHTTPRequestHandle(request), contextValue);
    }

    void onHTTPRequestDataReceived(long request, long contextValue, int offset, int bytesReceived) {
        ((SteamHTTPCallback)this.callback).onHTTPRequestDataReceived(new SteamHTTPRequestHandle(request), contextValue, offset, bytesReceived);
    }
}

