/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class PoisonLoseHpAction
extends AbstractGameAction {
    private static final float DURATION = 0.33f;

    public PoisonLoseHpAction(AbstractCreature target, AbstractCreature source, int amount, AbstractGameAction.AttackEffect effect) {
        this.setValues(target, source, amount);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = effect;
        this.duration = 0.33f;
    }

    @Override
    public void update() {
        if (AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT) {
            this.isDone = true;
            return;
        }
        if (this.duration == 0.33f && this.target.currentHealth > 0) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, this.attackEffect));
        }
        this.tickDuration();
        if (this.isDone) {
            AbstractPower p;
            if (this.target.currentHealth > 0) {
                this.target.tint.color = Color.CHARTREUSE.cpy();
                this.target.tint.changeColor(Color.WHITE.cpy());
                this.target.damage(new DamageInfo(this.source, this.amount, DamageInfo.DamageType.HP_LOSS));
                if (this.target.isDying && ++AbstractPlayer.poisonKillCount == 3 && AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
                    UnlockTracker.unlockAchievement("PLAGUE");
                }
            }
            if ((p = this.target.getPower("Poison")) != null) {
                --p.amount;
                if (p.amount == 0) {
                    this.target.powers.remove(p);
                } else {
                    p.updateDescription();
                }
            }
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
            this.addToTop(new WaitAction(0.1f));
        }
    }
}

