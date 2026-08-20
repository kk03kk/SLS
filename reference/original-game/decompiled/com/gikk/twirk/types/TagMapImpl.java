/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types;

import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.TwitchTags;
import java.util.HashMap;

class TagMapImpl
extends HashMap<String, String>
implements TagMap {
    public TagMapImpl(String tag) {
        if (tag.isEmpty()) {
            return;
        }
        String[] segments = tag.substring(1).split(";");
        int i = 0;
        for (String s : segments) {
            i = s.indexOf(61);
            if (i == -1) continue;
            String key = s.substring(0, i);
            String value = this.mightBeEscaped(key) ? this.replaceEscapes(s.substring(i + 1)) : s.substring(i + 1);
            this.put(key, value);
        }
    }

    @Override
    public String getAsString(String identifier) {
        return this.getOrDefault(identifier, "");
    }

    @Override
    public int getAsInt(String identifier) {
        try {
            return Integer.decode(this.getOrDefault(identifier, "-1"));
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public long getAsLong(String identifier) {
        try {
            return Long.decode(this.getOrDefault(identifier, "-1"));
        }
        catch (NumberFormatException e) {
            return -1L;
        }
    }

    @Override
    public boolean getAsBoolean(String identifier) {
        return this.getOrDefault(identifier, "0").equals("1");
    }

    private boolean mightBeEscaped(String segment) {
        return segment.equals("display-name") || segment.equals("ban-reason") || segment.equals("system-msg") || segment.equals(TwitchTags.PARAM_SUB_PLAN_NAME);
    }

    private String replaceEscapes(String segment) {
        segment = segment.replace("\\s", " ");
        segment = segment.replace("\\:", ";");
        return segment;
    }
}

