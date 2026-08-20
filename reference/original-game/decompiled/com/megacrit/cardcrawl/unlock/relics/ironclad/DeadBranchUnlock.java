/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.ironclad;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class DeadBranchUnlock
extends AbstractUnlock {
    public DeadBranchUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("Dead Branch");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

