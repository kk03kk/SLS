/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;

public class ThunderStrikeAction
extends AbstractGameAction {
    private DamageInfo info;
    private static final float DURATION = 0.01f;
    private static final float POST_ATTACK_WAIT_DUR = 0.2f;
    private int numTimes;

    public ThunderStrikeAction(AbstractCreature target, DamageInfo info, int numTimes) {
        this.info = info;
        this.target = target;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = AbstractGameAction.AttackEffect.NONE;
        this.duration = 0.01f;
        this.numTimes = numTimes;
    }

    @Override
    public void update() {
        if (this.target == null) {
            this.isDone = true;
            return;
        }
        if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
            AbstractDungeon.actionManager.clearPostCombatActions();
            this.isDone = true;
            return;
        }
        if (this.target.currentHealth > 0) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, this.attackEffect));
            AbstractDungeon.effectList.add(new LightningEffect(this.target.drawX, this.target.drawY));
            CardCrawlGame.sound.play("ORB_LIGHTNING_EVOKE", 0.1f);
            this.info.applyPowers(this.info.owner, this.target);
            this.target.damage(this.info);
            if (this.numTimes > 1 && !AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
                --this.numTimes;
                this.addToTop(new ThunderStrikeAction(AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng), this.info, this.numTimes));
            }
            this.addToTop(new WaitAction(0.2f));
        }
        this.isDone = true;
    }
}

