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
import com.megacrit.cardcrawl.powers.NextTurnBlockPower;

public class DEPRECATEDPrediction
extends AbstractCard {
    public static final String ID = "Prediction";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Prediction");

    public DEPRECATEDPrediction() {
        super(ID, DEPRECATEDPrediction.cardStrings.NAME, null, 1, DEPRECATEDPrediction.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 12;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new NextTurnBlockPower(p, this.block), this.block));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(4);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDPrediction();
    }
}

