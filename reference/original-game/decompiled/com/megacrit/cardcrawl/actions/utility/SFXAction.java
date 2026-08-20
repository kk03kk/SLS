/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;

public class SFXAction
extends AbstractGameAction {
    private String key;
    private float pitchVar = 0.0f;
    private boolean adjust = false;

    public SFXAction(String key) {
        this(key, 0.0f, false);
    }

    public SFXAction(String key, float pitchVar) {
        this(key, pitchVar, false);
    }

    public SFXAction(String key, float pitchVar, boolean pitchAdjust) {
        this.key = key;
        this.pitchVar = pitchVar;
        this.adjust = pitchAdjust;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        if (!this.adjust) {
            CardCrawlGame.sound.play(this.key, this.pitchVar);
        } else {
            CardCrawlGame.sound.playA(this.key, this.pitchVar);
        }
        this.isDone = true;
    }
}

