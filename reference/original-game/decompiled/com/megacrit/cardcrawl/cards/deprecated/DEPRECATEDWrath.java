/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDWrath
extends AbstractCard {
    public static final String ID = "Wrath";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Wrath");

    public DEPRECATEDWrath() {
        super(ID, DEPRECATEDWrath.cardStrings.NAME, null, 1, DEPRECATEDWrath.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ChangeStanceAction(ID));
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDWrath();
    }

    @Override
    public void upgrade() {
        this.upgradeName();
        this.upgradeBaseCost(0);
    }
}

