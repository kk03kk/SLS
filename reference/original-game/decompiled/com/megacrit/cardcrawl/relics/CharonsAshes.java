/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class CharonsAshes
extends AbstractRelic {
    public static final String ID = "Charon's Ashes";
    public static final int DMG = 3;

    public CharonsAshes() {
        super(ID, "ashes.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onExhaust(AbstractCard card) {
        this.flash();
        this.addToTop(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(3, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.FIRE));
        for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (mo.isDead) continue;
            this.addToTop(new RelicAboveCreatureAction(mo, this));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CharonsAshes();
    }
}

