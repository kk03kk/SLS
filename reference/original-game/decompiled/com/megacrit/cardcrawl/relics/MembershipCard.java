/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;

public class MembershipCard
extends AbstractRelic {
    public static final String ID = "Membership Card";
    public static final float MULTIPLIER = 0.5f;

    public MembershipCard() {
        super(ID, "membershipCard.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEnterRoom(AbstractRoom room) {
        if (room instanceof ShopRoom) {
            this.flash();
            this.pulse = true;
        } else {
            this.pulse = false;
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new MembershipCard();
    }
}

