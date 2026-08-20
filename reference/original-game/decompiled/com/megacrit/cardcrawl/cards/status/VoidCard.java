/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.status;

import com.megacrit.cardcrawl.actions.unique.LoseEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class VoidCard
extends AbstractCard {
    public static final String ID = "Void";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Void");

    public VoidCard() {
        super(ID, VoidCard.cardStrings.NAME, "status/void", -2, VoidCard.cardStrings.DESCRIPTION, AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);
        this.isEthereal = true;
    }

    @Override
    public void triggerWhenDrawn() {
        this.addToBot(new LoseEnergyAction(1));
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new VoidCard();
    }
}

