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
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapGenerator;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.rooms.TrueVictoryRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.scenes.TheEndingScene;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheEnding
extends AbstractDungeon {
    private static final Logger logger = LogManager.getLogger(TheEnding.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TheEnding");
    public static final String[] TEXT = TheEnding.uiStrings.TEXT;
    public static final String NAME = TEXT[0];
    public static final String ID = "TheEnding";

    public TheEnding(AbstractPlayer p, ArrayList<String> theList) {
        super(NAME, ID, p, theList);
        if (scene != null) {
            scene.dispose();
        }
        scene = new TheEndingScene();
        fadeColor = Color.valueOf("140a1eff");
        sourceFadeColor = Color.valueOf("140a1eff");
        this.initializeLevelSpecificChances();
        mapRng = new Random(Settings.seed + (long)(AbstractDungeon.actNum * 300));
        this.generateSpecialMap();
        CardCrawlGame.music.changeBGM(id);
    }

    public TheEnding(AbstractPlayer p, SaveFile saveFile) {
        super(NAME, p, saveFile);
        CardCrawlGame.dungeon = this;
        if (scene != null) {
            scene.dispose();
        }
        scene = new TheEndingScene();
        fadeColor = Color.valueOf("140a1eff");
        sourceFadeColor = Color.valueOf("140a1eff");
        this.initializeLevelSpecificChances();
        miscRng = new Random(Settings.seed + (long)saveFile.floor_num);
        CardCrawlGame.music.changeBGM(id);
        mapRng = new Random(Settings.seed + (long)(saveFile.act_num * 300));
        this.generateSpecialMap();
        firstRoomChosen = true;
        this.populatePathTaken(saveFile);
    }

    private void generateSpecialMap() {
        long startTime = System.currentTimeMillis();
        map = new ArrayList();
        ArrayList<MapRoomNode> row1 = new ArrayList<MapRoomNode>();
        MapRoomNode restNode = new MapRoomNode(3, 0);
        restNode.room = new RestRoom();
        MapRoomNode shopNode = new MapRoomNode(3, 1);
        shopNode.room = new ShopRoom();
        MapRoomNode enemyNode = new MapRoomNode(3, 2);
        enemyNode.room = new MonsterRoomElite();
        MapRoomNode bossNode = new MapRoomNode(3, 3);
        bossNode.room = new MonsterRoomBoss();
        MapRoomNode victoryNode = new MapRoomNode(3, 4);
        victoryNode.room = new TrueVictoryRoom();
        this.connectNode(restNode, shopNode);
        this.connectNode(shopNode, enemyNode);
        enemyNode.addEdge(new MapEdge(enemyNode.x, enemyNode.y, enemyNode.offsetX, enemyNode.offsetY, bossNode.x, bossNode.y, bossNode.offsetX, bossNode.offsetY, false));
        row1.add(new MapRoomNode(0, 0));
        row1.add(new MapRoomNode(1, 0));
        row1.add(new MapRoomNode(2, 0));
        row1.add(restNode);
        row1.add(new MapRoomNode(4, 0));
        row1.add(new MapRoomNode(5, 0));
        row1.add(new MapRoomNode(6, 0));
        ArrayList<MapRoomNode> row2 = new ArrayList<MapRoomNode>();
        row2.add(new MapRoomNode(0, 1));
        row2.add(new MapRoomNode(1, 1));
        row2.add(new MapRoomNode(2, 1));
        row2.add(shopNode);
        row2.add(new MapRoomNode(4, 1));
        row2.add(new MapRoomNode(5, 1));
        row2.add(new MapRoomNode(6, 1));
        ArrayList<MapRoomNode> row3 = new ArrayList<MapRoomNode>();
        row3.add(new MapRoomNode(0, 2));
        row3.add(new MapRoomNode(1, 2));
        row3.add(new MapRoomNode(2, 2));
        row3.add(enemyNode);
        row3.add(new MapRoomNode(4, 2));
        row3.add(new MapRoomNode(5, 2));
        row3.add(new MapRoomNode(6, 2));
        ArrayList<MapRoomNode> row4 = new ArrayList<MapRoomNode>();
        row4.add(new MapRoomNode(0, 3));
        row4.add(new MapRoomNode(1, 3));
        row4.add(new MapRoomNode(2, 3));
        row4.add(bossNode);
        row4.add(new MapRoomNode(4, 3));
        row4.add(new MapRoomNode(5, 3));
        row4.add(new MapRoomNode(6, 3));
        ArrayList<MapRoomNode> row5 = new ArrayList<MapRoomNode>();
        row5.add(new MapRoomNode(0, 4));
        row5.add(new MapRoomNode(1, 4));
        row5.add(new MapRoomNode(2, 4));
        row5.add(victoryNode);
        row5.add(new MapRoomNode(4, 4));
        row5.add(new MapRoomNode(5, 4));
        row5.add(new MapRoomNode(6, 4));
        map.add(row1);
        map.add(row2);
        map.add(row3);
        map.add(row4);
        map.add(row5);
        logger.info("Generated the following dungeon map:");
        logger.info(MapGenerator.toString(map, true));
        logger.info("Game Seed: " + Settings.seed);
        logger.info("Map generation time: " + (System.currentTimeMillis() - startTime) + "ms");
        firstRoomChosen = false;
        TheEnding.fadeIn();
    }

    private void connectNode(MapRoomNode src, MapRoomNode dst) {
        src.addEdge(new MapEdge(src.x, src.y, src.offsetX, src.offsetY, dst.x, dst.y, dst.offsetX, dst.offsetY, false));
    }

    @Override
    protected void initializeLevelSpecificChances() {
        shopRoomChance = 0.05f;
        restRoomChance = 0.12f;
        treasureRoomChance = 0.0f;
        eventRoomChance = 0.22f;
        eliteRoomChance = 0.08f;
        smallChestChance = 0;
        mediumChestChance = 100;
        largeChestChance = 0;
        commonRelicChance = 0;
        uncommonRelicChance = 100;
        rareRelicChance = 0;
        colorlessRareChance = 0.3f;
        cardUpgradedChance = AbstractDungeon.ascensionLevel >= 12 ? 0.25f : 0.5f;
    }

    @Override
    protected void generateMonsters() {
        monsterList = new ArrayList();
        monsterList.add("Shield and Spear");
        monsterList.add("Shield and Spear");
        monsterList.add("Shield and Spear");
        eliteMonsterList = new ArrayList();
        eliteMonsterList.add("Shield and Spear");
        eliteMonsterList.add("Shield and Spear");
        eliteMonsterList.add("Shield and Spear");
    }

    @Override
    protected void generateWeakEnemies(int count) {
    }

    @Override
    protected void generateStrongEnemies(int count) {
    }

    @Override
    protected void generateElites(int count) {
    }

    @Override
    protected ArrayList<String> generateExclusions() {
        return new ArrayList<String>();
    }

    @Override
    protected void initializeBoss() {
        bossList.add("The Heart");
        bossList.add("The Heart");
        bossList.add("The Heart");
    }

    @Override
    protected void initializeEventList() {
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
    }
}

