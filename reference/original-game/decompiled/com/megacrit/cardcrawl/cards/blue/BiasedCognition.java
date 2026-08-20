/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.BiasPower;
import com.megacrit.cardcrawl.powers.FocusPower;

public class BiasedCognition
extends AbstractCard {
    public static final String ID = "Biased Cognition";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Biased Cognition");

    public BiasedCognition() {
        super(ID, BiasedCognition.cardStrings.NAME, "blue/power/biased_cognition", 1, BiasedCognition.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new FocusPower(p, this.magicNumber), this.magicNumber));
        this.addToBot(new ApplyPowerAction(p, p, new BiasPower(p, 1), 1));
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
        return new BiasedCognition();
    }
}

