/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.twitchMessage;

import com.gikk.twirk.types.AbstractEmoteMessage;
import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.cheer.Cheer;
import java.util.List;

public interface TwitchMessage
extends AbstractEmoteMessage {
    public String getTag();

    public String getPrefix();

    public String getCommand();

    public String getTarget();

    public String getContent();

    public boolean isCheer();

    public List<Cheer> getCheers();

    public int getBits();

    public TagMap getTagMap();
}

