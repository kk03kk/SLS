/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.unique.EscapePlanAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class EscapePlan
extends AbstractCard {
    public static final String ID = "Escape Plan";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Escape Plan");

    public EscapePlan() {
        super(ID, EscapePlan.cardStrings.NAME, "green/skill/escape_plan", 0, EscapePlan.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DrawCardAction(1, new EscapePlanAction(this.block)));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(2);
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new EscapePlan();
    }
}

