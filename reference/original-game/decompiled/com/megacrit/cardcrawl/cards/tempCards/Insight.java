/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.tempCards;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.LightBulbEffect;

public class Insight
extends AbstractCard {
    public static final String ID = "Insight";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Insight");

    public Insight() {
        super(ID, Insight.cardStrings.NAME, "colorless/skill/insight", 0, Insight.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 2;
        this.exhaust = true;
        this.selfRetain = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (Settings.FAST_MODE) {
            this.addToBot(new VFXAction(new LightBulbEffect(p.hb)));
        } else {
            this.addToBot(new VFXAction(new LightBulbEffect(p.hb), 0.2f));
        }
        this.addToBot(new DrawCardAction(p, this.magicNumber));
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
        return new Insight();
    }
}

