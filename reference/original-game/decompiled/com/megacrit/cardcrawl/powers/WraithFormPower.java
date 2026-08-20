/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;

public class WraithFormPower
extends AbstractPower {
    public static final String POWER_ID = "Wraith Form v2";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Wraith Form v2");
    public static final String NAME = WraithFormPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = WraithFormPower.powerStrings.DESCRIPTIONS;

    public WraithFormPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("wraithForm");
        this.canGoNegative = true;
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new DexterityPower(AbstractDungeon.player, this.amount), this.amount));
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + -this.amount + DESCRIPTIONS[1];
    }
}

