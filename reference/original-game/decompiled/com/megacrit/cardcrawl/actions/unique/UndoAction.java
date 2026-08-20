/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class UndoAction
extends AbstractGameAction {
    private AbstractPlayer p = AbstractDungeon.player;

    public UndoAction() {
        this.duration = Settings.ACTION_DUR_MED;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            if (GameActionManager.turn == 1) {
                this.isDone = true;
                return;
            }
            if (this.p.currentHealth < GameActionManager.playerHpLastTurn) {
                this.p.heal(GameActionManager.playerHpLastTurn - this.p.currentHealth, true);
            } else if (this.p.currentHealth > GameActionManager.playerHpLastTurn) {
                this.addToTop(new DamageAction((AbstractCreature)this.p, new DamageInfo(this.p, this.p.currentHealth - GameActionManager.playerHpLastTurn, DamageInfo.DamageType.HP_LOSS), AbstractGameAction.AttackEffect.FIRE));
            }
        }
        this.tickDuration();
    }
}

