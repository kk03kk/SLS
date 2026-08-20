/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.optionCards;

import com.megacrit.cardcrawl.actions.common.GainGoldAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.SpotlightPlayerEffect;

public class FameAndFortune
extends AbstractCard {
    public static final String ID = "FameAndFortune";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("FameAndFortune");

    public FameAndFortune() {
        super(ID, FameAndFortune.cardStrings.NAME, "colorless/skill/fame_and_fortune", -2, FameAndFortune.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 25;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.onChoseThisOption();
    }

    @Override
    public void onChoseThisOption() {
        AbstractDungeon.effectList.add(new RainingGoldEffect(this.magicNumber * 2, true));
        AbstractDungeon.effectsQueue.add(new SpotlightPlayerEffect());
        this.addToBot(new GainGoldAction(this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(5);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new FameAndFortune();
    }
}

