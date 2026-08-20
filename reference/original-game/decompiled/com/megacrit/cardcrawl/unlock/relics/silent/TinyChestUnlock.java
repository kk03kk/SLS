/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.silent;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class TinyChestUnlock
extends AbstractUnlock {
    public TinyChestUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("Tiny Chest");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

