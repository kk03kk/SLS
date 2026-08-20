/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Distraction
extends AbstractCard {
    public static final String ID = "Distraction";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Distraction");

    public Distraction() {
        super(ID, Distraction.cardStrings.NAME, "green/skill/distraction", 1, Distraction.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractCard c = AbstractDungeon.returnTrulyRandomCardInCombat(AbstractCard.CardType.SKILL).makeCopy();
        c.setCostForTurn(-99);
        this.addToBot(new MakeTempCardInHandAction(c, true));
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
        return new Distraction();
    }
}

