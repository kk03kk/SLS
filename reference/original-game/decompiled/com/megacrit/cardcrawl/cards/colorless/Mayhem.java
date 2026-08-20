/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.MayhemPower;

public class Mayhem
extends AbstractCard {
    public static final String ID = "Mayhem";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Mayhem");

    public Mayhem() {
        super(ID, Mayhem.cardStrings.NAME, "colorless/power/mayhem", 2, Mayhem.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new MayhemPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Mayhem();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(1);
        }
    }
}

