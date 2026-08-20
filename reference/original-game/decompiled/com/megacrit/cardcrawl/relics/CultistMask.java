/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class CultistMask
extends AbstractRelic {
    public static final String ID = "CultistMask";

    public CultistMask() {
        super(ID, "cultistMask.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new SFXAction("VO_CULTIST_1A"));
        this.addToBot(new TalkAction(true, this.DESCRIPTIONS[1], 1.0f, 2.0f));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CultistMask();
    }
}

