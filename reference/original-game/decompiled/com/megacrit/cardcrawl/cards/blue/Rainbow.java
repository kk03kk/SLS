/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Dark;
import com.megacrit.cardcrawl.orbs.Frost;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.vfx.RainbowCardEffect;

public class Rainbow
extends AbstractCard {
    public static final String ID = "Rainbow";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Rainbow");

    public Rainbow() {
        super(ID, Rainbow.cardStrings.NAME, "blue/skill/rainbow", 2, Rainbow.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.showEvokeValue = true;
        this.showEvokeOrbCount = 3;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new VFXAction(new RainbowCardEffect()));
        this.addToBot(new ChannelAction(new Lightning()));
        this.addToBot(new ChannelAction(new Frost()));
        this.addToBot(new ChannelAction(new Dark()));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Rainbow();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.exhaust = false;
            this.rawDescription = Rainbow.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}

