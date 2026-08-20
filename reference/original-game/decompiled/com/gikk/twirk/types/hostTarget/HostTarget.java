/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.hostTarget;

import com.gikk.twirk.enums.HOSTTARGET_MODE;
import com.gikk.twirk.types.AbstractType;

public interface HostTarget
extends AbstractType {
    public HOSTTARGET_MODE getMode();

    public String getTarget();

    public int getViewerCount();
}

