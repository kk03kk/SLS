/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class MasterOfStrategy
extends AbstractCard {
    public static final String ID = "Master of Strategy";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Master of Strategy");

    public MasterOfStrategy() {
        super(ID, MasterOfStrategy.cardStrings.NAME, "colorless/skill/master_of_strategy", 0, MasterOfStrategy.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 3;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DrawCardAction(p, this.magicNumber));
    }

    @Override
    public AbstractCard makeCopy() {
        return new MasterOfStrategy();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }
}

