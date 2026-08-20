/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDWisdom
extends AbstractCard {
    public static final String ID = "Wisdom";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Wisdom");

    public DEPRECATEDWisdom() {
        super(ID, DEPRECATEDWisdom.cardStrings.NAME, null, 1, DEPRECATEDWisdom.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 2;
        this.selfRetain = true;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DrawCardAction(this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDWisdom();
    }
}

