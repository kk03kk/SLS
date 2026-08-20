/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.trials;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.trials.AbstractTrial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestTrial
extends AbstractTrial {
    @Override
    public AbstractPlayer setupPlayer(AbstractPlayer player) {
        player.maxHealth = 20;
        player.currentHealth = 10;
        player.gold = 777;
        return player;
    }

    @Override
    public boolean keepStarterRelic() {
        return false;
    }

    @Override
    public List<String> extraStartingRelicIDs() {
        return Arrays.asList("Derp Rock", "Unceasing Top");
    }

    @Override
    public boolean keepsStarterCards() {
        return true;
    }

    @Override
    public List<String> extraStartingCardIDs() {
        return Arrays.asList("Demon Form", "Wraith Form v2", "Echo Form");
    }

    @Override
    public boolean useRandomDailyMods() {
        return false;
    }

    @Override
    public ArrayList<String> dailyModIDs() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Diverse");
        retVal.add("Lethality");
        retVal.add("Time Dilation");
        retVal.add("Cursed Run");
        retVal.add("Elite Swarm");
        return retVal;
    }
}

