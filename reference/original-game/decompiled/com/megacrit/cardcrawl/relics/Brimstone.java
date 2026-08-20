/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Brimstone
extends AbstractRelic {
    public static final String ID = "Brimstone";
    private static final int STR_AMT = 2;
    private static final int ENEMY_STR_AMT = 1;

    public Brimstone() {
        super(ID, "brimstone.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[1] + 1 + this.DESCRIPTIONS[2];
    }

    @Override
    public void atTurnStart() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new StrengthPower(AbstractDungeon.player, 2), 2));
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            this.addToTop(new ApplyPowerAction(m, m, new StrengthPower(m, 1), 1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Brimstone();
    }
}

