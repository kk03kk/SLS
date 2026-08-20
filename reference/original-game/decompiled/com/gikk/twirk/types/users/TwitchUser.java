/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.enums.USER_TYPE;

public interface TwitchUser {
    public String getUserName();

    public String getDisplayName();

    public boolean isOwner();

    public boolean isMod();

    public boolean isTurbo();

    public boolean isSub();

    public USER_TYPE getUserType();

    public int getColor();

    public String[] getBadges();

    public long getUserID();
}

