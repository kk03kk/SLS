/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.metrics;

import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import java.util.ArrayList;
import java.util.HashMap;

public class MetricData {
    public int campfire_rested = 0;
    public int campfire_upgraded = 0;
    public int purchased_purges = 0;
    public float win_rate = 0.5f;
    public ArrayList<Integer> potions_floor_spawned = new ArrayList();
    public ArrayList<Integer> potions_floor_usage = new ArrayList();
    public ArrayList<Integer> current_hp_per_floor = new ArrayList();
    public ArrayList<Integer> max_hp_per_floor = new ArrayList();
    public ArrayList<Integer> gold_per_floor = new ArrayList();
    public ArrayList<String> path_per_floor = new ArrayList();
    public ArrayList<String> path_taken = new ArrayList();
    public ArrayList<String> items_purchased = new ArrayList();
    public ArrayList<Integer> item_purchase_floors = new ArrayList();
    public ArrayList<String> items_purged = new ArrayList();
    public ArrayList<Integer> items_purged_floors = new ArrayList();
    public ArrayList<HashMap> card_choices = new ArrayList();
    public ArrayList<HashMap> event_choices = new ArrayList();
    public ArrayList<HashMap> boss_relics = new ArrayList();
    public ArrayList<HashMap> damage_taken = new ArrayList();
    public ArrayList<HashMap> potions_obtained = new ArrayList();
    public ArrayList<HashMap> relics_obtained = new ArrayList();
    public ArrayList<HashMap> campfire_choices = new ArrayList();
    public String neowBonus = "";
    public String neowCost = "";

    public void clearData() {
        this.campfire_rested = 0;
        this.campfire_upgraded = 0;
        this.purchased_purges = 0;
        this.potions_floor_spawned.clear();
        this.potions_floor_usage.clear();
        this.current_hp_per_floor.clear();
        this.max_hp_per_floor.clear();
        this.gold_per_floor.clear();
        this.path_per_floor.clear();
        this.path_taken.clear();
        this.items_purchased.clear();
        this.item_purchase_floors.clear();
        this.items_purged.clear();
        this.items_purged_floors.clear();
        this.card_choices.clear();
        this.event_choices.clear();
        this.damage_taken.clear();
        this.potions_obtained.clear();
        this.relics_obtained.clear();
        this.campfire_choices.clear();
        this.boss_relics.clear();
        this.neowBonus = "";
        this.neowCost = "";
    }

    public void addEncounterData() {
        HashMap<String, Object> combat = new HashMap<String, Object>();
        combat.put("floor", AbstractDungeon.floorNum);
        combat.put("enemies", AbstractDungeon.lastCombatMetricKey);
        combat.put("damage", GameActionManager.damageReceivedThisCombat);
        combat.put("turns", GameActionManager.turn);
        this.damage_taken.add(combat);
    }

    public void addPotionObtainData(AbstractPotion potion) {
        HashMap<String, Object> obtainInfo = new HashMap<String, Object>();
        obtainInfo.put("key", potion.ID);
        obtainInfo.put("floor", AbstractDungeon.floorNum);
        this.potions_obtained.add(obtainInfo);
    }

    public void addRelicObtainData(AbstractRelic relic) {
        HashMap<String, Object> obtainInfo = new HashMap<String, Object>();
        obtainInfo.put("key", relic.relicId);
        obtainInfo.put("floor", AbstractDungeon.floorNum);
        this.relics_obtained.add(obtainInfo);
    }

    public void addCampfireChoiceData(String choiceKey) {
        this.addCampfireChoiceData(choiceKey, null);
    }

    public void addCampfireChoiceData(String choiceKey, String data) {
        HashMap<String, Object> choice = new HashMap<String, Object>();
        choice.put("floor", AbstractDungeon.floorNum);
        choice.put("key", choiceKey);
        if (data != null) {
            choice.put("data", data);
        }
        this.campfire_choices.add(choice);
    }

    public void addShopPurchaseData(String key) {
        if (this.items_purchased.size() == this.item_purchase_floors.size()) {
            this.item_purchase_floors.add(AbstractDungeon.floorNum);
        }
        this.items_purchased.add(key);
    }

    public void addPurgedItem(String key) {
        if (this.items_purged.size() == this.items_purged_floors.size()) {
            this.items_purged_floors.add(AbstractDungeon.floorNum);
        }
        this.items_purged.add(key);
        ++this.purchased_purges;
    }

    public void addNeowData(String bonus, String cost) {
        this.neowBonus = bonus;
        this.neowCost = cost;
    }
}

