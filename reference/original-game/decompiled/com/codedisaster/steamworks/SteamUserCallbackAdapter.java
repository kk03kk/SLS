/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserCallback;

class SteamUserCallbackAdapter
extends SteamCallbackAdapter<SteamUserCallback> {
    SteamUserCallbackAdapter(SteamUserCallback callback) {
        super(callback);
    }

    void onAuthSessionTicket(long authTicket, int result) {
        ((SteamUserCallback)this.callback).onAuthSessionTicket(new SteamAuthTicket(authTicket), SteamResult.byValue(result));
    }

    void onValidateAuthTicket(long steamID, int authSessionResponse, long ownerSteamID) {
        ((SteamUserCallback)this.callback).onValidateAuthTicket(new SteamID(steamID), SteamAuth.AuthSessionResponse.byOrdinal(authSessionResponse), new SteamID(ownerSteamID));
    }

    void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
        ((SteamUserCallback)this.callback).onMicroTxnAuthorization(appID, orderID, authorized);
    }

    void onEncryptedAppTicket(int result) {
        ((SteamUserCallback)this.callback).onEncryptedAppTicket(SteamResult.byValue(result));
    }
}

