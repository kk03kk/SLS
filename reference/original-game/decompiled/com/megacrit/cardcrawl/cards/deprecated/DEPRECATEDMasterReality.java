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
import com.megacrit.cardcrawl.powers.deprecated.DEPRECATEDMasterRealityPower;

public class DEPRECATEDMasterReality
extends AbstractCard {
    public static final String ID = "MasterReality";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("MasterReality");

    public DEPRECATEDMasterReality() {
        super(ID, DEPRECATEDMasterReality.cardStrings.NAME, "purple/power/master_reality", 1, DEPRECATEDMasterReality.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new DEPRECATEDMasterRealityPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDMasterReality();
    }
}

