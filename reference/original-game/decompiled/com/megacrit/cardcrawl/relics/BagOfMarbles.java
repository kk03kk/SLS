/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BagOfMarbles
extends AbstractRelic {
    public static final String ID = "Bag of Marbles";
    private static final int WEAK = 1;

    public BagOfMarbles() {
        super(ID, "marbles.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
            this.addToBot(new RelicAboveCreatureAction(mo, this));
            this.addToBot(new ApplyPowerAction((AbstractCreature)mo, (AbstractCreature)AbstractDungeon.player, (AbstractPower)new VulnerablePower(mo, 1, false), 1, true));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BagOfMarbles();
    }
}

