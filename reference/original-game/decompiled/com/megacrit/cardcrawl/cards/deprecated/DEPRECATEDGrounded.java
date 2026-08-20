/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.deprecated.DEPRECATEDGroundedPower;

public class DEPRECATEDGrounded
extends AbstractCard {
    public static final String ID = "Grounded";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Grounded");

    public DEPRECATEDGrounded() {
        super(ID, DEPRECATEDGrounded.cardStrings.NAME, null, 2, DEPRECATEDGrounded.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new DEPRECATEDGroundedPower(p)));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.updateCost(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDGrounded();
    }
}

