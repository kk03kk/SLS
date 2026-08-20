/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.notice;

import com.gikk.twirk.types.notice.DefaultNoticeBuilder;
import com.gikk.twirk.types.notice.Notice;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public interface NoticeBuilder {
    public static NoticeBuilder getDefault() {
        return new DefaultNoticeBuilder();
    }

    public Notice build(TwitchMessage var1);
}

