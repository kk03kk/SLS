/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.AbstractType;

public interface Userstate
extends AbstractType {
    public int getColor();

    public String getDisplayName();

    public boolean isMod();

    public boolean isSub();

    public boolean isTurbo();

    public USER_TYPE getUserType();

    public int[] getEmoteSets();
}

