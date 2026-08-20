/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Torii
extends AbstractRelic {
    public static final String ID = "Torii";
    private static final int THRESHOLD = 5;

    public Torii() {
        super(ID, "torii.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 5 + this.DESCRIPTIONS[1];
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.owner != null && info.type != DamageInfo.DamageType.HP_LOSS && info.type != DamageInfo.DamageType.THORNS && damageAmount > 1 && damageAmount <= 5) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            return 1;
        }
        return damageAmount;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Torii();
    }
}

