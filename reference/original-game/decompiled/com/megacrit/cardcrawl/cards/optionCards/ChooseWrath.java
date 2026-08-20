/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.optionCards;

import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class ChooseWrath
extends AbstractCard {
    public static final String ID = "Wrath";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Wrath");

    public ChooseWrath() {
        super(ID, ChooseWrath.cardStrings.NAME, "colorless/skill/wrath", -2, ChooseWrath.cardStrings.DESCRIPTION, AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void onChoseThisOption() {
        this.addToBot(new ChangeStanceAction(ID));
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new ChooseWrath();
    }
}

