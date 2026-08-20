/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations.steam;

import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserCallback;

public class SUCallback
implements SteamUserCallback {
    @Override
    public void onAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {
    }

    @Override
    public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
    }

    @Override
    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
    }

    @Override
    public void onEncryptedAppTicket(SteamResult result) {
    }
}

