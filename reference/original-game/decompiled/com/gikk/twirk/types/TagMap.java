/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types;

import com.gikk.twirk.types.TagMapImpl;
import java.util.Map;

public interface TagMap
extends Map<String, String> {
    public String getAsString(String var1);

    public int getAsInt(String var1);

    public long getAsLong(String var1);

    public boolean getAsBoolean(String var1);

    public static TagMap getDefault(String tag) {
        return new TagMapImpl(tag);
    }
}

