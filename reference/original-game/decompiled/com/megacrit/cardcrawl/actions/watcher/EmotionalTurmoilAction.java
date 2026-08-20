/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class EmotionalTurmoilAction
extends AbstractGameAction {
    public EmotionalTurmoilAction() {
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (AbstractDungeon.player.stance.ID.equals("Calm")) {
                this.addToBot(new ChangeStanceAction("Wrath"));
            } else if (AbstractDungeon.player.stance.ID.equals("Wrath")) {
                this.addToBot(new ChangeStanceAction("Calm"));
            }
            if (Settings.FAST_MODE) {
                this.isDone = true;
                return;
            }
        }
        this.tickDuration();
    }
}

