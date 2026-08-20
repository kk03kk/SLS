/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.watcher;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class CeramicFishUnlock
extends AbstractUnlock {
    public CeramicFishUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("CeramicFish");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

