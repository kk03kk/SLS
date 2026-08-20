/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;

public class AttackDamageRandomEnemyAction
extends AbstractGameAction {
    private AbstractCard card;
    private AbstractGameAction.AttackEffect effect;

    public AttackDamageRandomEnemyAction(AbstractCard card, AbstractGameAction.AttackEffect effect) {
        this.card = card;
        this.effect = effect;
    }

    public AttackDamageRandomEnemyAction(AbstractCard card) {
        this(card, AbstractGameAction.AttackEffect.NONE);
    }

    @Override
    public void update() {
        this.target = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);
        if (this.target != null) {
            this.card.calculateCardDamage((AbstractMonster)this.target);
            if (AbstractGameAction.AttackEffect.LIGHTNING == this.effect) {
                this.addToTop(new DamageAction(this.target, new DamageInfo(AbstractDungeon.player, this.card.damage, this.card.damageTypeForTurn), AbstractGameAction.AttackEffect.NONE));
                this.addToTop(new SFXAction("ORB_LIGHTNING_EVOKE", 0.1f));
                this.addToTop(new VFXAction(new LightningEffect(this.target.hb.cX, this.target.hb.cY)));
            } else {
                this.addToTop(new DamageAction(this.target, new DamageInfo(AbstractDungeon.player, this.card.damage, this.card.damageTypeForTurn), this.effect));
            }
        }
        this.isDone = true;
    }
}

