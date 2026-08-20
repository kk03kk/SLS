/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.stances.AbstractStance;
import com.megacrit.cardcrawl.stances.CalmStance;
import com.megacrit.cardcrawl.stances.WrathStance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class DEPRECATEDRandomStanceAction
extends AbstractGameAction {
    public DEPRECATEDRandomStanceAction() {
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            ArrayList<AbstractStance> stances = new ArrayList<AbstractStance>();
            AbstractStance oldStance = AbstractDungeon.player.stance;
            if (!oldStance.ID.equals("Wrath")) {
                stances.add(new WrathStance());
            }
            if (!oldStance.ID.equals("Calm")) {
                stances.add(new CalmStance());
            }
            Collections.shuffle(stances, new Random(AbstractDungeon.cardRandomRng.randomLong()));
            this.addToBot(new ChangeStanceAction(((AbstractStance)stances.get((int)0)).ID));
            if (Settings.FAST_MODE) {
                this.isDone = true;
                return;
            }
        }
        this.tickDuration();
    }
}

