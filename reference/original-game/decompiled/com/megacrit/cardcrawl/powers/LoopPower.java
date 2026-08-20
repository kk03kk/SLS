/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class LoopPower
extends AbstractPower {
    public static final String POWER_ID = "Loop";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Loop");
    public static final String NAME = LoopPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = LoopPower.powerStrings.DESCRIPTIONS;

    public LoopPower(AbstractCreature owner, int amt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("loop");
    }

    @Override
    public void atStartOfTurn() {
        if (!AbstractDungeon.player.orbs.isEmpty()) {
            this.flash();
            for (int i = 0; i < this.amount; ++i) {
                AbstractDungeon.player.orbs.get(0).onStartOfTurn();
                AbstractDungeon.player.orbs.get(0).onEndOfTurn();
            }
        }
    }

    @Override
    public void updateDescription() {
        this.description = this.amount <= 1 ? DESCRIPTIONS[0] : DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }
}

