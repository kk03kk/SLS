/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class StasisPower
extends AbstractPower {
    public static final String POWER_ID = "Stasis";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Stasis");
    private AbstractCard card;

    public StasisPower(AbstractCreature owner, AbstractCard card) {
        this.name = StasisPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.card = card;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("stasis");
    }

    @Override
    public void updateDescription() {
        this.description = StasisPower.powerStrings.DESCRIPTIONS[0] + FontHelper.colorString(this.card.name, "y") + StasisPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void onDeath() {
        if (AbstractDungeon.player.hand.size() != 10) {
            this.addToBot(new MakeTempCardInHandAction(this.card, false, true));
        } else {
            this.addToBot(new MakeTempCardInDiscardAction(this.card, true));
        }
    }
}

