/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.PeacePipe;
import com.megacrit.cardcrawl.relics.Shovel;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.ui.campfire.LiftOption;
import java.util.ArrayList;

public class Girya
extends AbstractRelic {
    public static final String ID = "Girya";
    public static final int STR_LIMIT = 3;

    public Girya() {
        super(ID, "kettlebell.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.HEAVY);
        this.counter = 0;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
        if (this.counter != 0) {
            this.flash();
            this.addToTop(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new StrengthPower(AbstractDungeon.player, this.counter), this.counter));
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        }
    }

    @Override
    public boolean canSpawn() {
        if (AbstractDungeon.floorNum >= 48 && !Settings.isEndless) {
            return false;
        }
        int campfireRelicCount = 0;
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            if (!(r instanceof PeacePipe) && !(r instanceof Shovel) && !(r instanceof Girya)) continue;
            ++campfireRelicCount;
        }
        return campfireRelicCount < 2;
    }

    @Override
    public void addCampfireOption(ArrayList<AbstractCampfireOption> options) {
        options.add(new LiftOption(this.counter < 3));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Girya();
    }
}

