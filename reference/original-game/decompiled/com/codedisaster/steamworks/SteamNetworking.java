/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamInterface;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import com.codedisaster.steamworks.SteamNetworkingCallbackAdapter;
import com.codedisaster.steamworks.SteamNetworkingNative;
import java.nio.ByteBuffer;

public class SteamNetworking
extends SteamInterface {
    private final boolean isServer;
    private final int[] tmpIntResult = new int[1];
    private final long[] tmpLongResult = new long[1];

    public SteamNetworking(SteamNetworkingCallback callback) {
        this(false, SteamNetworkingNative.createCallback(new SteamNetworkingCallbackAdapter(callback)));
    }

    SteamNetworking(boolean isServer, long callback) {
        super(callback);
        this.isServer = isServer;
    }

    public boolean sendP2PPacket(SteamID steamIDRemote, ByteBuffer data, P2PSend sendType, int channel) throws SteamException {
        if (!data.isDirect()) {
            throw new SteamException("Direct buffer required!");
        }
        return SteamNetworkingNative.sendP2PPacket(this.isServer, steamIDRemote.handle, data, data.position(), data.remaining(), sendType.ordinal(), channel);
    }

    public boolean isP2PPacketAvailable(int channel, int[] msgSize) {
        return SteamNetworkingNative.isP2PPacketAvailable(this.isServer, msgSize, channel);
    }

    public int readP2PPacket(SteamID steamIDRemote, ByteBuffer dest, int channel) throws SteamException {
        if (!dest.isDirect()) {
            throw new SteamException("Direct buffer required!");
        }
        if (SteamNetworkingNative.readP2PPacket(this.isServer, dest, dest.position(), dest.remaining(), this.tmpIntResult, this.tmpLongResult, channel)) {
            steamIDRemote.handle = this.tmpLongResult[0];
            return this.tmpIntResult[0];
        }
        return 0;
    }

    public boolean acceptP2PSessionWithUser(SteamID steamIDRemote) {
        return SteamNetworkingNative.acceptP2PSessionWithUser(this.isServer, steamIDRemote.handle);
    }

    public boolean closeP2PSessionWithUser(SteamID steamIDRemote) {
        return SteamNetworkingNative.closeP2PSessionWithUser(this.isServer, steamIDRemote.handle);
    }

    public boolean closeP2PChannelWithUser(SteamID steamIDRemote, int channel) {
        return SteamNetworkingNative.closeP2PChannelWithUser(this.isServer, steamIDRemote.handle, channel);
    }

    public boolean getP2PSessionState(SteamID steamIDRemote, P2PSessionState connectionState) {
        return SteamNetworkingNative.getP2PSessionState(this.isServer, steamIDRemote.handle, connectionState);
    }

    public boolean allowP2PPacketRelay(boolean allow) {
        return SteamNetworkingNative.allowP2PPacketRelay(this.isServer, allow);
    }

    public static class P2PSessionState {
        byte connectionActive;
        byte connecting;
        byte sessionError;
        byte usingRelay;
        int bytesQueuedForSend;
        int packetsQueuedForSend;
        int remoteIP;
        short remotePort;

        public boolean isConnectionActive() {
            return this.connectionActive != 0;
        }

        public boolean isConnecting() {
            return this.connecting != 0;
        }

        public P2PSessionError getLastSessionError() {
            return P2PSessionError.byOrdinal(this.sessionError);
        }

        public boolean isUsingRelay() {
            return this.usingRelay != 0;
        }

        public int getBytesQueuedForSend() {
            return this.bytesQueuedForSend;
        }

        public int getPacketsQueuedForSend() {
            return this.packetsQueuedForSend;
        }

        public int getRemoteIP() {
            return this.remoteIP;
        }

        public short getRemotePort() {
            return this.remotePort;
        }
    }

    public static enum P2PSessionError {
        None,
        NotRunningApp,
        NoRightsToApp,
        DestinationNotLoggedIn,
        Timeout;

        private static final P2PSessionError[] values;

        public static P2PSessionError byOrdinal(int sessionError) {
            return values[sessionError];
        }

        static {
            values = P2PSessionError.values();
        }
    }

    public static enum P2PSend {
        Unreliable,
        UnreliableNoDelay,
        Reliable,
        ReliableWithBuffering;

    }
}

