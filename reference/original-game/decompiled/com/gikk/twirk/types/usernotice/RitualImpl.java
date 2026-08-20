/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice;

import com.gikk.twirk.types.usernotice.subtype.Ritual;

class RitualImpl
implements Ritual {
    private final String ritualName;

    public RitualImpl(String ritualName) {
        this.ritualName = ritualName;
    }

    @Override
    public String getRitualName() {
        return this.ritualName;
    }
}

