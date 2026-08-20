/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.cheer;

import com.gikk.twirk.types.cheer.Cheer;
import com.gikk.twirk.types.cheer.CheerSize;
import com.gikk.twirk.types.cheer.CheerTheme;
import com.gikk.twirk.types.cheer.CheerType;
import java.util.Objects;

public class CheerImpl
implements Cheer {
    private static final String ROOT_URL = "static-cdn.jtvnw.net/bits/<theme>/<type>/<color>/<size>";
    private final int bits;
    private final String message;

    public CheerImpl(int bits, String message) {
        this.bits = bits;
        this.message = message;
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Cheer)) {
            return false;
        }
        Cheer other = (Cheer)obj;
        return this.bits == other.getBits() && this.message.equals(other.getMessage());
    }

    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + this.bits;
        hash = 17 * hash + Objects.hashCode(this.message);
        return hash;
    }

    @Override
    public String getImageURL(CheerTheme theme, CheerType type, CheerSize size) {
        String color = this.bits < 100 ? "gray" : (this.bits < 1000 ? "purple" : (this.bits < 5000 ? "green" : (this.bits < 10000 ? "blue" : "red")));
        StringBuilder b = new StringBuilder(ROOT_URL);
        b.append("/").append(theme.getValue()).append("/").append(type.getValue()).append("/").append(color).append("/").append(size.getValue());
        return b.toString();
    }
}

