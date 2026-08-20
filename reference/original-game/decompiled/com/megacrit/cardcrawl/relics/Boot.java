/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Boot
extends AbstractRelic {
    public static final String ID = "Boot";
    private static final int THRESHOLD = 5;

    public Boot() {
        super(ID, "boot.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 4 + this.DESCRIPTIONS[1];
    }

    @Override
    public int onAttackToChangeDamage(DamageInfo info, int damageAmount) {
        if (info.owner != null && info.type != DamageInfo.DamageType.HP_LOSS && info.type != DamageInfo.DamageType.THORNS && damageAmount > 0 && damageAmount < 5) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            return 5;
        }
        return damageAmount;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Boot();
    }
}

