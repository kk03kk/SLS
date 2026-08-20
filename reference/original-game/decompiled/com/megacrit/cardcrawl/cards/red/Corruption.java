/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.CorruptionPower;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.VerticalAuraEffect;

public class Corruption
extends AbstractCard {
    public static final String ID = "Corruption";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Corruption");

    public Corruption() {
        super(ID, Corruption.cardStrings.NAME, "red/power/corruption", 3, Corruption.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.RED, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new VFXAction(p, new VerticalAuraEffect(Color.BLACK, p.hb.cX, p.hb.cY), 0.33f));
        this.addToBot(new SFXAction("ATTACK_FIRE"));
        this.addToBot(new VFXAction(p, new VerticalAuraEffect(Color.PURPLE, p.hb.cX, p.hb.cY), 0.33f));
        this.addToBot(new VFXAction(p, new VerticalAuraEffect(Color.CYAN, p.hb.cX, p.hb.cY), 0.0f));
        this.addToBot(new VFXAction(p, new BorderLongFlashEffect(Color.MAGENTA), 0.0f, true));
        boolean powerExists = false;
        for (AbstractPower pow : p.powers) {
            if (!pow.ID.equals(ID)) continue;
            powerExists = true;
            break;
        }
        if (!powerExists) {
            this.addToBot(new ApplyPowerAction(p, p, new CorruptionPower(p)));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Corruption();
    }
}

