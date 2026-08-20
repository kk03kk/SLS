/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.runHistory;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.screens.runHistory.RunPathElement;
import com.megacrit.cardcrawl.screens.stats.BattleStats;
import com.megacrit.cardcrawl.screens.stats.CampfireChoice;
import com.megacrit.cardcrawl.screens.stats.CardChoiceStats;
import com.megacrit.cardcrawl.screens.stats.EventStats;
import com.megacrit.cardcrawl.screens.stats.ObtainStats;
import com.megacrit.cardcrawl.screens.stats.RunData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class RunHistoryPath {
    public static final String BOSS_TREASURE_LABEL = "T!";
    public static final String HEART_LABEL = "<3";
    public static final int MAX_NODES_PER_ROW = 20;
    private RunData runData;
    public List<RunPathElement> pathElements = new ArrayList<RunPathElement>();
    private int rows = 0;

    public void setRunData(RunData newData) {
        this.runData = newData;
        ArrayList<String> labels = new ArrayList<String>();
        int choiceIndex = 0;
        int outcomeIndex = 0;
        List<String> choices = this.runData.path_taken;
        List<String> outcomes = this.runData.path_per_floor;
        if (choices == null) {
            choices = new LinkedList<String>();
        }
        if (outcomes == null) {
            outcomes = new LinkedList<String>();
        }
        int bossTreasureCount = 0;
        while (choiceIndex < choices.size() || outcomeIndex < outcomes.size()) {
            Object outcome;
            String choice = choiceIndex < choices.size() ? choices.get(choiceIndex) : "_";
            Object object = outcome = outcomeIndex < outcomes.size() ? outcomes.get(outcomeIndex) : "_";
            if (outcome == null) {
                ++outcomeIndex;
                if (bossTreasureCount < 2) {
                    labels.add(BOSS_TREASURE_LABEL);
                    ++bossTreasureCount;
                    continue;
                }
                if (bossTreasureCount == 2) {
                    labels.add(HEART_LABEL);
                    ++bossTreasureCount;
                    continue;
                }
                labels.add("null");
                continue;
            }
            if (choice.equals("?") && !((String)outcome).equals("?")) {
                labels.add("?(" + (String)outcome + ")");
            } else {
                labels.add(choice);
            }
            ++choiceIndex;
            ++outcomeIndex;
        }
        HashMap<Integer, BattleStats> battlesByFloor = new HashMap<Integer, BattleStats>();
        for (BattleStats battleStats : this.runData.damage_taken) {
            battlesByFloor.put(battleStats.floor, battleStats);
        }
        HashMap<Integer, EventStats> eventsByFloor = new HashMap<Integer, EventStats>();
        for (EventStats eventStats : this.runData.event_choices) {
            eventsByFloor.put(eventStats.floor, eventStats);
        }
        HashMap<Integer, CardChoiceStats> hashMap = new HashMap<Integer, CardChoiceStats>();
        for (CardChoiceStats cardChoiceStats : this.runData.card_choices) {
            hashMap.put(cardChoiceStats.floor, cardChoiceStats);
        }
        HashMap hashMap2 = new HashMap();
        if (this.runData.relics_obtained != null) {
            for (ObtainStats obtainStats : this.runData.relics_obtained) {
                if (!hashMap2.containsKey(obtainStats.floor)) {
                    hashMap2.put(obtainStats.floor, new ArrayList());
                }
                ((List)hashMap2.get(obtainStats.floor)).add(obtainStats.key);
            }
        }
        HashMap hashMap3 = new HashMap();
        if (this.runData.potions_obtained != null) {
            for (ObtainStats obtainStats : this.runData.potions_obtained) {
                if (!hashMap3.containsKey(obtainStats.floor)) {
                    hashMap3.put(obtainStats.floor, new ArrayList());
                }
                ((List)hashMap3.get(obtainStats.floor)).add(obtainStats.key);
            }
        }
        HashMap<Integer, CampfireChoice> hashMap4 = new HashMap<Integer, CampfireChoice>();
        if (this.runData.campfire_choices != null) {
            for (CampfireChoice choice : this.runData.campfire_choices) {
                hashMap4.put(choice.floor, choice);
            }
        }
        HashMap hashMap5 = new HashMap();
        if (this.runData.items_purchased != null && this.runData.item_purchase_floors != null && this.runData.items_purchased.size() == this.runData.item_purchase_floors.size()) {
            for (int i = 0; i < this.runData.items_purchased.size(); ++i) {
                int floor = this.runData.item_purchase_floors.get(i);
                String key = this.runData.items_purchased.get(i);
                if (!hashMap5.containsKey(floor)) {
                    hashMap5.put(floor, new ArrayList());
                }
                ((ArrayList)hashMap5.get(floor)).add(key);
            }
        }
        HashMap purgesByFloor = new HashMap();
        if (this.runData.items_purged != null && this.runData.items_purged_floors != null && this.runData.items_purged.size() == this.runData.items_purged_floors.size()) {
            for (int i = 0; i < this.runData.items_purged.size(); ++i) {
                int floor = this.runData.items_purged_floors.get(i);
                String key = this.runData.items_purged.get(i);
                if (!purgesByFloor.containsKey(floor)) {
                    purgesByFloor.put(floor, new ArrayList());
                }
                ((ArrayList)purgesByFloor.get(floor)).add(key);
            }
        }
        this.pathElements.clear();
        this.rows = 0;
        int bossRelicChoiceIndex = 0;
        int tmpColumn = 0;
        for (int i = 0; i < labels.size(); ++i) {
            String label = (String)labels.get(i);
            int floor = i + 1;
            RunPathElement element = new RunPathElement(label, floor);
            if (this.runData.current_hp_per_floor != null && this.runData.max_hp_per_floor != null && i < this.runData.current_hp_per_floor.size() && i < this.runData.max_hp_per_floor.size()) {
                element.addHpData(this.runData.current_hp_per_floor.get(i), this.runData.max_hp_per_floor.get(i));
            }
            if (this.runData.gold_per_floor != null && i < this.runData.gold_per_floor.size()) {
                element.addGoldData(this.runData.gold_per_floor.get(i));
            }
            if (battlesByFloor.containsKey(floor)) {
                element.addBattleData((BattleStats)battlesByFloor.get(floor));
            }
            if (eventsByFloor.containsKey(floor)) {
                element.addEventData((EventStats)eventsByFloor.get(floor));
            }
            if (hashMap.containsKey(floor)) {
                element.addCardChoiceData((CardChoiceStats)hashMap.get(floor));
            }
            if (hashMap2.containsKey(floor)) {
                element.addRelicObtainStats((List)hashMap2.get(floor));
            }
            if (hashMap3.containsKey(floor)) {
                element.addPotionObtainStats((List)hashMap3.get(floor));
            }
            if (hashMap4.containsKey(floor)) {
                element.addCampfireChoiceData((CampfireChoice)hashMap4.get(floor));
            }
            if (hashMap5.containsKey(floor)) {
                element.addShopPurchaseData((ArrayList)hashMap5.get(floor));
            }
            if (purgesByFloor.containsKey(floor)) {
                element.addPurgeData((ArrayList)purgesByFloor.get(floor));
            }
            if (label.equals(BOSS_TREASURE_LABEL) && this.runData.boss_relics != null && bossRelicChoiceIndex < this.runData.boss_relics.size()) {
                element.addRelicObtainStats(this.runData.boss_relics.get((int)bossRelicChoiceIndex).picked);
                ++bossRelicChoiceIndex;
            }
            this.pathElements.add(element);
            element.col = tmpColumn++;
            element.row = this.rows;
            if (!this.isActEndNode(element) && i != labels.size() - 1) continue;
            tmpColumn = 0;
            ++this.rows;
        }
        this.rows = Math.max(this.rows, this.pathElements.size() / 20);
    }

    public void update() {
        boolean isHovered = false;
        for (RunPathElement element : this.pathElements) {
            element.update();
            if (!element.isHovered()) continue;
            isHovered = true;
        }
        if (isHovered) {
            CardCrawlGame.cursor.changeType(GameCursor.CursorType.INSPECT);
        }
    }

    private boolean isActEndNode(RunPathElement element) {
        return element.nodeType == RunPathElement.PathNodeType.BOSS_TREASURE || element.nodeType == RunPathElement.PathNodeType.HEART;
    }

    public void render(SpriteBatch sb, float x, float y) {
        float offsetX = x;
        float offsetY = y;
        int sinceOffset = 0;
        for (RunPathElement element : this.pathElements) {
            element.position(offsetX, offsetY);
            if (this.isActEndNode(element) || sinceOffset > 20) {
                offsetX = x;
                offsetY -= RunPathElement.getApproximateHeight();
                sinceOffset = 0;
                continue;
            }
            offsetX += RunPathElement.getApproximateWidth();
            ++sinceOffset;
        }
        for (RunPathElement element : this.pathElements) {
            element.render(sb);
        }
    }

    public float approximateHeight() {
        return (float)this.rows * RunPathElement.getApproximateHeight();
    }

    public float approximateMaxWidth() {
        return 16.5f * RunPathElement.getApproximateWidth();
    }
}

