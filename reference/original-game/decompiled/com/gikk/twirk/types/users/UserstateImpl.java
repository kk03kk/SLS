/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.users.DefaultUserstateBuilder;
import com.gikk.twirk.types.users.Userstate;

class UserstateImpl
implements Userstate {
    private final int color;
    private final String displayName;
    private final boolean isMod;
    private final boolean isSub;
    private final boolean isTurbo;
    private final USER_TYPE userType;
    private final int[] emoteSets;
    private final String rawLine;

    UserstateImpl(DefaultUserstateBuilder builder) {
        this.color = builder.color;
        this.displayName = builder.displayName;
        this.isMod = builder.isMod;
        this.isSub = builder.isSub;
        this.isTurbo = builder.isTurbo;
        this.userType = builder.userType;
        this.emoteSets = builder.emoteSets;
        this.rawLine = builder.rawLine;
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public boolean isMod() {
        return this.isMod;
    }

    @Override
    public boolean isSub() {
        return this.isSub;
    }

    @Override
    public boolean isTurbo() {
        return this.isTurbo;
    }

    @Override
    public USER_TYPE getUserType() {
        return this.userType;
    }

    @Override
    public int[] getEmoteSets() {
        return this.emoteSets;
    }

    @Override
    public String getRaw() {
        return this.rawLine;
    }
}

