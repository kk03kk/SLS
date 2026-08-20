/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.mode;

import com.gikk.twirk.types.AbstractType;

public interface Mode
extends AbstractType {
    public MODE_EVENT getEvent();

    public String getUser();

    public static enum MODE_EVENT {
        GAINED_MOD,
        LOST_MOD;

    }
}

