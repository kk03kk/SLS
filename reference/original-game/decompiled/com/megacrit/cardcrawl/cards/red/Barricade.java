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
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.BarricadePower;

public class Barricade
extends AbstractCard {
    public static final String ID = "Barricade";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Barricade");

    public Barricade() {
        super(ID, Barricade.cardStrings.NAME, "red/power/barricade", 3, Barricade.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.RED, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        boolean powerExists = false;
        for (AbstractPower pow : p.powers) {
            if (!pow.ID.equals(ID)) continue;
            powerExists = true;
            break;
        }
        if (!powerExists) {
            this.addToBot(new ApplyPowerAction(p, p, new BarricadePower(p)));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Barricade();
    }
}

