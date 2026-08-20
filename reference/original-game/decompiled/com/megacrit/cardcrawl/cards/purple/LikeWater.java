/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.LikeWaterPower;

public class LikeWater
extends AbstractCard {
    public static final String ID = "LikeWater";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("LikeWater");

    public LikeWater() {
        super(ID, LikeWater.cardStrings.NAME, "purple/power/like_water", 1, LikeWater.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new LikeWaterPower(p, this.magicNumber), this.magicNumber));
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
        return new LikeWater();
    }
}

