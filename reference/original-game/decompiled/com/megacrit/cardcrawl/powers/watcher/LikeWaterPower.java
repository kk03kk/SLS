/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class LikeWaterPower
extends AbstractPower {
    public static final String POWER_ID = "LikeWaterPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("LikeWaterPower");

    public LikeWaterPower(AbstractCreature owner, int amt) {
        this.name = LikeWaterPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("like_water");
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        if (this.amount > 999) {
            this.amount = 999;
        }
        this.updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = LikeWaterPower.powerStrings.DESCRIPTIONS[0] + this.amount + LikeWaterPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurnPreEndTurnCards(boolean isPlayer) {
        if (isPlayer) {
            AbstractPlayer p = (AbstractPlayer)this.owner;
            if (p.stance.ID.equals("Calm")) {
                this.flash();
                this.addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
            }
        }
    }
}

