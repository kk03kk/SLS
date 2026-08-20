/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.watcher.ChooseOneAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.optionCards.ChooseCalm;
import com.megacrit.cardcrawl.cards.optionCards.ChooseWrath;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import java.util.ArrayList;

public class DEPRECATEDPolymath
extends AbstractCard {
    public static final String ID = "Polymath";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Polymath");

    public DEPRECATEDPolymath() {
        super(ID, DEPRECATEDPolymath.cardStrings.NAME, null, 1, DEPRECATEDPolymath.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        ArrayList<AbstractCard> stanceChoices = new ArrayList<AbstractCard>();
        stanceChoices.add(new ChooseWrath());
        stanceChoices.add(new ChooseCalm());
        this.addToBot(new ChooseOneAction(stanceChoices));
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
        return new DEPRECATEDPolymath();
    }
}

