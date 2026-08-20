/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Tranquility
extends AbstractCard {
    public static final String ID = "ClearTheMind";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("ClearTheMind");

    public Tranquility() {
        super(ID, Tranquility.cardStrings.NAME, "purple/skill/tranquility", 1, Tranquility.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
        this.selfRetain = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ChangeStanceAction("Calm"));
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
        return new Tranquility();
    }
}

