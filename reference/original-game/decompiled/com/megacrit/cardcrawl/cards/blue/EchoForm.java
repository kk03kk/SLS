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
import com.megacrit.cardcrawl.powers.EchoPower;

public class EchoForm
extends AbstractCard {
    public static final String ID = "Echo Form";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Echo Form");

    public EchoForm() {
        super(ID, EchoForm.cardStrings.NAME, "blue/power/echo_form", 3, EchoForm.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.isEthereal = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new EchoPower(p, 1), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.isEthereal = false;
            this.upgradeName();
            this.rawDescription = EchoForm.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new EchoForm();
    }
}

