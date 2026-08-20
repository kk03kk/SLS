/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;

public class ChooseOneAction
extends AbstractGameAction {
    private ArrayList<AbstractCard> choices;

    public ChooseOneAction(ArrayList<AbstractCard> choices) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.choices = choices;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.chooseOneOpen(this.choices);
            this.tickDuration();
            return;
        }
        this.tickDuration();
    }
}

