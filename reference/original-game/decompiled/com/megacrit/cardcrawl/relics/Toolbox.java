/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.ChooseOneColorless;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Toolbox
extends AbstractRelic {
    public static final String ID = "Toolbox";

    public Toolbox() {
        super(ID, "toolbox.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStartPreDraw() {
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new ChooseOneColorless());
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Toolbox();
    }
}

