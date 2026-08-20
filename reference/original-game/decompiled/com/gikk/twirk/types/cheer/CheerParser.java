/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.cheer;

import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.cheer.Cheer;
import com.gikk.twirk.types.cheer.CheerImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheerParser {
    private static final Pattern PATTERN = Pattern.compile("([a-zA-Z]+([1-9][0-9]+\\b(?<=\\w)))");

    public static List<Cheer> parseCheer(TagMap tagMap, String content) {
        int bit;
        ArrayList<Cheer> list = new ArrayList<Cheer>();
        int bits = tagMap.getAsInt("bits");
        if (bits == -1) {
            return list;
        }
        Matcher matcher = PATTERN.matcher(content);
        for (int bitsFound = 0; matcher.find() && bitsFound < bits; bitsFound += bit) {
            bit = Integer.parseInt(matcher.group(2));
            CheerImpl cheer = new CheerImpl(bit, matcher.group(1));
            list.add(cheer);
        }
        return list;
    }
}

