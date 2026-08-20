/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.enums;

public enum USER_TYPE {
    OWNER(9),
    MOD(6),
    GLOBAL_MOD(4),
    ADMIN(4),
    STAFF(4),
    SUBSCRIBER(2),
    DEFAULT(0);

    public final int value;

    private USER_TYPE(int value) {
        this.value = value;
    }
}

