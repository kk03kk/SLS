/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.twitchMessage;

import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.cheer.Cheer;
import com.gikk.twirk.types.cheer.CheerParser;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.emote.EmoteParser;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.twitchMessage.TwitchMessageBuilder;
import com.gikk.twirk.types.twitchMessage.TwitchMessageImpl;
import java.util.List;

class DefaultTwitchMessageBuilder
implements TwitchMessageBuilder {
    String line;
    String tag;
    String prefix;
    String command;
    String target;
    String content;
    String id;
    int roomID;
    List<Emote> emotes;
    List<Cheer> cheers;
    TagMap tagMap;

    DefaultTwitchMessageBuilder() {
    }

    @Override
    public TwitchMessage build(String chatLine) {
        if (chatLine.startsWith("@")) {
            this.parseWithTag(chatLine);
        } else {
            this.parseWithoutTag(chatLine);
        }
        this.line = chatLine;
        this.tagMap = TagMap.getDefault(this.tag);
        this.id = this.tagMap.getAsString("id");
        this.roomID = this.tagMap.getAsInt("room-id");
        this.emotes = EmoteParser.parseEmotes(this.content, this.tag);
        this.cheers = CheerParser.parseCheer(this.tagMap, this.content);
        return new TwitchMessageImpl(this);
    }

    private void parseWithTag(String line) {
        String[] parts = line.split(" ", 5);
        this.content = parts.length == 5 ? (parts[4].startsWith(":") ? parts[4].substring(1) : parts[4]) : "";
        this.target = parts.length >= 4 ? parts[3] : "";
        this.command = parts.length >= 3 ? parts[2] : "";
        this.prefix = parts.length >= 2 ? parts[1] : "";
        this.tag = parts.length >= 1 ? parts[0] : "";
    }

    private void parseWithoutTag(String line) {
        char c;
        this.tag = "";
        StringBuilder build = new StringBuilder();
        int i = 0;
        while ((c = line.charAt(i++)) != ' ') {
            build.append(c);
        }
        this.prefix = build.toString().trim();
        build.setLength(0);
        do {
            build.append(c);
        } while (i < line.length() && (c = line.charAt(i++)) != ' ');
        this.command = build.toString().trim();
        build.setLength(0);
        do {
            build.append(c);
        } while (i < line.length() && (c = line.charAt(i++)) != ':' && c != '+' && c != '-');
        this.target = build.toString().trim();
        build.setLength(0);
        if (i == line.length()) {
            this.content = "";
            return;
        }
        do {
            build.append(c);
        } while (i < line.length() && (c = line.charAt(i++)) != '\r');
        String temp = build.toString().trim();
        this.content = temp.startsWith(":") ? temp.substring(1) : temp;
    }
}

