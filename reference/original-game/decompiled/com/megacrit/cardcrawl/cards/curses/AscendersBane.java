/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class AscendersBane
extends AbstractCard {
    public static final String ID = "AscendersBane";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("AscendersBane");

    public AscendersBane() {
        super(ID, AscendersBane.cardStrings.NAME, "curse/ascenders_bane", -2, AscendersBane.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
        this.isEthereal = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new AscendersBane();
    }
}

