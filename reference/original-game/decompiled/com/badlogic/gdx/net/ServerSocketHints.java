/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.net;

public class ServerSocketHints {
    public int backlog = 16;
    public int performancePrefConnectionTime = 0;
    public int performancePrefLatency = 1;
    public int performancePrefBandwidth = 0;
    public boolean reuseAddress = true;
    public int acceptTimeout = 5000;
    public int receiveBufferSize = 4096;
}

