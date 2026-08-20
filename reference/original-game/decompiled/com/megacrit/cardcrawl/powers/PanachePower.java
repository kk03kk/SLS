/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class PanachePower
extends AbstractPower {
    public static final String POWER_ID = "Panache";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Panache");
    public static final String NAME = PanachePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = PanachePower.powerStrings.DESCRIPTIONS;
    public static final int CARD_AMT = 5;
    private int damage;

    public PanachePower(AbstractCreature owner, int damage) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = 5;
        this.damage = damage;
        this.updateDescription();
        this.loadRegion("panache");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] + this.damage + DESCRIPTIONS[2] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[3] + this.damage + DESCRIPTIONS[2];
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.damage += stackAmount;
        this.updateDescription();
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        --this.amount;
        if (this.amount == 0) {
            this.flash();
            this.amount = 5;
            this.addToBot(new DamageAllEnemiesAction((AbstractCreature)AbstractDungeon.player, DamageInfo.createDamageMatrix(this.damage, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
        }
        this.updateDescription();
    }

    @Override
    public void atStartOfTurn() {
        this.amount = 5;
        this.updateDescription();
    }
}

