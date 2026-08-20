/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics.deprecated;

import com.megacrit.cardcrawl.relics.AbstractRelic;

public class DEPRECATED_DarkCore
extends AbstractRelic {
    public static final String ID = "Dark Core";

    public DEPRECATED_DarkCore() {
        super(ID, "vCore.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new DEPRECATED_DarkCore();
    }
}

