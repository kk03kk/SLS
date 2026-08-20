/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.cheer;

import com.gikk.twirk.types.cheer.CheerSize;
import com.gikk.twirk.types.cheer.CheerTheme;
import com.gikk.twirk.types.cheer.CheerType;

public interface Cheer {
    public int getBits();

    public String getMessage();

    public String getImageURL(CheerTheme var1, CheerType var2, CheerSize var3);
}

