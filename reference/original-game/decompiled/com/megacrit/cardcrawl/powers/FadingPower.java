/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.SuicideAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.ExplosionSmallEffect;

public class FadingPower
extends AbstractPower {
    public static final String POWER_ID = "Fading";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Fading");
    public static final String NAME = FadingPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = FadingPower.powerStrings.DESCRIPTIONS;

    public FadingPower(AbstractCreature owner, int turns) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = turns;
        this.updateDescription();
        this.loadRegion("fading");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[2] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void duringTurn() {
        if (this.amount == 1 && !this.owner.isDying) {
            this.addToBot(new VFXAction(new ExplosionSmallEffect(this.owner.hb.cX, this.owner.hb.cY), 0.1f));
            this.addToBot(new SuicideAction((AbstractMonster)this.owner));
        } else {
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
            this.updateDescription();
        }
    }
}

