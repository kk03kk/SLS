/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Metamorphosis
extends AbstractCard {
    public static final String ID = "Metamorphosis";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Metamorphosis");

    public Metamorphosis() {
        super(ID, Metamorphosis.cardStrings.NAME, "colorless/skill/metamorphosis", 2, Metamorphosis.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 3;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.magicNumber; ++i) {
            AbstractCard card = AbstractDungeon.returnTrulyRandomCardInCombat(AbstractCard.CardType.ATTACK).makeCopy();
            if (card.cost > 0) {
                card.cost = 0;
                card.costForTurn = 0;
                card.isCostModified = true;
            }
            this.addToBot(new MakeTempCardInDrawPileAction(card, 1, true, true));
        }
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
        return new Metamorphosis();
    }
}

