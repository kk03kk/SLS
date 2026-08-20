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
import com.megacrit.cardcrawl.powers.RagePower;
import com.megacrit.cardcrawl.vfx.combat.ShockWaveEffect;

public class Rage
extends AbstractCard {
    public static final String ID = "Rage";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Rage");

    public Rage() {
        super(ID, Rage.cardStrings.NAME, "red/skill/rage", 0, Rage.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new SFXAction("RAGE"));
        this.addToBot(new VFXAction(p, new ShockWaveEffect(p.hb.cX, p.hb.cY, Color.ORANGE, ShockWaveEffect.ShockWaveType.CHAOTIC), 1.0f));
        this.addToBot(new ApplyPowerAction(p, p, new RagePower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Rage();
    }
}

