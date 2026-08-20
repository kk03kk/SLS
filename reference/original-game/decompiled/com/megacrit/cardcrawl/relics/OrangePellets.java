/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.unique.RemoveDebuffsAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class OrangePellets
extends AbstractRelic {
    public static final String ID = "OrangePellets";
    private static boolean SKILL = false;
    private static boolean POWER = false;
    private static boolean ATTACK = false;

    public OrangePellets() {
        super(ID, "pellets.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atTurnStart() {
        SKILL = false;
        POWER = false;
        ATTACK = false;
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.ATTACK) {
            ATTACK = true;
        } else if (card.type == AbstractCard.CardType.SKILL) {
            SKILL = true;
        } else if (card.type == AbstractCard.CardType.POWER) {
            POWER = true;
        }
        if (ATTACK && SKILL && POWER) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new RemoveDebuffsAction(AbstractDungeon.player));
            SKILL = false;
            POWER = false;
            ATTACK = false;
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new OrangePellets();
    }
}

