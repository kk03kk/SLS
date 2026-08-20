/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.actions.watcher.JudgementAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.GiantTextEffect;
import com.megacrit.cardcrawl.vfx.combat.WeightyImpactEffect;

public class Judgement
extends AbstractCard {
    public static final String ID = "Judgement";
    public static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Judgement");

    public Judgement() {
        super(ID, Judgement.cardStrings.NAME, "purple/skill/judgment", 1, Judgement.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ENEMY);
        this.baseMagicNumber = 30;
        this.magicNumber = 30;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new VFXAction(new WeightyImpactEffect(m.hb.cX, m.hb.cY, Color.GOLD.cpy())));
            this.addToBot(new WaitAction(0.8f));
            this.addToBot(new VFXAction(new GiantTextEffect(m.hb.cX, m.hb.cY)));
        }
        this.addToBot(new JudgementAction(m, this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(10);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Judgement();
    }
}

