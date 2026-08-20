/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.dungeons;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.monsters.MonsterInfo;
import com.megacrit.cardcrawl.rooms.EmptyRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.scenes.TheCityScene;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheCity
extends AbstractDungeon {
    private static final Logger logger = LogManager.getLogger(TheCity.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TheCity");
    public static final String[] TEXT = TheCity.uiStrings.TEXT;
    public static final String NAME = TEXT[0];
    public static final String ID = "TheCity";

    public TheCity(AbstractPlayer p, ArrayList<String> theList) {
        super(NAME, ID, p, theList);
        if (scene != null) {
            scene.dispose();
        }
        scene = new TheCityScene();
        fadeColor = Color.valueOf("0a1e1eff");
        sourceFadeColor = Color.valueOf("0a1e1eff");
        this.initializeLevelSpecificChances();
        mapRng = new com.megacrit.cardcrawl.random.Random(Settings.seed + (long)(AbstractDungeon.actNum * 100));
        TheCity.generateMap();
        CardCrawlGame.music.changeBGM(id);
        AbstractDungeon.currMapNode = new MapRoomNode(0, -1);
        AbstractDungeon.currMapNode.room = new EmptyRoom();
    }

    public TheCity(AbstractPlayer p, SaveFile saveFile) {
        super(NAME, p, saveFile);
        if (scene != null) {
            scene.dispose();
        }
        scene = new TheCityScene();
        fadeColor = Color.valueOf("0a1e1eff");
        sourceFadeColor = Color.valueOf("0a1e1eff");
        this.initializeLevelSpecificChances();
        miscRng = new com.megacrit.cardcrawl.random.Random(Settings.seed + (long)saveFile.floor_num);
        CardCrawlGame.music.changeBGM(id);
        mapRng = new com.megacrit.cardcrawl.random.Random(Settings.seed + (long)(saveFile.act_num * 100));
        TheCity.generateMap();
        firstRoomChosen = true;
        this.populatePathTaken(saveFile);
    }

    @Override
    protected void initializeLevelSpecificChances() {
        shopRoomChance = 0.05f;
        restRoomChance = 0.12f;
        treasureRoomChance = 0.0f;
        eventRoomChance = 0.22f;
        eliteRoomChance = 0.08f;
        smallChestChance = 50;
        mediumChestChance = 33;
        largeChestChance = 17;
        commonRelicChance = 50;
        uncommonRelicChance = 33;
        rareRelicChance = 17;
        colorlessRareChance = 0.3f;
        cardUpgradedChance = AbstractDungeon.ascensionLevel >= 12 ? 0.125f : 0.25f;
    }

    @Override
    protected void generateMonsters() {
        this.generateWeakEnemies(2);
        this.generateStrongEnemies(12);
        this.generateElites(10);
    }

    @Override
    protected void generateWeakEnemies(int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<MonsterInfo>();
        monsters.add(new MonsterInfo("Spheric Guardian", 2.0f));
        monsters.add(new MonsterInfo("Chosen", 2.0f));
        monsters.add(new MonsterInfo("Shell Parasite", 2.0f));
        monsters.add(new MonsterInfo("3 Byrds", 2.0f));
        monsters.add(new MonsterInfo("2 Thieves", 2.0f));
        MonsterInfo.normalizeWeights(monsters);
        this.populateMonsterList(monsters, count, false);
    }

    @Override
    protected void generateStrongEnemies(int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<MonsterInfo>();
        monsters.add(new MonsterInfo("Chosen and Byrds", 2.0f));
        monsters.add(new MonsterInfo("Sentry and Sphere", 2.0f));
        monsters.add(new MonsterInfo("Snake Plant", 6.0f));
        monsters.add(new MonsterInfo("Snecko", 4.0f));
        monsters.add(new MonsterInfo("Centurion and Healer", 6.0f));
        monsters.add(new MonsterInfo("Cultist and Chosen", 3.0f));
        monsters.add(new MonsterInfo("3 Cultists", 3.0f));
        monsters.add(new MonsterInfo("Shelled Parasite and Fungi", 3.0f));
        MonsterInfo.normalizeWeights(monsters);
        this.populateFirstStrongEnemy(monsters, this.generateExclusions());
        this.populateMonsterList(monsters, count, false);
    }

    @Override
    protected void generateElites(int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<MonsterInfo>();
        monsters.add(new MonsterInfo("Gremlin Leader", 1.0f));
        monsters.add(new MonsterInfo("Slavers", 1.0f));
        monsters.add(new MonsterInfo("Book of Stabbing", 1.0f));
        MonsterInfo.normalizeWeights(monsters);
        this.populateMonsterList(monsters, count, true);
    }

    @Override
    protected ArrayList<String> generateExclusions() {
        ArrayList<String> retVal = new ArrayList<String>();
        switch ((String)monsterList.get(monsterList.size() - 1)) {
            case "Spheric Guardian": {
                retVal.add("Sentry and Sphere");
                break;
            }
            case "3 Byrds": {
                retVal.add("Chosen and Byrds");
                break;
            }
            case "Chosen": {
                retVal.add("Chosen and Byrds");
                retVal.add("Cultist and Chosen");
                break;
            }
        }
        return retVal;
    }

    @Override
    protected void initializeBoss() {
        bossList.clear();
        if (Settings.isDailyRun) {
            bossList.add("Automaton");
            bossList.add("Collector");
            bossList.add("Champ");
            Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
        } else if (!UnlockTracker.isBossSeen("CHAMP")) {
            bossList.add("Champ");
        } else if (!UnlockTracker.isBossSeen("AUTOMATON")) {
            bossList.add("Automaton");
        } else if (!UnlockTracker.isBossSeen("COLLECTOR")) {
            bossList.add("Collector");
        } else {
            bossList.add("Automaton");
            bossList.add("Collector");
            bossList.add("Champ");
            Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
        }
        if (bossList.size() == 1) {
            bossList.add(bossList.get(0));
        } else if (bossList.isEmpty()) {
            logger.warn("Boss list was empty. How?");
            bossList.add("Automaton");
            bossList.add("Collector");
            bossList.add("Champ");
            Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
        }
    }

    @Override
    protected void initializeEventList() {
        eventList.add("Addict");
        eventList.add("Back to Basics");
        eventList.add("Beggar");
        eventList.add("Colosseum");
        eventList.add("Cursed Tome");
        eventList.add("Drug Dealer");
        eventList.add("Forgotten Altar");
        eventList.add("Ghosts");
        eventList.add("Masked Bandits");
        eventList.add("Nest");
        eventList.add("The Library");
        eventList.add("The Mausoleum");
        eventList.add("Vampires");
    }

    @Override
    protected void initializeEventImg() {
        if (eventBackgroundImg != null) {
            eventBackgroundImg.dispose();
            eventBackgroundImg = null;
        }
        eventBackgroundImg = ImageMaster.loadImage("images/ui/event/panel.png");
    }

    @Override
    protected void initializeShrineList() {
        shrineList.add("Match and Keep!");
        shrineList.add("Wheel of Change");
        shrineList.add("Golden Shrine");
        shrineList.add("Transmorgrifier");
        shrineList.add("Purifier");
        shrineList.add("Upgrade Shrine");
    }
}

