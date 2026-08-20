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
import com.megacrit.cardcrawl.powers.deprecated.DEPRECATEDHotHotPower;

public class DEPRECATEDHotHot
extends AbstractCard {
    public static final String ID = "HotHot";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("HotHot");
    public static final String UPGRADE_DESCRIPTION = DEPRECATEDHotHot.cardStrings.UPGRADE_DESCRIPTION;
    private static final int COST = 1;

    public DEPRECATEDHotHot() {
        super(ID, DEPRECATEDHotHot.cardStrings.NAME, null, 1, DEPRECATEDHotHot.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseMagicNumber = 6;
        this.magicNumber = 6;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new DEPRECATEDHotHotPower(p, this.magicNumber), this.magicNumber));
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
        return new DEPRECATEDHotHot();
    }
}

