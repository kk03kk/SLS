/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.notice;

import com.gikk.twirk.enums.NOTICE_EVENT;
import com.gikk.twirk.types.notice.DefaultNoticeBuilder;
import com.gikk.twirk.types.notice.Notice;

class NoticeImpl
implements Notice {
    private final NOTICE_EVENT event;
    private final String message;
    private final String rawLine;
    private final String rawEvent;

    NoticeImpl(DefaultNoticeBuilder builder) {
        this.event = builder.event;
        this.message = builder.message;
        this.rawLine = builder.rawLine;
        this.rawEvent = builder.rawEvent;
    }

    @Override
    public NOTICE_EVENT getEvent() {
        return this.event;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public String getRaw() {
        return this.rawLine;
    }

    @Override
    public String getRawNoticeID() {
        return this.rawEvent;
    }
}

