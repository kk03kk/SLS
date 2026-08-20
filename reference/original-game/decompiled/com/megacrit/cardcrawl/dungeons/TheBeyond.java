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
import com.megacrit.cardcrawl.monsters.MonsterInfo;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.scenes.TheBeyondScene;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheBeyond
extends AbstractDungeon {
    private static final Logger logger = LogManager.getLogger(TheBeyond.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TheBeyond");
    public static final String[] TEXT = TheBeyond.uiStrings.TEXT;
    public static final String NAME = TEXT[0];
    public static final String ID = "TheBeyond";

    public TheBeyond(AbstractPlayer p, ArrayList<String> theList) {
        super(NAME, ID, p, theList);
        if (scene != null) {
            scene.dispose();
        }
        scene = new TheBeyondScene();
        fadeColor = Color.valueOf("140a1eff");
        sourceFadeColor = Color.valueOf("140a1eff");
        this.initializeLevelSpecificChances();
        mapRng = new com.megacrit.cardcrawl.random.Random(Settings.seed + (long)(AbstractDungeon.actNum * 200));
        TheBeyond.generateMap();
        CardCrawlGame.music.changeBGM(id);
    }

    public TheBeyond(AbstractPlayer p, SaveFile saveFile) {
        super(NAME, p, saveFile);
        CardCrawlGame.dungeon = this;
        if (scene != null) {
            scene.dispose();
        }
        scene = new TheBeyondScene();
        fadeColor = Color.valueOf("140a1eff");
        sourceFadeColor = Color.valueOf("140a1eff");
        this.initializeLevelSpecificChances();
        miscRng = new com.megacrit.cardcrawl.random.Random(Settings.seed + (long)saveFile.floor_num);
        CardCrawlGame.music.changeBGM(id);
        mapRng = new com.megacrit.cardcrawl.random.Random(Settings.seed + (long)(saveFile.act_num * 200));
        TheBeyond.generateMap();
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
        cardUpgradedChance = AbstractDungeon.ascensionLevel >= 12 ? 0.25f : 0.5f;
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
        monsters.add(new MonsterInfo("3 Darklings", 2.0f));
        monsters.add(new MonsterInfo("Orb Walker", 2.0f));
        monsters.add(new MonsterInfo("3 Shapes", 2.0f));
        MonsterInfo.normalizeWeights(monsters);
        this.populateMonsterList(monsters, count, false);
    }

    @Override
    protected void generateStrongEnemies(int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<MonsterInfo>();
        monsters.add(new MonsterInfo("Spire Growth", 1.0f));
        monsters.add(new MonsterInfo("Transient", 1.0f));
        monsters.add(new MonsterInfo("4 Shapes", 1.0f));
        monsters.add(new MonsterInfo("Maw", 1.0f));
        monsters.add(new MonsterInfo("Sphere and 2 Shapes", 1.0f));
        monsters.add(new MonsterInfo("Jaw Worm Horde", 1.0f));
        monsters.add(new MonsterInfo("3 Darklings", 1.0f));
        monsters.add(new MonsterInfo("Writhing Mass", 1.0f));
        MonsterInfo.normalizeWeights(monsters);
        this.populateFirstStrongEnemy(monsters, this.generateExclusions());
        this.populateMonsterList(monsters, count, false);
    }

    @Override
    protected void generateElites(int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<MonsterInfo>();
        monsters.add(new MonsterInfo("Giant Head", 2.0f));
        monsters.add(new MonsterInfo("Nemesis", 2.0f));
        monsters.add(new MonsterInfo("Reptomancer", 2.0f));
        MonsterInfo.normalizeWeights(monsters);
        this.populateMonsterList(monsters, count, true);
    }

    @Override
    protected ArrayList<String> generateExclusions() {
        ArrayList<String> retVal = new ArrayList<String>();
        switch ((String)monsterList.get(monsterList.size() - 1)) {
            case "3 Darklings": {
                retVal.add("3 Darklings");
                break;
            }
            case "Orb Walker": {
                retVal.add("Orb Walker");
                break;
            }
            case "3 Shapes": {
                retVal.add("4 Shapes");
                break;
            }
        }
        return retVal;
    }

    @Override
    protected void initializeBoss() {
        bossList.clear();
        if (Settings.isDailyRun) {
            bossList.add("Awakened One");
            bossList.add("Time Eater");
            bossList.add("Donu and Deca");
            Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
        } else if (!UnlockTracker.isBossSeen("CROW")) {
            bossList.add("Awakened One");
        } else if (!UnlockTracker.isBossSeen("DONUT")) {
            bossList.add("Donu and Deca");
        } else if (!UnlockTracker.isBossSeen("WIZARD")) {
            bossList.add("Time Eater");
        } else {
            bossList.add("Awakened One");
            bossList.add("Time Eater");
            bossList.add("Donu and Deca");
            Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
        }
        if (bossList.size() == 1) {
            bossList.add(bossList.get(0));
        } else if (bossList.isEmpty()) {
            logger.warn("Boss list was empty. How?");
            bossList.add("Awakened One");
            bossList.add("Time Eater");
            bossList.add("Donu and Deca");
            Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
        }
    }

    @Override
    protected void initializeEventList() {
        eventList.add("Falling");
        eventList.add("MindBloom");
        eventList.add("The Moai Head");
        eventList.add("Mysterious Sphere");
        eventList.add("SensoryStone");
        eventList.add("Tomb of Lord Red Mask");
        eventList.add("Winding Halls");
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

