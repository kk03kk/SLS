/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.AnimateOrbAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.actions.defect.EvokeWithoutRemovingOrbAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Dualcast
extends AbstractCard {
    public static final String ID = "Dualcast";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Dualcast");

    public Dualcast() {
        super(ID, Dualcast.cardStrings.NAME, "blue/skill/dualcast", 1, Dualcast.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.BASIC, AbstractCard.CardTarget.NONE);
        this.showEvokeValue = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new AnimateOrbAction(1));
        this.addToBot(new EvokeWithoutRemovingOrbAction(1));
        this.addToBot(new AnimateOrbAction(1));
        this.addToBot(new EvokeOrbAction(1));
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
        return new Dualcast();
    }
}

