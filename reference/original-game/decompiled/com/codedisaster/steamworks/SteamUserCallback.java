/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;

public interface SteamUserCallback {
    public void onAuthSessionTicket(SteamAuthTicket var1, SteamResult var2);

    public void onValidateAuthTicket(SteamID var1, SteamAuth.AuthSessionResponse var2, SteamID var3);

    public void onMicroTxnAuthorization(int var1, long var2, boolean var4);

    public void onEncryptedAppTicket(SteamResult var1);
}

