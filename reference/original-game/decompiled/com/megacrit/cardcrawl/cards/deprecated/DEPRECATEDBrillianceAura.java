/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDBrillianceAura
extends AbstractCard {
    public static final String ID = "BrillianceAura";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("BrillianceAura");

    public DEPRECATEDBrillianceAura() {
        super(ID, DEPRECATEDBrillianceAura.cardStrings.NAME, null, 2, DEPRECATEDBrillianceAura.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.selfRetain = true;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void atTurnStartPreDraw() {
        this.addToBot(new DrawCardAction(1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDBrillianceAura();
    }
}

