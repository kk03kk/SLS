/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.silent;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class ArtOfWarUnlock
extends AbstractUnlock {
    public ArtOfWarUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("Art of War");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

