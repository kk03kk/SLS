/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DiscardAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.ThrowDaggerEffect;

public class DaggerThrow
extends AbstractCard {
    public static final String ID = "Dagger Throw";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Dagger Throw");

    public DaggerThrow() {
        super(ID, DaggerThrow.cardStrings.NAME, "green/attack/dagger_throw", 1, DaggerThrow.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 9;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new VFXAction(new ThrowDaggerEffect(m.hb.cX, m.hb.cY)));
        }
        this.addToBot(new DamageAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn)));
        this.addToBot(new DrawCardAction(p, 1));
        this.addToBot(new DiscardAction(p, p, 1, false));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DaggerThrow();
    }
}

