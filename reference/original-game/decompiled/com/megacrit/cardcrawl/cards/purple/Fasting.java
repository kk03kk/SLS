/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.watcher.EnergyDownPower;
import com.megacrit.cardcrawl.vfx.combat.FastingEffect;

public class Fasting
extends AbstractCard {
    public static final String ID = "Fasting2";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Fasting2");

    public Fasting() {
        super(ID, Fasting.cardStrings.NAME, "purple/power/fasting", 2, Fasting.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (p != null) {
            this.addToBot(new VFXAction(new FastingEffect(p.hb.cX, p.hb.cY, Color.CHARTREUSE)));
        }
        this.addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, this.magicNumber), this.magicNumber));
        this.addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, this.magicNumber), this.magicNumber));
        this.addToBot(new ApplyPowerAction(p, p, new EnergyDownPower(p, 1, true), 1));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Fasting();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
            this.initializeDescription();
        }
    }
}

