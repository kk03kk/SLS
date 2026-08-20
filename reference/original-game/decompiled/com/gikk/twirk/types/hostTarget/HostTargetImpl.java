/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.hostTarget;

import com.gikk.twirk.enums.HOSTTARGET_MODE;
import com.gikk.twirk.types.hostTarget.DefaultHostTargetBuilder;
import com.gikk.twirk.types.hostTarget.HostTarget;

class HostTargetImpl
implements HostTarget {
    final HOSTTARGET_MODE mode;
    final String target;
    final int viwerAmount;
    private final String rawLine;

    HostTargetImpl(DefaultHostTargetBuilder builder) {
        this.mode = builder.mode;
        this.target = builder.target;
        this.viwerAmount = builder.viwerAmount;
        this.rawLine = builder.rawLine;
    }

    @Override
    public HOSTTARGET_MODE getMode() {
        return this.mode;
    }

    @Override
    public String getTarget() {
        return this.target;
    }

    @Override
    public int getViewerCount() {
        return this.viwerAmount;
    }

    @Override
    public String getRaw() {
        return this.rawLine;
    }
}

