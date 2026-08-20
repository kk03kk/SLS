/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.emote;

import com.gikk.twirk.enums.EMOTE_SIZE;
import com.gikk.twirk.types.emote.Emote;
import java.util.LinkedList;

class EmoteImpl
implements Emote {
    private static final String EMOTE_URL_BASE = "http://static-cdn.jtvnw.net/emoticons/v1/";
    private int emoteID;
    private final LinkedList<Emote.EmoteIndices> indices = new LinkedList();
    private String pattern;

    EmoteImpl() {
    }

    public EmoteImpl setEmoteID(int emoteID) {
        this.emoteID = emoteID;
        return this;
    }

    public EmoteImpl setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public EmoteImpl addIndices(int begin, int end) {
        this.indices.add(new Emote.EmoteIndices(begin, end));
        return this;
    }

    @Override
    public int getEmoteID() {
        return this.emoteID;
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

    @Override
    public LinkedList<Emote.EmoteIndices> getIndices() {
        return this.indices;
    }

    @Override
    public String getEmoteImageUrl(EMOTE_SIZE imageSize) {
        return EMOTE_URL_BASE + this.emoteID + imageSize.value;
    }

    public String toString() {
        StringBuilder out = new StringBuilder(this.emoteID + " " + (this.pattern == null ? "NULL" : this.pattern) + "[ ");
        for (Emote.EmoteIndices index : this.indices) {
            out.append(index.toString());
        }
        out.append(" ]");
        return out.toString();
    }
}

