/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice;

import com.gikk.twirk.types.usernotice.subtype.Subscription;
import com.gikk.twirk.types.usernotice.subtype.SubscriptionGift;
import com.gikk.twirk.types.usernotice.subtype.SubscriptionPlan;
import java.util.Optional;

class SubscriptionImpl
implements Subscription {
    private final SubscriptionPlan plan;
    private final int months;
    private final int streak;
    private final boolean shareStreak;
    private final String planName;
    private final Optional<SubscriptionGift> subGift;

    SubscriptionImpl(SubscriptionPlan plan, int months, int streak, boolean sharedStreak, String planName, SubscriptionGift subGift) {
        this.plan = plan;
        this.months = months;
        this.streak = streak;
        this.shareStreak = sharedStreak;
        this.planName = planName;
        this.subGift = Optional.ofNullable(subGift);
    }

    @Override
    public SubscriptionPlan getSubscriptionPlan() {
        return this.plan;
    }

    @Override
    public int getMonths() {
        return this.months;
    }

    @Override
    public int getStreak() {
        return this.streak;
    }

    @Override
    public boolean isSharedStreak() {
        return this.shareStreak;
    }

    @Override
    public String getSubscriptionPlanName() {
        return this.planName;
    }

    @Override
    public boolean isResub() {
        return this.months > 1;
    }

    @Override
    public boolean isGift() {
        return this.subGift.isPresent();
    }

    @Override
    public Optional<SubscriptionGift> getSubscriptionGift() {
        return this.subGift;
    }
}

