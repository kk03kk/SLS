/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

public class SteamMatchmakingServerNetAdr {
    short connectionPort;
    short queryPort;
    int ip;

    SteamMatchmakingServerNetAdr() {
    }

    public SteamMatchmakingServerNetAdr(int ip, short queryPort, short connectionPort) {
        this.ip = ip;
        this.queryPort = queryPort;
        this.connectionPort = connectionPort;
    }

    public short getConnectionPort() {
        return this.connectionPort;
    }

    public short getQueryPort() {
        return this.queryPort;
    }

    public int getIP() {
        return this.ip;
    }

    public String getConnectionAddressString() {
        return SteamMatchmakingServerNetAdr.toString(this.ip, this.connectionPort);
    }

    public String getQueryAddressString() {
        return SteamMatchmakingServerNetAdr.toString(this.ip, this.queryPort);
    }

    private static String toString(int ip, short port) {
        return String.format("%d.%d.%d.%d:%d", ip >> 24 & 0xFF, ip >> 16 & 0xFF, ip >> 8 & 0xFF, ip & 0xFF, port);
    }
}

