/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.roomstate;

import com.gikk.twirk.types.AbstractType;

public interface Roomstate
extends AbstractType {
    public String getBroadcasterLanguage();

    public int get9kMode();

    public int getSubMode();

    public int getSlowModeTimer();
}

