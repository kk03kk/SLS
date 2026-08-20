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
import com.megacrit.cardcrawl.powers.AmplifyPower;

public class Amplify
extends AbstractCard {
    public static final String ID = "Amplify";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Amplify");

    public Amplify() {
        super(ID, Amplify.cardStrings.NAME, "blue/skill/amplify", 1, Amplify.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new AmplifyPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
            this.rawDescription = Amplify.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Amplify();
    }
}

