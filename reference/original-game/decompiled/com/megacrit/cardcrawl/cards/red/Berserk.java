/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.BerserkPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;

public class Berserk
extends AbstractCard {
    public static final String ID = "Berserk";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Berserk");

    public Berserk() {
        super(ID, Berserk.cardStrings.NAME, "red/power/berserk", 0, Berserk.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.RED, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new VulnerablePower(p, this.magicNumber, false), this.magicNumber));
        this.addToBot(new ApplyPowerAction(p, p, new BerserkPower(p, 1), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(-1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Berserk();
    }
}

