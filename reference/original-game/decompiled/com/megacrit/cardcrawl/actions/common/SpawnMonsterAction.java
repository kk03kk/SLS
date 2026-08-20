/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.MinionPower;
import com.megacrit.cardcrawl.powers.SlowPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class SpawnMonsterAction
extends AbstractGameAction {
    private boolean used = false;
    private static final float DURATION = 0.1f;
    private AbstractMonster m;
    private boolean minion;
    private int targetSlot;
    private boolean useSmartPositioning;

    public SpawnMonsterAction(AbstractMonster m, boolean isMinion) {
        this(m, isMinion, -99);
        this.useSmartPositioning = true;
    }

    public SpawnMonsterAction(AbstractMonster m, boolean isMinion, int slot) {
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        this.duration = 0.1f;
        this.m = m;
        this.minion = isMinion;
        this.targetSlot = slot;
        this.useSmartPositioning = false;
    }

    @Override
    public void update() {
        if (!this.used) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onSpawnMonster(this.m);
            }
            this.m.init();
            this.m.applyPowers();
            if (this.useSmartPositioning) {
                int position = 0;
                for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    if (!(this.m.drawX > mo.drawX)) continue;
                    ++position;
                }
                AbstractDungeon.getCurrRoom().monsters.addMonster(position, this.m);
            } else {
                AbstractDungeon.getCurrRoom().monsters.addMonster(this.targetSlot, this.m);
            }
            this.m.showHealthBar();
            if (ModHelper.isModEnabled("Lethality")) {
                this.addToBot(new ApplyPowerAction(this.m, this.m, new StrengthPower(this.m, 3), 3));
            }
            if (ModHelper.isModEnabled("Time Dilation")) {
                this.addToBot(new ApplyPowerAction(this.m, this.m, new SlowPower(this.m, 0)));
            }
            if (this.minion) {
                this.addToTop(new ApplyPowerAction(this.m, this.m, new MinionPower(this.m)));
            }
            this.used = true;
        }
        this.tickDuration();
    }
}

