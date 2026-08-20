/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.defect;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class DataDiskUnlock
extends AbstractUnlock {
    public DataDiskUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("DataDisk");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

