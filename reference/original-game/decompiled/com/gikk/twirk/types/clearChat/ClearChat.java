/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.clearChat;

import com.gikk.twirk.enums.CLEARCHAT_MODE;
import com.gikk.twirk.types.AbstractType;

public interface ClearChat
extends AbstractType {
    public CLEARCHAT_MODE getMode();

    public String getTarget();

    public int getDuration();

    public String getReason();
}

