/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.watcher.OmniscienceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Omniscience
extends AbstractCard {
    public static final String ID = "Omniscience";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Omniscience");

    public Omniscience() {
        super(ID, Omniscience.cardStrings.NAME, "purple/skill/omniscience", 4, Omniscience.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
        this.magicNumber = this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new OmniscienceAction(this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Omniscience();
    }
}

