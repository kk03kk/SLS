/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class StrikeUpPower
extends AbstractPower {
    public static final String POWER_ID = "StrikeUp";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("StrikeUp");

    public StrikeUpPower(AbstractCreature owner, int amt) {
        this.name = StrikeUpPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("accuracy");
        this.updateExistingStrikes();
    }

    @Override
    public void updateDescription() {
        this.description = StrikeUpPower.powerStrings.DESCRIPTIONS[0] + this.amount + StrikeUpPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        this.updateExistingStrikes();
    }

    private void updateExistingStrikes() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!c.hasTag(AbstractCard.CardTags.STRIKE)) continue;
            c.baseDamage = CardLibrary.getCard((String)c.cardID).baseDamage + this.amount;
        }
        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (!c.hasTag(AbstractCard.CardTags.STRIKE)) continue;
            c.baseDamage = CardLibrary.getCard((String)c.cardID).baseDamage + this.amount;
        }
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (!c.hasTag(AbstractCard.CardTags.STRIKE)) continue;
            c.baseDamage = CardLibrary.getCard((String)c.cardID).baseDamage + this.amount;
        }
        for (AbstractCard c : AbstractDungeon.player.exhaustPile.group) {
            if (!c.hasTag(AbstractCard.CardTags.STRIKE)) continue;
            c.baseDamage = CardLibrary.getCard((String)c.cardID).baseDamage + this.amount;
        }
    }

    @Override
    public void onDrawOrDiscard() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!c.hasTag(AbstractCard.CardTags.STRIKE)) continue;
            c.baseDamage = CardLibrary.getCard((String)c.cardID).baseDamage + this.amount;
        }
    }
}

