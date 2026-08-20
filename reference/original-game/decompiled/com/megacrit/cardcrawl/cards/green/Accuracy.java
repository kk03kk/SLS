/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AccuracyPower;

public class Accuracy
extends AbstractCard {
    public static final String ID = "Accuracy";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Accuracy");

    public Accuracy() {
        super(ID, Accuracy.cardStrings.NAME, "green/power/accuracy", 1, Accuracy.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 4;
        this.cardsToPreview = new Shiv();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new AccuracyPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Accuracy();
    }
}

