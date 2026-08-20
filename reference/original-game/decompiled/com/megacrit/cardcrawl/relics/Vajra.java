/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Vajra
extends AbstractRelic {
    public static final String ID = "Vajra";
    private static final int STR = 1;

    public Vajra() {
        super(ID, "vajra.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new StrengthPower(AbstractDungeon.player, 1), 1));
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Vajra();
    }
}

