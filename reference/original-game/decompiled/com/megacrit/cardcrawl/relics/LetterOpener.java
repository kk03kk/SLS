/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class LetterOpener
extends AbstractRelic {
    public static final String ID = "Letter Opener";
    private static final int DAMAGE = 5;

    public LetterOpener() {
        super(ID, "letterOpener.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 5 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atTurnStart() {
        this.counter = 0;
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.SKILL) {
            ++this.counter;
            if (this.counter % 3 == 0) {
                this.flash();
                this.counter = 0;
                this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
                this.addToBot(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(5, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.SLASH_HEAVY));
            }
        }
    }

    @Override
    public void onVictory() {
        this.counter = -1;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new LetterOpener();
    }
}

