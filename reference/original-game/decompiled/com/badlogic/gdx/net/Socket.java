/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.net;

import com.badlogic.gdx.utils.Disposable;
import java.io.InputStream;
import java.io.OutputStream;

public interface Socket
extends Disposable {
    public boolean isConnected();

    public InputStream getInputStream();

    public OutputStream getOutputStream();

    public String getRemoteAddress();
}

