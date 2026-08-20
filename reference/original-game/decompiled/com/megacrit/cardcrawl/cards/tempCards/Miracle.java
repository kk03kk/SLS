/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.tempCards;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.MiracleEffect;

public class Miracle
extends AbstractCard {
    public static final String ID = "Miracle";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Miracle");

    public Miracle() {
        super(ID, Miracle.cardStrings.NAME, "colorless/skill/miracle", 0, Miracle.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
        this.selfRetain = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (!Settings.DISABLE_EFFECTS) {
            this.addToBot(new VFXAction(new BorderFlashEffect(Color.GOLDENROD, true)));
        }
        this.addToBot(new VFXAction(new MiracleEffect()));
        if (this.upgraded) {
            this.addToBot(new GainEnergyAction(2));
        } else {
            this.addToBot(new GainEnergyAction(1));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = Miracle.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Miracle();
    }
}

