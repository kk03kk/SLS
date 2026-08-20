/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ConfusionPower
extends AbstractPower {
    public static final String POWER_ID = "Confusion";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Confusion");
    public static final String NAME = ConfusionPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ConfusionPower.powerStrings.DESCRIPTIONS;

    public ConfusionPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("confusion");
        this.type = AbstractPower.PowerType.DEBUFF;
        this.priority = 0;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_CONFUSION", 0.05f);
    }

    @Override
    public void onCardDraw(AbstractCard card) {
        if (card.cost >= 0) {
            int newCost = AbstractDungeon.cardRandomRng.random(3);
            if (card.cost != newCost) {
                card.costForTurn = card.cost = newCost;
                card.isCostModified = true;
            }
            card.freeToPlayOnce = false;
        }
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}

