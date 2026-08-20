/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Writhe
extends AbstractCard {
    public static final String ID = "Writhe";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Writhe");

    public Writhe() {
        super(ID, Writhe.cardStrings.NAME, "curse/writhe", -2, Writhe.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.CURSE, AbstractCard.CardTarget.NONE);
        this.isInnate = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Writhe();
    }
}

