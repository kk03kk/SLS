/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.metrics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.VictoryScreen;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Metrics
implements Runnable {
    private static final Logger logger = LogManager.getLogger(Metrics.class.getName());
    private HashMap<Object, Object> params = new HashMap();
    private Gson gson = new Gson();
    private long lastPlaytimeEnd;
    public static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
    public boolean death;
    public boolean trueVictory;
    public MonsterGroup monsters = null;
    public MetricRequestType type;

    public void setValues(boolean death, boolean trueVictor, MonsterGroup monsters, MetricRequestType type) {
        this.death = death;
        this.trueVictory = trueVictor;
        this.monsters = monsters;
        this.type = type;
    }

    private void sendPost(String fileName) {
    }

    private void addData(Object key, Object value) {
        this.params.put(key, value);
    }

    private void sendPost(String url, final String fileToDelete) {
        HashMap<String, Object> event = new HashMap<String, Object>();
        event.put("event", this.params);
        if (Settings.isBeta) {
            event.put("host", CardCrawlGame.playerName);
        } else {
            event.put("host", CardCrawlGame.alias);
        }
        event.put("time", System.currentTimeMillis() / 1000L);
        String data = this.gson.toJson(event);
        logger.info("UPLOADING METRICS TO: url=" + url + ",data=" + data);
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest().method("POST").url(url).header("Content-Type", "application/json").header("Accept", "application/json").header("User-Agent", "curl/7.43.0").build();
        httpRequest.setContent(data);
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener(){

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                logger.info("Metrics: http request response: " + httpResponse.getResultAsString());
                if (fileToDelete != null) {
                    Gdx.files.local(fileToDelete).delete();
                }
            }

            @Override
            public void failed(Throwable t) {
                logger.info("Metrics: http request failed: " + t.toString());
            }

            @Override
            public void cancelled() {
                logger.info("Metrics: http request cancelled.");
            }
        });
    }

    private void gatherAllData(boolean death, boolean trueVictor, MonsterGroup monsters) {
        this.addData("play_id", UUID.randomUUID().toString());
        this.addData("build_version", CardCrawlGame.TRUE_VERSION_NUM);
        this.addData("seed_played", Settings.seed.toString());
        this.addData("chose_seed", Settings.seedSet);
        this.addData("seed_source_timestamp", Settings.seedSourceTimestamp);
        this.addData("is_daily", Settings.isDailyRun);
        this.addData("special_seed", Settings.specialSeed);
        if (ModHelper.enabledMods.size() > 0) {
            this.addData("daily_mods", ModHelper.getEnabledModIDs());
        }
        this.addData("is_trial", Settings.isTrial);
        this.addData("is_endless", Settings.isEndless);
        if (death) {
            AbstractPlayer player = AbstractDungeon.player;
            CardCrawlGame.metricData.current_hp_per_floor.add(player.currentHealth);
            CardCrawlGame.metricData.max_hp_per_floor.add(player.maxHealth);
            CardCrawlGame.metricData.gold_per_floor.add(player.gold);
        }
        this.addData("is_ascension_mode", AbstractDungeon.isAscensionMode);
        this.addData("ascension_level", AbstractDungeon.ascensionLevel);
        this.addData("neow_bonus", CardCrawlGame.metricData.neowBonus);
        this.addData("neow_cost", CardCrawlGame.metricData.neowCost);
        this.addData("is_beta", Settings.isBeta);
        this.addData("is_prod", Settings.isDemo);
        this.addData("victory", !death);
        this.addData("floor_reached", AbstractDungeon.floorNum);
        if (trueVictor) {
            this.addData("score", VictoryScreen.calcScore(!death));
        } else {
            this.addData("score", DeathScreen.calcScore(!death));
        }
        this.lastPlaytimeEnd = System.currentTimeMillis() / 1000L;
        this.addData("timestamp", this.lastPlaytimeEnd);
        this.addData("local_time", timestampFormatter.format(Calendar.getInstance().getTime()));
        this.addData("playtime", (long)CardCrawlGame.playtime);
        this.addData("player_experience", Settings.totalPlayTime);
        this.addData("master_deck", AbstractDungeon.player.masterDeck.getCardIdsForMetrics());
        this.addData("relics", AbstractDungeon.player.getRelicNames());
        this.addData("gold", AbstractDungeon.player.gold);
        this.addData("campfire_rested", CardCrawlGame.metricData.campfire_rested);
        this.addData("campfire_upgraded", CardCrawlGame.metricData.campfire_upgraded);
        this.addData("purchased_purges", CardCrawlGame.metricData.purchased_purges);
        this.addData("potions_floor_spawned", CardCrawlGame.metricData.potions_floor_spawned);
        this.addData("potions_floor_usage", CardCrawlGame.metricData.potions_floor_usage);
        this.addData("current_hp_per_floor", CardCrawlGame.metricData.current_hp_per_floor);
        this.addData("max_hp_per_floor", CardCrawlGame.metricData.max_hp_per_floor);
        this.addData("gold_per_floor", CardCrawlGame.metricData.gold_per_floor);
        this.addData("path_per_floor", CardCrawlGame.metricData.path_per_floor);
        this.addData("path_taken", CardCrawlGame.metricData.path_taken);
        this.addData("items_purchased", CardCrawlGame.metricData.items_purchased);
        this.addData("item_purchase_floors", CardCrawlGame.metricData.item_purchase_floors);
        this.addData("items_purged", CardCrawlGame.metricData.items_purged);
        this.addData("items_purged_floors", CardCrawlGame.metricData.items_purged_floors);
        this.addData("character_chosen", AbstractDungeon.player.chosenClass.name());
        this.addData("card_choices", CardCrawlGame.metricData.card_choices);
        this.addData("event_choices", CardCrawlGame.metricData.event_choices);
        this.addData("boss_relics", CardCrawlGame.metricData.boss_relics);
        this.addData("damage_taken", CardCrawlGame.metricData.damage_taken);
        this.addData("potions_obtained", CardCrawlGame.metricData.potions_obtained);
        this.addData("relics_obtained", CardCrawlGame.metricData.relics_obtained);
        this.addData("campfire_choices", CardCrawlGame.metricData.campfire_choices);
        this.addData("circlet_count", AbstractDungeon.player.getCircletCount());
        Prefs pref = AbstractDungeon.player.getPrefs();
        int numVictory = pref.getInteger("WIN_COUNT", 0);
        int numDeath = pref.getInteger("LOSE_COUNT", 0);
        if (numVictory <= 0) {
            this.addData("win_rate", Float.valueOf(0.0f));
        } else {
            this.addData("win_rate", numVictory / (numDeath + numVictory));
        }
        if (death && monsters != null) {
            this.addData("killed_by", AbstractDungeon.lastCombatMetricKey);
        } else {
            this.addData("killed_by", null);
        }
    }

    private void gatherAllDataAndSend(boolean death, boolean trueVictor, MonsterGroup monsters) {
        if (DeathScreen.shouldUploadMetricData()) {
            this.gatherAllData(death, trueVictor, monsters);
            this.sendPost(null);
        }
    }

    public void gatherAllDataAndSave(boolean death, boolean trueVictor, MonsterGroup monsters) {
        FileHandle file;
        this.gatherAllData(death, trueVictor, monsters);
        String data = this.gson.toJson(this.params);
        if (!Settings.isDailyRun) {
            String local_runs_save_path = "runs" + File.separator;
            switch (CardCrawlGame.saveSlot) {
                case 0: {
                    break;
                }
                default: {
                    local_runs_save_path = local_runs_save_path + CardCrawlGame.saveSlot + "_";
                }
            }
            local_runs_save_path = local_runs_save_path + AbstractDungeon.player.chosenClass.name() + File.separator + this.lastPlaytimeEnd + ".run";
            file = Gdx.files.local(local_runs_save_path);
        } else {
            String tmpPath = "runs" + File.separator;
            switch (CardCrawlGame.saveSlot) {
                case 0: {
                    break;
                }
                default: {
                    tmpPath = tmpPath + CardCrawlGame.saveSlot + "_";
                }
            }
            file = Gdx.files.local(tmpPath + "DAILY" + File.separator + this.lastPlaytimeEnd + ".run");
        }
        file.writeString(data, false);
        this.removeExcessRunFiles();
    }

    private void removeExcessRunFiles() {
        if (!Settings.isConsoleBuild) {
            return;
        }
        FileHandle fh = Gdx.files.local("runs");
        FileHandle[] allFolders = fh.list();
        HashMap<String, FileHandle> map = new HashMap<String, FileHandle>();
        ArrayList<String> runNames = new ArrayList<String>();
        for (FileHandle i : allFolders) {
            FileHandle[] runs;
            for (FileHandle j : runs = i.list("run")) {
                runNames.add(j.name());
                map.put(j.name(), j);
            }
        }
        int excessFileThreshold = 500;
        int numFilesToDelete = runNames.size() - excessFileThreshold;
        if (runNames.size() < excessFileThreshold) {
            return;
        }
        Collections.sort(runNames);
        for (int i = 0; i < numFilesToDelete; ++i) {
            if (!map.containsKey(runNames.get(i))) continue;
            logger.info("DELETING EXCESS RUN: " + map.get(((String)runNames.get(i)).toString()));
            ((FileHandle)map.get(runNames.get(i))).delete();
        }
    }

    @Override
    public void run() {
        switch (this.type) {
            case UPLOAD_CRASH: {
                if (Settings.isModded) break;
                this.gatherAllDataAndSend(this.death, false, this.monsters);
                break;
            }
            case UPLOAD_METRICS: {
                if (Settings.isModded) break;
                this.gatherAllDataAndSend(this.death, this.trueVictory, this.monsters);
                break;
            }
            default: {
                logger.info("Unspecified MetricRequestType: " + this.type.name() + " in run()");
            }
        }
    }

    public static enum MetricRequestType {
        UPLOAD_METRICS,
        UPLOAD_CRASH,
        NONE;

    }
}

