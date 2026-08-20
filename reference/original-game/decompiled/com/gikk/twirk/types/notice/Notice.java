/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.notice;

import com.gikk.twirk.enums.NOTICE_EVENT;
import com.gikk.twirk.types.AbstractType;

public interface Notice
extends AbstractType {
    public NOTICE_EVENT getEvent();

    public String getMessage();

    public String getRawNoticeID();
}

