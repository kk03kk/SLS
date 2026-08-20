/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.BiteEffect;

public class Bite
extends AbstractCard {
    public static final String ID = "Bite";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Bite");

    public Bite() {
        super(ID, Bite.cardStrings.NAME, "colorless/attack/bite", 1, Bite.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 7;
        this.magicNumber = this.baseMagicNumber = 2;
        this.tags.add(AbstractCard.CardTags.HEALING);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            if (Settings.FAST_MODE) {
                this.addToBot(new VFXAction(new BiteEffect(m.hb.cX, m.hb.cY - 40.0f * Settings.scale, Settings.GOLD_COLOR.cpy()), 0.1f));
            } else {
                this.addToBot(new VFXAction(new BiteEffect(m.hb.cX, m.hb.cY - 40.0f * Settings.scale, Settings.GOLD_COLOR.cpy()), 0.3f));
            }
        }
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.NONE));
        this.addToBot(new HealAction(p, p, this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(1);
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Bite();
    }
}

