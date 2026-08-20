/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics.deprecated;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.LoseStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class DEPRECATEDYin
extends AbstractRelic {
    public static final String ID = "Yin";

    public DEPRECATEDYin() {
        super(ID, "yin.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.SKILL) {
            this.flash();
            AbstractPlayer p = AbstractDungeon.player;
            this.addToBot(new RelicAboveCreatureAction(p, this));
            this.addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, 1), 1));
            this.addToBot(new ApplyPowerAction(p, p, new LoseStrengthPower(p, 1), 1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new DEPRECATEDYin();
    }
}

