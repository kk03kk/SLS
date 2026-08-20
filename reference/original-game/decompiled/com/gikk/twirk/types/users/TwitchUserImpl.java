/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.users.DefaultTwitchUserBuilder;
import com.gikk.twirk.types.users.TwitchUser;

class TwitchUserImpl
implements TwitchUser {
    private final String userName;
    private final String displayName;
    private final boolean isOwner;
    private final boolean isMod;
    private final boolean isSub;
    private final boolean isTurbo;
    private final int color;
    private final long userID;
    private final USER_TYPE userType;
    private final String[] badges;

    TwitchUserImpl(DefaultTwitchUserBuilder builder) {
        this.userName = builder.userName;
        this.displayName = builder.displayName;
        this.isOwner = builder.isOwner;
        this.isMod = builder.isMod;
        this.isSub = builder.isSub;
        this.isTurbo = builder.isTurbo;
        this.badges = builder.badges;
        this.userID = builder.userID;
        this.userType = builder.userType;
        this.color = builder.color;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public boolean isOwner() {
        return this.isOwner;
    }

    @Override
    public boolean isMod() {
        return this.isMod;
    }

    @Override
    public boolean isTurbo() {
        return this.isTurbo;
    }

    @Override
    public boolean isSub() {
        return this.isSub;
    }

    @Override
    public USER_TYPE getUserType() {
        return this.userType;
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public String[] getBadges() {
        return this.badges;
    }

    @Override
    public long getUserID() {
        return this.userID;
    }
}

