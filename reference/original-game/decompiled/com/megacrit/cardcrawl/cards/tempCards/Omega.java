/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.tempCards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.OmegaPower;

public class Omega
extends AbstractCard {
    public static final String ID = "Omega";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Omega");

    public Omega() {
        super(ID, Omega.cardStrings.NAME, "colorless/power/omega", 3, Omega.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 50;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new OmegaPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(10);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Omega();
    }
}

