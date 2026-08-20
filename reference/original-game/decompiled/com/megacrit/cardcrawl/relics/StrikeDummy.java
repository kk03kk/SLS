/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class StrikeDummy
extends AbstractRelic {
    public static final String ID = "StrikeDummy";

    public StrikeDummy() {
        super(ID, "dummy.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public float atDamageModify(float damage, AbstractCard c) {
        if (c.hasTag(AbstractCard.CardTags.STRIKE)) {
            return damage + 3.0f;
        }
        return damage;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new StrikeDummy();
    }
}

