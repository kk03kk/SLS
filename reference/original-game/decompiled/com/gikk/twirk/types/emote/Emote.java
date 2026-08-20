/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.emote;

import com.gikk.twirk.enums.EMOTE_SIZE;
import java.util.LinkedList;

public interface Emote {
    public int getEmoteID();

    public LinkedList<EmoteIndices> getIndices();

    public String getPattern();

    public String getEmoteImageUrl(EMOTE_SIZE var1);

    public static class EmoteIndices {
        public final int beingIndex;
        public final int endIndex;

        public EmoteIndices(int begin, int end) {
            this.beingIndex = begin;
            this.endIndex = end;
        }

        public String toString() {
            return "(" + this.beingIndex + "," + this.endIndex + ")";
        }
    }
}

