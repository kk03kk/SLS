/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class WristBlade
extends AbstractRelic {
    public static final String ID = "WristBlade";

    public WristBlade() {
        super(ID, "wBlade.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new WristBlade();
    }

    @Override
    public float atDamageModify(float damage, AbstractCard c) {
        if (c.costForTurn == 0 || c.freeToPlayOnce && c.cost != -1) {
            return damage + 4.0f;
        }
        return damage;
    }
}

