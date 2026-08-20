/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class JackOfAllTrades
extends AbstractCard {
    public static final String ID = "Jack Of All Trades";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Jack Of All Trades");

    public JackOfAllTrades() {
        super(ID, JackOfAllTrades.cardStrings.NAME, "colorless/skill/jack_of_all_trades", 0, JackOfAllTrades.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractCard c = AbstractDungeon.returnTrulyRandomColorlessCardInCombat(AbstractDungeon.cardRandomRng).makeCopy();
        this.addToBot(new MakeTempCardInHandAction(c, 1));
        if (this.upgraded) {
            c = AbstractDungeon.returnTrulyRandomColorlessCardInCombat(AbstractDungeon.cardRandomRng).makeCopy();
            this.addToBot(new MakeTempCardInHandAction(c, 1));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
            this.rawDescription = JackOfAllTrades.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new JackOfAllTrades();
    }
}

