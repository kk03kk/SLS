/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.relics.defect;

import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class VirusUnlock
extends AbstractUnlock {
    public VirusUnlock() {
        this.type = AbstractUnlock.UnlockType.RELIC;
        this.relic = RelicLibrary.getRelic("Symbiotic Virus");
        this.key = this.relic.relicId;
        this.title = this.relic.name;
    }
}

