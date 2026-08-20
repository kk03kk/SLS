/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.AdrenalineEffect;

public class Adrenaline
extends AbstractCard {
    public static final String ID = "Adrenaline";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Adrenaline");

    public Adrenaline() {
        super(ID, Adrenaline.cardStrings.NAME, "green/skill/adrenaline", 0, Adrenaline.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new VFXAction(new AdrenalineEffect(), 0.15f));
        if (this.upgraded) {
            this.addToBot(new GainEnergyAction(2));
        } else {
            this.addToBot(new GainEnergyAction(1));
        }
        this.addToBot(new DrawCardAction(p, 2));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = Adrenaline.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Adrenaline();
    }
}

