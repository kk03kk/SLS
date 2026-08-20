/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice.subtype;

import com.gikk.twirk.types.usernotice.subtype.SubscriptionGift;
import com.gikk.twirk.types.usernotice.subtype.SubscriptionPlan;
import java.util.Optional;

public interface Subscription {
    public SubscriptionPlan getSubscriptionPlan();

    public int getMonths();

    public int getStreak();

    public boolean isSharedStreak();

    public String getSubscriptionPlanName();

    public boolean isResub();

    public boolean isGift();

    public Optional<SubscriptionGift> getSubscriptionGift();
}

