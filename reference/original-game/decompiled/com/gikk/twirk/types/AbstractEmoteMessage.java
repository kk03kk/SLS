/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types;

import com.gikk.twirk.types.AbstractType;
import com.gikk.twirk.types.emote.Emote;
import java.util.List;

public interface AbstractEmoteMessage
extends AbstractType {
    public boolean hasEmotes();

    public List<Emote> getEmotes();

    public long getSentTimestamp();

    public long getRoomID();

    public String getMessageID();
}

