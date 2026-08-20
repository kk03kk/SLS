/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.trials;

import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import java.util.ArrayList;
import java.util.List;

public class AbstractTrial {
    public String name;
    public AbstractPlayer.PlayerClass c;
    public int energy;
    public CardGroup deck;
    public ArrayList<AbstractRelic> relics = new ArrayList();

    public AbstractPlayer setupPlayer(AbstractPlayer player) {
        return player;
    }

    public boolean keepStarterRelic() {
        return true;
    }

    public List<String> extraStartingRelicIDs() {
        return new ArrayList<String>();
    }

    public boolean keepsStarterCards() {
        return true;
    }

    public List<String> extraStartingCardIDs() {
        return new ArrayList<String>();
    }

    public boolean useRandomDailyMods() {
        return false;
    }

    public ArrayList<String> dailyModIDs() {
        return new ArrayList<String>();
    }
}

