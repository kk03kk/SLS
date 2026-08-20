/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice;

import com.gikk.twirk.types.usernotice.subtype.SubscriptionGift;

class SubscriptionGiftImpl
implements SubscriptionGift {
    private final String recipiantUserName;
    private final String recipiantDisplayName;
    private final long RecipiantUserID;

    public SubscriptionGiftImpl(String recipientUserName, String recipiantDisplayName, long RecipiantUserID) {
        this.recipiantUserName = recipientUserName;
        this.recipiantDisplayName = recipiantDisplayName;
        this.RecipiantUserID = RecipiantUserID;
    }

    @Override
    public String getRecipiantDisplayName() {
        return this.recipiantDisplayName;
    }

    @Override
    public long getRecipiantUserID() {
        return this.RecipiantUserID;
    }

    @Override
    public String getRecipiantUserName() {
        return this.recipiantUserName;
    }
}

