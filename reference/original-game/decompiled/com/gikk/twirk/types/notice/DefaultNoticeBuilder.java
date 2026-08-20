/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.notice;

import com.gikk.twirk.enums.NOTICE_EVENT;
import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.notice.Notice;
import com.gikk.twirk.types.notice.NoticeBuilder;
import com.gikk.twirk.types.notice.NoticeImpl;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public class DefaultNoticeBuilder
implements NoticeBuilder {
    NOTICE_EVENT event;
    String message;
    String rawLine;
    String rawEvent;

    @Override
    public Notice build(TwitchMessage message) {
        TagMap r = message.getTagMap();
        this.rawEvent = r.getAsString("msg-id");
        this.event = NOTICE_EVENT.of(this.rawEvent);
        this.message = message.getContent();
        this.rawLine = message.getRaw();
        return new NoticeImpl(this);
    }
}

