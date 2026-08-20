/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.trials;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.trials.AbstractTrial;
import java.util.ArrayList;
import java.util.List;

public class CustomTrial
extends AbstractTrial {
    private boolean isKeepingStarterRelic = true;
    private ArrayList<String> relicIds = new ArrayList();
    private boolean isKeepingStarterCards = true;
    private ArrayList<String> cardIds = new ArrayList();
    private boolean useRandomDailyMods;
    private ArrayList<String> dailyModIds = new ArrayList();
    private Integer maxHpOverride = null;

    public void setMaxHpOverride(int maxHp) {
        this.maxHpOverride = maxHp;
    }

    public void addStarterCards(List<String> moreCardIds) {
        this.cardIds.addAll(moreCardIds);
    }

    public void setStarterCards(List<String> starterCards) {
        this.cardIds.clear();
        this.cardIds.addAll(starterCards);
        this.isKeepingStarterCards = false;
    }

    public void addStarterRelic(String relicId) {
        this.relicIds.add(relicId);
    }

    public void addStarterRelics(List<String> moreRelics) {
        this.relicIds.addAll(moreRelics);
    }

    public void setStarterRelics(List<String> starterRelics) {
        this.relicIds.clear();
        this.relicIds.addAll(starterRelics);
        this.isKeepingStarterRelic = false;
    }

    public void setShouldKeepStarterRelic(boolean shouldKeep) {
        this.isKeepingStarterRelic = shouldKeep;
    }

    public void addDailyMod(String modId) {
        this.dailyModIds.add(modId);
    }

    public void addDailyMods(List<String> moreDailyMods) {
        this.dailyModIds.addAll(moreDailyMods);
    }

    public void setRandomDailyMods() {
        this.useRandomDailyMods = true;
    }

    @Override
    public AbstractPlayer setupPlayer(AbstractPlayer player) {
        if (this.maxHpOverride != null) {
            player.maxHealth = this.maxHpOverride;
            player.currentHealth = this.maxHpOverride;
        }
        return player;
    }

    @Override
    public boolean keepStarterRelic() {
        return this.isKeepingStarterRelic;
    }

    @Override
    public List<String> extraStartingRelicIDs() {
        return this.relicIds;
    }

    @Override
    public boolean keepsStarterCards() {
        return this.isKeepingStarterCards;
    }

    @Override
    public List<String> extraStartingCardIDs() {
        return this.cardIds;
    }

    @Override
    public boolean useRandomDailyMods() {
        return this.useRandomDailyMods;
    }

    @Override
    public ArrayList<String> dailyModIDs() {
        return this.dailyModIds;
    }
}

