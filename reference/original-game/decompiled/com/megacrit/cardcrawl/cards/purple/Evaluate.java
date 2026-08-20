/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Insight;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Evaluate
extends AbstractCard {
    public static final String ID = "Evaluate";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Evaluate");

    public Evaluate() {
        super(ID, Evaluate.cardStrings.NAME, "purple/skill/evaluate", 1, Evaluate.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 6;
        this.cardsToPreview = new Insight();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        Insight card = new Insight();
        this.addToBot(new MakeTempCardInDrawPileAction(card, 1, true, true, false));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Evaluate();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(4);
        }
    }
}

