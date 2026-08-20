/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.watcher.TriggerMarksAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.MarkPower;
import com.megacrit.cardcrawl.vfx.combat.PressurePointEffect;

public class PressurePoints
extends AbstractCard {
    public static final String ID = "PathToVictory";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("PathToVictory");

    public PressurePoints() {
        super(ID, PressurePoints.cardStrings.NAME, "purple/skill/pressure_points", 1, PressurePoints.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.magicNumber = this.baseMagicNumber = 8;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new VFXAction(new PressurePointEffect(m.hb.cX, m.hb.cY)));
        }
        this.addToBot(new ApplyPowerAction(m, p, new MarkPower(m, this.magicNumber), this.magicNumber));
        this.addToBot(new TriggerMarksAction(this));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new PressurePoints();
    }
}

