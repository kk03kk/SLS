/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;

public class ActionLogEntry {
    public AbstractGameAction.ActionType type;

    public ActionLogEntry(AbstractGameAction.ActionType type) {
        this.type = type;
    }

    public String toString() {
        return this.type.toString();
    }
}

