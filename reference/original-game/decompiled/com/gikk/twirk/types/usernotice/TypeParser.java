/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice;

import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.TwitchTags;
import com.gikk.twirk.types.usernotice.RaidImpl;
import com.gikk.twirk.types.usernotice.RitualImpl;
import com.gikk.twirk.types.usernotice.SubscriptionGiftImpl;
import com.gikk.twirk.types.usernotice.SubscriptionImpl;
import com.gikk.twirk.types.usernotice.subtype.Raid;
import com.gikk.twirk.types.usernotice.subtype.Ritual;
import com.gikk.twirk.types.usernotice.subtype.Subscription;
import com.gikk.twirk.types.usernotice.subtype.SubscriptionGift;
import com.gikk.twirk.types.usernotice.subtype.SubscriptionPlan;

class TypeParser {
    TypeParser() {
    }

    static Raid parseRaid(TagMap map) {
        String displayName = map.getAsString("msg-param-displayName");
        String loginName = map.getAsString("msg-param-login");
        int count = map.getAsInt("msg-param-viewerCount");
        return new RaidImpl(displayName, loginName, count);
    }

    static Subscription parseSubscription(TagMap map) {
        int months = map.getAsInt("msg-param-cumulative-months");
        int streak = 0;
        boolean shareStreak = map.getAsBoolean("msg-param-should-share-streak");
        if (shareStreak) {
            streak = map.getAsInt("msg-param-streak-months");
        }
        if (months <= 0) {
            months = 1;
        }
        String plan = map.getAsString("msg-param-sub-plan");
        SubscriptionPlan subPlan = SubscriptionPlan.of(plan);
        String planName = map.getAsString(TwitchTags.PARAM_SUB_PLAN_NAME);
        SubscriptionGift sg = TypeParser.parseSubscriptionGift(map);
        return new SubscriptionImpl(subPlan, months, streak, shareStreak, planName, sg);
    }

    static Ritual parseRitual(TagMap map) {
        String ritualName = map.getAsString(TwitchTags.PARAM_RITUAL_NAME);
        return new RitualImpl(ritualName);
    }

    private static SubscriptionGift parseSubscriptionGift(TagMap map) {
        if (!map.containsKey(TwitchTags.PARAM_RECIPIANT_ID)) {
            return null;
        }
        long receiverID = map.getAsLong(TwitchTags.PARAM_RECIPIANT_ID);
        String recipientDisplayName = map.getAsString(TwitchTags.PARAM_RECIPIANT_DISPLAY_NAME);
        String recipientUserName = map.getAsString(TwitchTags.PARAM_RECIPIANT_NAME);
        return new SubscriptionGiftImpl(recipientUserName, recipientDisplayName, receiverID);
    }
}

