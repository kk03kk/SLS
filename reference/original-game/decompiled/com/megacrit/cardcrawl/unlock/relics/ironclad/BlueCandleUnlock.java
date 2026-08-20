/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.ironclad;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class BlueCandleUnlock
extends AbstractUnlock {
    public BlueCandleUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("Blue Candle");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

