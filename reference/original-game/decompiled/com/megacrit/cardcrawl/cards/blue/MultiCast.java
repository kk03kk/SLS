/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.unique.MulticastAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class MultiCast
extends AbstractCard {
    public static final String ID = "Multi-Cast";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Multi-Cast");

    public MultiCast() {
        super(ID, MultiCast.cardStrings.NAME, "blue/skill/multicast", -1, MultiCast.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.showEvokeValue = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new MulticastAction(p, this.energyOnUse, this.upgraded, this.freeToPlayOnce));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = MultiCast.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new MultiCast();
    }
}

