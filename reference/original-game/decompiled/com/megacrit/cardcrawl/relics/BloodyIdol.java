/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BloodyIdol
extends AbstractRelic {
    public static final String ID = "Bloody Idol";
    private static final int HEAL_AMOUNT = 5;

    public BloodyIdol() {
        super(ID, "bloodyChalice.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 5 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onGainGold() {
        this.flash();
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        AbstractDungeon.player.heal(5, true);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BloodyIdol();
    }
}

