/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.emote;

import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.emote.EmoteImpl;
import java.util.LinkedList;
import java.util.List;

public class EmoteParser {
    private static final String EMOTES_IDENTIFIER = "emotes=";

    public static List<Emote> parseEmotes(String content, String tag) {
        LinkedList<Emote> emotes = new LinkedList<Emote>();
        int begin = tag.indexOf(EMOTES_IDENTIFIER);
        int end = tag.indexOf(59, begin);
        if (begin == -1 || begin + EMOTES_IDENTIFIER.length() == end) {
            return emotes;
        }
        String emotesString = tag.substring(begin + EMOTES_IDENTIFIER.length(), end);
        EmoteImpl emote = new EmoteImpl();
        StringBuilder str = new StringBuilder();
        String emoteID = "";
        String beginIndex = "";
        block6: for (char c : emotesString.toCharArray()) {
            switch (c) {
                case ':': {
                    emoteID = str.toString();
                    str.setLength(0);
                    continue block6;
                }
                case '-': {
                    beginIndex = str.toString();
                    str.setLength(0);
                    continue block6;
                }
                case ',': {
                    EmoteParser.addIndices(emote, beginIndex, str.toString());
                    str.setLength(0);
                    continue block6;
                }
                case '/': {
                    EmoteParser.finalizeEmote(content, emotes, emote, emoteID, beginIndex, str.toString());
                    emote = new EmoteImpl();
                    str.setLength(0);
                    continue block6;
                }
                default: {
                    str.append(c);
                }
            }
        }
        EmoteParser.finalizeEmote(content, emotes, emote, emoteID, beginIndex, str.toString());
        return emotes;
    }

    private static void finalizeEmote(String content, List<Emote> emotes, EmoteImpl emote, String emoteID, String beginIndex, String endIndex) {
        int begin = Integer.parseInt(beginIndex);
        int end = Integer.parseInt(endIndex) + 1;
        emote.addIndices(begin, end);
        emote.setEmoteID(Integer.parseInt(emoteID));
        emote.setPattern(content.substring(begin, end));
        emotes.add(emote);
    }

    private static void addIndices(EmoteImpl emote, String beginIndex, String endIndex) {
        int begin = Integer.parseInt(beginIndex);
        int end = Integer.parseInt(endIndex) + 1;
        emote.addIndices(begin, end);
    }
}

