/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.DevotionPower;
import com.megacrit.cardcrawl.vfx.combat.DevotionEffect;

public class Devotion
extends AbstractCard {
    public static final String ID = "Devotion";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Devotion");

    public Devotion() {
        super(ID, Devotion.cardStrings.NAME, "purple/power/devotion", 1, Devotion.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new SFXAction("HEAL_2", -0.4f, true));
        float doop = 0.8f;
        if (Settings.FAST_MODE) {
            doop = 0.0f;
        }
        this.addToBot(new VFXAction(new DevotionEffect(), doop));
        this.addToBot(new ApplyPowerAction(p, p, new DevotionPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Devotion();
    }

    @Override
    public void initializeDescription() {
        super.initializeDescription();
        this.keywords.add(GameDictionary.ENLIGHTENMENT.NAMES[0].toLowerCase());
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }
}

