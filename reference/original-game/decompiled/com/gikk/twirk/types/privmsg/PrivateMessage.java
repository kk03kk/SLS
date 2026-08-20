/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.privmsg;

import com.gikk.twirk.types.AbstractEmoteMessage;
import com.gikk.twirk.types.cheer.Cheer;
import java.util.List;

public interface PrivateMessage
extends AbstractEmoteMessage {
    public boolean isCheer();

    public List<Cheer> getCheers();

    public int getBits();
}

