/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.status;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Wound
extends AbstractCard {
    public static final String ID = "Wound";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Wound");

    public Wound() {
        super(ID, Wound.cardStrings.NAME, "status/wound", -2, Wound.cardStrings.DESCRIPTION, AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Wound();
    }
}

