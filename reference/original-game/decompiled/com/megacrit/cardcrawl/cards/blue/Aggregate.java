/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.AggregateEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Aggregate
extends AbstractCard {
    public static final String ID = "Aggregate";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Aggregate");

    public Aggregate() {
        super(ID, Aggregate.cardStrings.NAME, "blue/skill/aggregate", 1, Aggregate.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new AggregateEnergyAction(this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(-1);
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Aggregate();
    }
}

