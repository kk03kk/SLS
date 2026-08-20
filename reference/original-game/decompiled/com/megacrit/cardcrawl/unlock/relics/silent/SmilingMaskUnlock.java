/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.silent;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class SmilingMaskUnlock
extends AbstractUnlock {
    public SmilingMaskUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("Smiling Mask");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

