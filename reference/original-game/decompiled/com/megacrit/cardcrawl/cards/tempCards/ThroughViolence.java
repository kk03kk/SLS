/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.tempCards;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.ViolentAttackEffect;

public class ThroughViolence
extends AbstractCard {
    public static final String ID = "ThroughViolence";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("ThroughViolence");

    public ThroughViolence() {
        super(ID, ThroughViolence.cardStrings.NAME, "colorless/attack/through_violence", 0, ThroughViolence.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 20;
        this.selfRetain = true;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            if (Settings.FAST_MODE) {
                this.addToBot(new VFXAction(new ViolentAttackEffect(m.hb.cX, m.hb.cY, Color.VIOLET)));
            } else {
                this.addToBot(new VFXAction(new ViolentAttackEffect(m.hb.cX, m.hb.cY, Color.VIOLET), 0.4f));
            }
        }
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.SLASH_HEAVY));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(10);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new ThroughViolence();
    }
}

