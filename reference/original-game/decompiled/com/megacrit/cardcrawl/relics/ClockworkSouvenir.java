/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class ClockworkSouvenir
extends AbstractRelic {
    public static final String ID = "ClockworkSouvenir";

    public ClockworkSouvenir() {
        super(ID, "clockwork.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new ArtifactPower(AbstractDungeon.player, 1), 1));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new ClockworkSouvenir();
    }
}

