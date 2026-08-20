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
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.ForesightPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.GiantEyeEffect;

public class Foresight
extends AbstractCard {
    public static final String ID = "Wireheading";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Wireheading");

    public Foresight() {
        super(ID, Foresight.cardStrings.NAME, "purple/power/foresight", 1, Foresight.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (p != null) {
            this.addToBot(new VFXAction(new BorderFlashEffect(Color.VIOLET, true)));
            this.addToBot(new VFXAction(new GiantEyeEffect(p.hb.cX, p.hb.cY + 300.0f * Settings.scale, new Color(1.0f, 0.8f, 1.0f, 0.0f))));
        }
        this.addToBot(new ApplyPowerAction(p, p, new ForesightPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Foresight();
    }
}

