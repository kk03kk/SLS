/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class NextTurnBlockPower
extends AbstractPower {
    public static final String POWER_ID = "Next Turn Block";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Next Turn Block");
    public static final String NAME = NextTurnBlockPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = NextTurnBlockPower.powerStrings.DESCRIPTIONS;

    public NextTurnBlockPower(AbstractCreature owner, int armorAmt, String newName) {
        this.name = newName;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = armorAmt;
        this.updateDescription();
        this.loadRegion("defenseNext");
    }

    public NextTurnBlockPower(AbstractCreature owner, int armorAmt) {
        this(owner, armorAmt, NAME);
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void atStartOfTurn() {
        this.flash();
        AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.owner.hb.cX, this.owner.hb.cY, AbstractGameAction.AttackEffect.SHIELD));
        this.addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
    }
}

