/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.Girya;
import com.megacrit.cardcrawl.relics.Shovel;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.ui.campfire.TokeOption;
import java.util.ArrayList;

public class PeacePipe
extends AbstractRelic {
    public static final String ID = "Peace Pipe";

    public PeacePipe() {
        super(ID, "peacePipe.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
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
        options.add(new TokeOption(!CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()).isEmpty()));
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PeacePipe();
    }
}

