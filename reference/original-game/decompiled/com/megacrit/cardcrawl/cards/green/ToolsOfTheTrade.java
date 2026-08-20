/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ToolsOfTheTradePower;

public class ToolsOfTheTrade
extends AbstractCard {
    public static final String ID = "Tools of the Trade";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Tools of the Trade");

    public ToolsOfTheTrade() {
        super(ID, ToolsOfTheTrade.cardStrings.NAME, "green/power/tools_of_the_trade", 1, ToolsOfTheTrade.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new ToolsOfTheTradePower(p, 1), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(0);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new ToolsOfTheTrade();
    }
}

