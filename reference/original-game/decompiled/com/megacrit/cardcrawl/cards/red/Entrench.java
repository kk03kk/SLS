/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.unique.DoubleYourBlockAction;
import com.megacrit.cardcrawl.actions.utility.ExhaustAllEtherealAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Entrench
extends AbstractCard {
    public static final String ID = "Entrench";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Entrench");

    public Entrench() {
        super(ID, Entrench.cardStrings.NAME, "red/skill/entrench", 2, Entrench.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DoubleYourBlockAction(p));
    }

    @Override
    public void triggerOnEndOfPlayerTurn() {
        this.addToTop(new ExhaustAllEtherealAction());
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(1);
            this.rawDescription = Entrench.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Entrench();
    }
}

