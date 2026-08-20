/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.twitchMessage;

import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.cheer.Cheer;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.twitchMessage.DefaultTwitchMessageBuilder;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import java.util.List;

class TwitchMessageImpl
implements TwitchMessage {
    private final String line;
    private final String tag;
    private final String prefix;
    private final String command;
    private final String target;
    private final String content;
    private final String id;
    private final int roomID;
    private final List<Emote> emotes;
    private final List<Cheer> cheers;
    private final TagMap tagMap;

    TwitchMessageImpl(DefaultTwitchMessageBuilder builder) {
        this.line = builder.line;
        this.tag = builder.tag;
        this.prefix = builder.prefix;
        this.command = builder.command;
        this.target = builder.target;
        this.content = builder.content;
        this.emotes = builder.emotes;
        this.cheers = builder.cheers;
        this.tagMap = builder.tagMap;
        this.id = builder.id;
        this.roomID = builder.roomID;
    }

    @Override
    public String getRaw() {
        return this.line;
    }

    @Override
    public String getTag() {
        return this.tag;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public String getCommand() {
        return this.command;
    }

    @Override
    public String getTarget() {
        return this.target;
    }

    @Override
    public String getContent() {
        return this.content;
    }

    @Override
    public boolean hasEmotes() {
        return !this.emotes.isEmpty();
    }

    @Override
    public List<Emote> getEmotes() {
        return this.emotes;
    }

    @Override
    public boolean isCheer() {
        return !this.cheers.isEmpty();
    }

    @Override
    public List<Cheer> getCheers() {
        return this.cheers;
    }

    public String toString() {
        return this.line;
    }

    @Override
    public TagMap getTagMap() {
        return this.tagMap;
    }

    @Override
    public int getBits() {
        int bits = 0;
        for (Cheer c : this.getCheers()) {
            bits += c.getBits();
        }
        return bits;
    }

    @Override
    public long getSentTimestamp() {
        return this.tagMap.getAsLong("tmi-sent-ts");
    }

    @Override
    public long getRoomID() {
        return this.roomID;
    }

    @Override
    public String getMessageID() {
        return this.id;
    }
}

