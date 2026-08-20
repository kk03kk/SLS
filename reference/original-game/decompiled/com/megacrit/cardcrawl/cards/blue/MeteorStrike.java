/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.vfx.combat.WeightyImpactEffect;

public class MeteorStrike
extends AbstractCard {
    public static final String ID = "Meteor Strike";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Meteor Strike");

    public MeteorStrike() {
        super(ID, MeteorStrike.cardStrings.NAME, "blue/attack/meteor_strike", 5, MeteorStrike.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 24;
        this.magicNumber = this.baseMagicNumber = 3;
        this.tags.add(AbstractCard.CardTags.STRIKE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new VFXAction(new WeightyImpactEffect(m.hb.cX, m.hb.cY)));
        }
        this.addToBot(new WaitAction(0.8f));
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.NONE));
        for (int i = 0; i < this.magicNumber; ++i) {
            this.addToBot(new ChannelAction(new Plasma()));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(6);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new MeteorStrike();
    }
}

