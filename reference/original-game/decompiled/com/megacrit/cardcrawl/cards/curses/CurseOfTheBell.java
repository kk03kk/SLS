/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class CurseOfTheBell
extends AbstractCard {
    public static final String ID = "CurseOfTheBell";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("CurseOfTheBell");

    public CurseOfTheBell() {
        super(ID, CurseOfTheBell.cardStrings.NAME, "curse/curse_of_the_bell", -2, CurseOfTheBell.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new CurseOfTheBell();
    }
}

