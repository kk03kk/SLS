/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BlueCandle
extends AbstractRelic {
    public static final String ID = "Blue Candle";
    public static final int HP_LOSS = 1;

    public BlueCandle() {
        super(ID, "blueCandle.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BlueCandle();
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.CURSE) {
            AbstractDungeon.player.getRelic(ID).flash();
            this.addToBot(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, 1, AbstractGameAction.AttackEffect.FIRE));
            card.exhaust = true;
            action.exhaustCard = true;
        }
    }
}

