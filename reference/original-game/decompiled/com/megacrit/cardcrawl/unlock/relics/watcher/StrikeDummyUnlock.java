/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.watcher;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class StrikeDummyUnlock
extends AbstractUnlock {
    public StrikeDummyUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("StrikeDummy");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

