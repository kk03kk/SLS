/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.runHistory;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.EventHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.stats.BattleStats;
import com.megacrit.cardcrawl.screens.stats.CampfireChoice;
import com.megacrit.cardcrawl.screens.stats.CardChoiceStats;
import com.megacrit.cardcrawl.screens.stats.EventStats;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunPathElement {
    private static final boolean SHOW_ROOM_TYPE = true;
    private static final boolean SHOW_HP = true;
    private static final boolean SHOW_GOLD = true;
    private static final boolean SHOW_FIGHT_DETAILS = true;
    private static final boolean SHOW_EVENT_DETAILS = true;
    private static final boolean SHOW_EVENT_PLAYER_CHOICE = true;
    private static final boolean SHOW_CARD_PICK_DETAILS = true;
    private static final boolean SHOW_CARD_SKIP_INFO = true;
    private static final boolean SHOW_RELIC_OBTAIN_DETAILS = true;
    private static final boolean SHOW_POTION_OBTAIN_DETAILS = true;
    private static final boolean SHOW_CAMPFIRE_CHOICE_DETAILS = true;
    private static final boolean SHOW_PURCHASE_DETAILS = true;
    private static final float ICON_SIZE = 48.0f * Settings.scale;
    private static final float ICON_HOVER_SCALE = 2.0f;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("RunHistoryPathNodes");
    public static final String[] TEXT = RunPathElement.uiStrings.TEXT;
    private static final String NEW_LINE = " NL ";
    private static final String TAB = " TAB ";
    private static final String TEXT_ERROR = TEXT[0];
    private static final String TEXT_MONSTER = TEXT[1];
    private static final String TEXT_ELITE = TEXT[2];
    private static final String TEXT_EVENT = TEXT[3];
    private static final String TEXT_BOSS = TEXT[4];
    private static final String TEXT_TREASURE = TEXT[5];
    private static final String TEXT_BOSS_TREASURE = TEXT[6];
    private static final String TEXT_CAMPFIRE = TEXT[7];
    private static final String TEXT_SHOP = TEXT[8];
    private static final String TEXT_UNKNOWN_MONSTER = TEXT[9];
    private static final String TEXT_UNKN0WN_SHOP = TEXT[10];
    private static final String TEXT_UNKNOWN_TREASURE = TEXT[11];
    private static final String TEXT_FLOOR_FORMAT = TEXT[12];
    private static final String TEXT_SIMPLE_FLOOR_FORMAT = TEXT[13];
    private static final String TEXT_DAMAGE_FORMAT = TEXT[14];
    private static final String TEXT_TURNS_FORMAT = TEXT[15];
    private static final String TEXT_COMBAT_HP_FORMAT = TEXT[16];
    private static final String TEXT_GOLD_FORMAT = TEXT[17];
    private static final String TEXT_OBTAIN_HEADER = TEXT[18];
    private static final String TEXT_SKIP_HEADER = TEXT[19];
    private static final String TEXT_MISSING_INFO = TEXT[20];
    private static final String TEXT_SINGING_BOWL_CHOICE = TEXT[21];
    private static final String TEXT_OBTAIN_TYPE_CARD = TEXT[22];
    private static final String TEXT_OBTAIN_TYPE_RELIC = TEXT[23];
    private static final String TEXT_OBTAIN_TYPE_POTION = TEXT[24];
    private static final String TEXT_OBTAIN_TYPE_SPECIAL = TEXT[25];
    private static final String TEXT_REST_OPTION = TEXT[26];
    private static final String TEXT_SMITH_OPTION = TEXT[27];
    private static final String TEXT_TOKE_OPTION = TEXT[28];
    private static final String TEXT_DIG_OPTION = TEXT[29];
    private static final String TEXT_LIFT_OPTION = TEXT[30];
    private static final String TEXT_RECALL_OPTION = TEXT[46];
    private static final String TEXT_PURCHASED = TEXT[31];
    private static final String TEXT_SPENT = TEXT[32];
    private static final String TEXT_TOOK = TEXT[33];
    private static final String TEXT_LOST = TEXT[34];
    private static final String TEXT_GENERIC_MAX_HP_FORMAT = TEXT[35];
    private static final String TEXT_HEALED = TEXT[36];
    private static final String TEXT_GAINED = TEXT[37];
    private static final String TEXT_IGNORED = TEXT[38];
    private static final String TEXT_GENERIC_HP_FORMAT = TEXT[39];
    private static final String TEXT_EVENT_DAMAGE = TEXT[42];
    private static final String TEXT_UPGRADED = TEXT[43];
    private static final String TEXT_TRANSFORMED = TEXT[44];
    private static final String TEXT_LOST_RELIC = TEXT[45];
    private static final String TEXT_REMOVE_OPTION = TEXT_TOKE_OPTION;
    public Hitbox hb = new Hitbox(ICON_SIZE, ICON_SIZE);
    public PathNodeType nodeType;
    private int floor;
    public int col;
    public int row;
    private Integer currentHP;
    private Integer maxHP;
    private Integer gold;
    private BattleStats battleStats;
    private EventStats eventStats;
    private CardChoiceStats cardChoiceStats;
    private List<String> relicsObtained;
    private List<String> potionsObtained;
    private CampfireChoice campfireChoice;
    private List<String> shopPurchases;
    private List<String> shopPurges;
    private String cachedTooltip = null;

    public RunPathElement(String roomKey, int floorNum) {
        this.nodeType = this.pathNodeTypeForRoomKey(roomKey);
        this.floor = floorNum;
    }

    public void addHpData(Integer current, Integer max) {
        this.currentHP = current;
        this.maxHP = max;
    }

    public void addGoldData(Integer gold) {
        this.gold = gold;
    }

    public void addBattleData(BattleStats battle) {
        this.battleStats = battle;
    }

    public void addEventData(EventStats event) {
        this.eventStats = event;
    }

    public void addCardChoiceData(CardChoiceStats choice) {
        this.cardChoiceStats = choice;
    }

    public void addRelicObtainStats(List<String> relicKeys) {
        this.relicsObtained = relicKeys;
    }

    public void addRelicObtainStats(String relicKey) {
        this.addRelicObtainStats(Arrays.asList(relicKey));
    }

    public void addPotionObtainStats(List<String> potionKey) {
        this.potionsObtained = potionKey;
    }

    public void addCampfireChoiceData(CampfireChoice choice) {
        this.campfireChoice = choice;
    }

    public void addShopPurchaseData(ArrayList<String> keys) {
        this.shopPurchases = keys;
    }

    public void addPurgeData(ArrayList<String> keys) {
        this.shopPurges = keys;
    }

    public void update() {
        this.hb.update();
        if (this.hb.hovered) {
            float tipX = this.hb.x + 64.0f * Settings.scale;
            float tipY = this.hb.y + ICON_SIZE / 2.0f;
            String header = String.format(TEXT_SIMPLE_FLOOR_FORMAT, this.floor);
            TipHelper.renderGenericTip(tipX, tipY, header, this.getTipDescriptionText());
        }
    }

    private String getTipHeaderWithRoomTypeText() {
        return String.format(TEXT_FLOOR_FORMAT, Integer.toString(this.floor), this.stringForType());
    }

    private String getTipDescriptionText() {
        int i;
        boolean showCards;
        boolean displayGold;
        boolean displayHP;
        if (this.cachedTooltip != null) {
            return this.cachedTooltip;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.stringForType());
        boolean bl = displayHP = this.currentHP != null && this.maxHP != null;
        if (displayHP) {
            if (sb.length() > 0) {
                sb.append(NEW_LINE);
            }
            sb.append(String.format(TEXT_COMBAT_HP_FORMAT, this.currentHP, this.maxHP));
            sb.append(TAB);
        }
        boolean bl2 = displayGold = this.gold != null;
        if (displayGold) {
            sb.append(String.format(TEXT_GOLD_FORMAT, this.gold));
        }
        if (this.eventStats != null) {
            String cardName;
            String cardID;
            int i2;
            if (sb.length() > 0) {
                sb.append(NEW_LINE);
            }
            sb.append(this.localizedEventNameForKey(this.eventStats.event_name));
            if (!this.eventStats.player_choice.equals("Took Portal") && this.eventStats.max_hp_gain == 0 && this.eventStats.max_hp_loss == 0 && this.eventStats.gold_loss == 0 && this.eventStats.gold_gain == 0 && this.eventStats.damage_taken == 0 && this.eventStats.damage_healed == 0 && this.eventStats.cards_removed == null && this.eventStats.cards_obtained == null && this.eventStats.cards_transformed == null && this.eventStats.cards_upgraded == null && this.eventStats.relics_obtained == null && this.eventStats.relics_lost == null && this.eventStats.potions_obtained == null && this.battleStats == null) {
                sb.append(NEW_LINE);
                sb.append(TAB).append(TEXT_IGNORED);
            }
            if (this.eventStats.relics_lost != null && !this.eventStats.relics_lost.isEmpty()) {
                sb.append(NEW_LINE);
                for (i2 = 0; i2 < this.eventStats.relics_lost.size(); ++i2) {
                    String relicID = this.eventStats.relics_lost.get(i2);
                    String relicName = RelicLibrary.getRelic((String)relicID).name;
                    sb.append(TAB).append(String.format(TEXT_LOST_RELIC, relicName));
                    if (i2 >= this.eventStats.relics_lost.size() - 1) continue;
                    sb.append(NEW_LINE);
                }
            }
            if (this.eventStats.max_hp_loss != 0) {
                sb.append(NEW_LINE);
                sb.append(TAB).append(TEXT_LOST);
                sb.append(String.format(TEXT_GENERIC_MAX_HP_FORMAT, this.eventStats.max_hp_loss));
            }
            if (this.eventStats.damage_taken != 0) {
                sb.append(NEW_LINE);
                sb.append(TAB).append(TEXT_TOOK);
                sb.append(String.format(TEXT_EVENT_DAMAGE, this.eventStats.damage_taken));
            }
            if (this.eventStats.gold_loss != 0) {
                sb.append(NEW_LINE);
                sb.append(TAB).append(TEXT_SPENT);
                sb.append(String.format(TEXT_GOLD_FORMAT, this.eventStats.gold_loss));
            }
            if (this.eventStats.max_hp_gain != 0) {
                sb.append(NEW_LINE);
                sb.append(TAB).append(TEXT_GAINED);
                sb.append(String.format(TEXT_GENERIC_MAX_HP_FORMAT, this.eventStats.max_hp_gain));
            }
            if (this.eventStats.damage_healed != 0) {
                sb.append(NEW_LINE);
                sb.append(TAB).append(TEXT_HEALED);
                sb.append(String.format(TEXT_GENERIC_HP_FORMAT, this.eventStats.damage_healed));
            }
            if (this.eventStats.gold_gain != 0) {
                sb.append(NEW_LINE);
                sb.append(TAB).append(TEXT_GAINED);
                sb.append(String.format(TEXT_GOLD_FORMAT, this.eventStats.gold_gain));
            }
            if (this.eventStats.cards_removed != null && !this.eventStats.cards_removed.isEmpty()) {
                sb.append(NEW_LINE);
                for (i2 = 0; i2 < this.eventStats.cards_removed.size(); ++i2) {
                    cardID = this.eventStats.cards_removed.get(i2);
                    cardName = CardLibrary.getCardNameFromMetricID(cardID);
                    sb.append(TAB).append(String.format(TEXT_REMOVE_OPTION, cardName));
                    if (i2 >= this.eventStats.cards_removed.size() - 1) continue;
                    sb.append(NEW_LINE);
                }
            }
            if (this.eventStats.cards_upgraded != null && !this.eventStats.cards_upgraded.isEmpty()) {
                sb.append(NEW_LINE);
                for (i2 = 0; i2 < this.eventStats.cards_upgraded.size(); ++i2) {
                    cardID = this.eventStats.cards_upgraded.get(i2);
                    cardName = CardLibrary.getCardNameFromMetricID(cardID);
                    sb.append(TAB).append(String.format(TEXT_UPGRADED, cardName));
                    if (i2 >= this.eventStats.cards_upgraded.size() - 1) continue;
                    sb.append(NEW_LINE);
                }
            }
            if (this.eventStats.cards_transformed != null && !this.eventStats.cards_transformed.isEmpty()) {
                sb.append(NEW_LINE);
                for (i2 = 0; i2 < this.eventStats.cards_transformed.size(); ++i2) {
                    cardID = this.eventStats.cards_transformed.get(i2);
                    cardName = CardLibrary.getCardNameFromMetricID(cardID);
                    sb.append(TAB).append(String.format(TEXT_TRANSFORMED, cardName));
                    if (i2 >= this.eventStats.cards_transformed.size() - 1) continue;
                    sb.append(NEW_LINE);
                }
            }
            if ((this.eventStats.cards_obtained != null && !this.eventStats.cards_obtained.isEmpty() || this.eventStats.relics_obtained != null && !this.eventStats.relics_obtained.isEmpty() || this.eventStats.potions_obtained != null && !this.eventStats.potions_obtained.isEmpty()) && this.relicsObtained == null && this.battleStats == null && this.cardChoiceStats == null && this.potionsObtained == null) {
                String name;
                sb.append(NEW_LINE).append(TEXT_OBTAIN_HEADER);
                if (this.eventStats.relics_obtained != null && !this.eventStats.relics_obtained.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(NEW_LINE);
                    }
                    for (i2 = 0; i2 < this.eventStats.relics_obtained.size(); ++i2) {
                        name = RelicLibrary.getRelic((String)this.eventStats.relics_obtained.get((int)i2)).name;
                        if (i2 > 0) {
                            sb.append(NEW_LINE);
                        }
                        sb.append(TAB).append(TEXT_OBTAIN_TYPE_RELIC).append(name);
                    }
                }
                if (!(this.eventStats.cards_obtained == null || this.eventStats.cards_obtained.isEmpty() || this.eventStats.cards_obtained.isEmpty() || this.eventStats.cards_obtained.equals("SKIP"))) {
                    if (sb.length() > 0) {
                        sb.append(NEW_LINE);
                    }
                    for (i2 = 0; i2 < this.eventStats.cards_obtained.size(); ++i2) {
                        name = CardLibrary.getCardNameFromMetricID(this.eventStats.cards_obtained.get(i2));
                        if (i2 > 0) {
                            sb.append(NEW_LINE);
                        }
                        sb.append(TAB).append(TEXT_OBTAIN_TYPE_CARD).append(name);
                    }
                }
                if (this.eventStats.potions_obtained != null && !this.eventStats.potions_obtained.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(NEW_LINE);
                    }
                    for (String key : this.eventStats.potions_obtained) {
                        sb.append(TAB).append(TEXT_OBTAIN_TYPE_POTION).append(PotionHelper.getPotion((String)key).name);
                    }
                }
            }
        }
        if (this.battleStats != null) {
            if (sb.length() > 0) {
                sb.append(NEW_LINE);
            }
            sb.append(this.localizedEnemyNameForKey(this.battleStats.enemies));
            sb.append(NEW_LINE);
            sb.append(TAB).append(String.format(TEXT_DAMAGE_FORMAT, this.battleStats.damage));
            sb.append(NEW_LINE);
            sb.append(TAB).append(String.format(TEXT_TURNS_FORMAT, this.battleStats.turns));
        }
        if (this.campfireChoice != null) {
            if (sb.length() > 0) {
                sb.append(NEW_LINE);
            }
            switch (this.campfireChoice.key) {
                case "REST": {
                    sb.append(TEXT_REST_OPTION);
                    break;
                }
                case "SMITH": {
                    sb.append(String.format(TEXT_SMITH_OPTION, CardLibrary.getCardNameFromMetricID(this.campfireChoice.data)));
                    break;
                }
                case "PURGE": {
                    sb.append(String.format(TEXT_TOKE_OPTION, CardLibrary.getCardNameFromMetricID(this.campfireChoice.data)));
                    break;
                }
                case "DIG": {
                    sb.append(TEXT_DIG_OPTION);
                    break;
                }
                case "LIFT": {
                    sb.append(String.format(TEXT_LIFT_OPTION, this.campfireChoice.data, 3));
                    break;
                }
                case "RECALL": {
                    sb.append(TEXT_RECALL_OPTION);
                    break;
                }
                default: {
                    sb.append(TEXT_MISSING_INFO);
                }
            }
        }
        boolean showRelic = this.relicsObtained != null;
        boolean showPotion = this.potionsObtained != null;
        boolean bl3 = showCards = this.cardChoiceStats != null && !this.cardChoiceStats.picked.equals("SKIP");
        if (showRelic || showPotion || showCards) {
            sb.append(NEW_LINE).append(TEXT_OBTAIN_HEADER);
        }
        if (showRelic) {
            if (sb.length() > 0) {
                sb.append(NEW_LINE);
            }
            for (i = 0; i < this.relicsObtained.size(); ++i) {
                String name = RelicLibrary.getRelic((String)this.relicsObtained.get((int)i)).name;
                if (i > 0) {
                    sb.append(NEW_LINE);
                }
                sb.append(TAB).append(TEXT_OBTAIN_TYPE_RELIC).append(name);
            }
        }
        if (showCards && !this.cardChoiceStats.picked.isEmpty() && !this.cardChoiceStats.picked.equals("SKIP")) {
            String text = this.cardChoiceStats.picked.equals("Singing Bowl") ? TEXT_OBTAIN_TYPE_SPECIAL + TEXT_SINGING_BOWL_CHOICE : TEXT_OBTAIN_TYPE_CARD + CardLibrary.getCardNameFromMetricID(this.cardChoiceStats.picked);
            if (sb.length() > 0) {
                sb.append(NEW_LINE);
            }
            sb.append(TAB).append(text);
        }
        if (showPotion) {
            if (sb.length() > 0) {
                sb.append(NEW_LINE);
            }
            for (String key : this.potionsObtained) {
                sb.append(TAB).append(TEXT_OBTAIN_TYPE_POTION).append(PotionHelper.getPotion((String)key).name);
            }
        }
        if (this.cardChoiceStats != null) {
            sb.append(NEW_LINE);
            sb.append(TEXT_SKIP_HEADER);
            sb.append(NEW_LINE);
            for (i = 0; i < this.cardChoiceStats.not_picked.size(); ++i) {
                String cardID = this.cardChoiceStats.not_picked.get(i);
                String cardName = CardLibrary.getCardNameFromMetricID(cardID);
                sb.append(TAB).append(TEXT_OBTAIN_TYPE_CARD).append(cardName);
                if (i >= this.cardChoiceStats.not_picked.size() - 1) continue;
                sb.append(NEW_LINE);
            }
        }
        if (this.shopPurchases != null) {
            sb.append(NEW_LINE).append(TEXT_PURCHASED);
            for (String key : this.shopPurchases) {
                String text = null;
                if (CardLibrary.isACard(key)) {
                    text = TEXT_OBTAIN_TYPE_CARD + CardLibrary.getCardNameFromMetricID(key);
                } else if (RelicLibrary.isARelic(key)) {
                    text = TEXT_OBTAIN_TYPE_RELIC + RelicLibrary.getRelic((String)key).name;
                } else if (PotionHelper.isAPotion(key)) {
                    text = TEXT_OBTAIN_TYPE_POTION + PotionHelper.getPotion((String)key).name;
                }
                if (text == null) continue;
                sb.append(NEW_LINE).append(TAB).append(text);
            }
        }
        if (this.shopPurges != null) {
            for (String key : this.shopPurges) {
                sb.append(NEW_LINE).append(String.format(TEXT_REMOVE_OPTION, CardLibrary.getCardNameFromMetricID(key)));
            }
        }
        if (sb.length() > 0) {
            this.cachedTooltip = sb.toString();
            return this.cachedTooltip;
        }
        return TEXT_MISSING_INFO;
    }

    public boolean isHovered() {
        return this.hb.hovered;
    }

    public void position(float x, float y) {
        this.hb.move(x, y - ICON_SIZE);
    }

    public static float getApproximateWidth() {
        return ICON_SIZE;
    }

    public static float getApproximateHeight() {
        return ICON_SIZE;
    }

    public void render(SpriteBatch sb) {
        Texture image = this.imageForType(this.nodeType);
        if (this.isHovered()) {
            float hoverSize = ICON_SIZE * 2.0f;
            float offset = (hoverSize - ICON_SIZE) / 2.0f;
            sb.draw(image, this.hb.x - offset, this.hb.y - offset, hoverSize, hoverSize);
        } else {
            sb.draw(image, this.hb.x, this.hb.y, ICON_SIZE, ICON_SIZE);
        }
        this.hb.render(sb);
    }

    public String localizedEnemyNameForKey(String enemyId) {
        return MonsterHelper.getEncounterName(enemyId);
    }

    public String localizedEventNameForKey(String eventId) {
        return EventHelper.getEventName(eventId);
    }

    public PathNodeType pathNodeTypeForRoomKey(String roomKey) {
        switch (roomKey) {
            case "M": {
                return PathNodeType.MONSTER;
            }
            case "E": {
                return PathNodeType.ELITE;
            }
            case "?": {
                return PathNodeType.EVENT;
            }
            case "B": 
            case "BOSS": {
                return PathNodeType.BOSS;
            }
            case "T": {
                return PathNodeType.TREASURE;
            }
            case "T!": {
                return PathNodeType.BOSS_TREASURE;
            }
            case "R": {
                return PathNodeType.CAMPFIRE;
            }
            case "$": {
                return PathNodeType.SHOP;
            }
            case "?(M)": {
                return PathNodeType.UNKNOWN_MONSTER;
            }
            case "?($)": {
                return PathNodeType.UNKNOWN_SHOP;
            }
            case "?(T)": {
                return PathNodeType.UNKNOWN_TREASURE;
            }
            case "?(_)": {
                return PathNodeType.EVENT;
            }
            case "<3": {
                return PathNodeType.HEART;
            }
        }
        return PathNodeType.ERROR;
    }

    public Texture imageForType(PathNodeType nodeType) {
        switch (nodeType) {
            case MONSTER: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_MONSTER;
            }
            case ELITE: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_ELITE;
            }
            case EVENT: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_EVENT;
            }
            case BOSS: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_BOSS;
            }
            case TREASURE: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_CHEST;
            }
            case BOSS_TREASURE: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_BOSS_CHEST;
            }
            case CAMPFIRE: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_REST;
            }
            case SHOP: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_SHOP;
            }
            case UNKNOWN_MONSTER: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_UNKNOWN_MONSTER;
            }
            case UNKNOWN_SHOP: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_UNKNOWN_SHOP;
            }
            case UNKNOWN_TREASURE: {
                return ImageMaster.RUN_HISTORY_MAP_ICON_UNKNOWN_CHEST;
            }
        }
        return ImageMaster.RUN_HISTORY_MAP_ICON_EVENT;
    }

    private String stringForType() {
        return this.stringForType(this.nodeType);
    }

    private String stringForType(PathNodeType type) {
        switch (type) {
            case MONSTER: {
                return TEXT_MONSTER;
            }
            case ELITE: {
                return TEXT_ELITE;
            }
            case EVENT: {
                return TEXT_EVENT;
            }
            case BOSS: {
                return TEXT_BOSS;
            }
            case TREASURE: {
                return TEXT_TREASURE;
            }
            case BOSS_TREASURE: {
                return TEXT_BOSS_TREASURE;
            }
            case CAMPFIRE: {
                return TEXT_CAMPFIRE;
            }
            case SHOP: {
                return TEXT_SHOP;
            }
            case UNKNOWN_MONSTER: {
                return TEXT_UNKNOWN_MONSTER;
            }
            case UNKNOWN_SHOP: {
                return TEXT_UNKN0WN_SHOP;
            }
            case UNKNOWN_TREASURE: {
                return TEXT_UNKNOWN_TREASURE;
            }
        }
        return TEXT_ERROR;
    }

    public static enum PathNodeType {
        ERROR,
        MONSTER,
        ELITE,
        EVENT,
        BOSS,
        TREASURE,
        BOSS_TREASURE,
        CAMPFIRE,
        SHOP,
        UNKNOWN_MONSTER,
        UNKNOWN_SHOP,
        UNKNOWN_TREASURE,
        HEART;

    }
}

