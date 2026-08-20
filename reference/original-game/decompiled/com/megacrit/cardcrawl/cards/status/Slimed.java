/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.status;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Slimed
extends AbstractCard {
    public static final String ID = "Slimed";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Slimed");

    public Slimed() {
        super(ID, Slimed.cardStrings.NAME, "status/slimed", 1, Slimed.cardStrings.DESCRIPTION, AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Slimed();
    }
}

