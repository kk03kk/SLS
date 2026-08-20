/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;

class SteamNetworkingCallbackAdapter
extends SteamCallbackAdapter<SteamNetworkingCallback> {
    SteamNetworkingCallbackAdapter(SteamNetworkingCallback callback) {
        super(callback);
    }

    void onP2PSessionConnectFail(long steamIDRemote, int sessionError) {
        SteamID id = new SteamID(steamIDRemote);
        ((SteamNetworkingCallback)this.callback).onP2PSessionConnectFail(id, SteamNetworking.P2PSessionError.byOrdinal(sessionError));
    }

    void onP2PSessionRequest(long steamIDRemote) {
        SteamID id = new SteamID(steamIDRemote);
        ((SteamNetworkingCallback)this.callback).onP2PSessionRequest(id);
    }
}

