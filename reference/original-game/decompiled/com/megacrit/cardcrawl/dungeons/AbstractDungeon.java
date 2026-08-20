/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.dungeons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.SoulGroup;
import com.megacrit.cardcrawl.cards.colorless.SwiftStrike;
import com.megacrit.cardcrawl.cards.curses.AscendersBane;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.ExceptionHandler;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.credits.CreditsScreen;
import com.megacrit.cardcrawl.dungeons.Exordium;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.shrines.NoteForYourself;
import com.megacrit.cardcrawl.helpers.BlightHelper;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.EventHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.map.DungeonMap;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapGenerator;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.map.RoomTypeAssigner;
import com.megacrit.cardcrawl.metrics.Metrics;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.MonsterInfo;
import com.megacrit.cardcrawl.neow.NeowRoom;
import com.megacrit.cardcrawl.neow.NeowUnlockScreen;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import com.megacrit.cardcrawl.rewards.chests.LargeChest;
import com.megacrit.cardcrawl.rewards.chests.MediumChest;
import com.megacrit.cardcrawl.rewards.chests.SmallChest;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EmptyRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoomBoss;
import com.megacrit.cardcrawl.rooms.VictoryRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.scenes.AbstractScene;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.DiscardPileViewScreen;
import com.megacrit.cardcrawl.screens.DrawPileViewScreen;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import com.megacrit.cardcrawl.screens.ExhaustPileViewScreen;
import com.megacrit.cardcrawl.screens.MasterDeckViewScreen;
import com.megacrit.cardcrawl.screens.VictoryScreen;
import com.megacrit.cardcrawl.screens.options.InputSettingsScreen;
import com.megacrit.cardcrawl.screens.options.SettingsScreen;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.stances.NeutralStance;
import com.megacrit.cardcrawl.ui.FtueTip;
import com.megacrit.cardcrawl.ui.buttons.DynamicBanner;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.UnlockCharacterScreen;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ObtainKeyEffect;
import com.megacrit.cardcrawl.vfx.PlayerTurnEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractDungeon {
    protected static final Logger logger = LogManager.getLogger(AbstractDungeon.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("AbstractDungeon");
    public static final String[] TEXT = AbstractDungeon.uiStrings.TEXT;
    public static String name;
    public static String levelNum;
    public static String id;
    public static int floorNum;
    public static int actNum;
    public static AbstractPlayer player;
    public static ArrayList<AbstractUnlock> unlocks;
    protected static float shrineChance;
    protected static float cardUpgradedChance;
    public static AbstractCard transformedCard;
    public static boolean loading_post_combat;
    public static boolean is_victory;
    public static Texture eventBackgroundImg;
    public static Random monsterRng;
    public static Random mapRng;
    public static Random eventRng;
    public static Random merchantRng;
    public static Random cardRng;
    public static Random treasureRng;
    public static Random relicRng;
    public static Random potionRng;
    public static Random monsterHpRng;
    public static Random aiRng;
    public static Random shuffleRng;
    public static Random cardRandomRng;
    public static Random miscRng;
    public static CardGroup srcColorlessCardPool;
    public static CardGroup srcCurseCardPool;
    public static CardGroup srcCommonCardPool;
    public static CardGroup srcUncommonCardPool;
    public static CardGroup srcRareCardPool;
    public static CardGroup colorlessCardPool;
    public static CardGroup curseCardPool;
    public static CardGroup commonCardPool;
    public static CardGroup uncommonCardPool;
    public static CardGroup rareCardPool;
    public static ArrayList<String> commonRelicPool;
    public static ArrayList<String> uncommonRelicPool;
    public static ArrayList<String> rareRelicPool;
    public static ArrayList<String> shopRelicPool;
    public static ArrayList<String> bossRelicPool;
    public static String lastCombatMetricKey;
    public static ArrayList<String> monsterList;
    public static ArrayList<String> eliteMonsterList;
    public static ArrayList<String> bossList;
    public static String bossKey;
    public static ArrayList<String> eventList;
    public static ArrayList<String> shrineList;
    public static ArrayList<String> specialOneTimeEventList;
    public static GameActionManager actionManager;
    public static ArrayList<AbstractGameEffect> topLevelEffects;
    public static ArrayList<AbstractGameEffect> topLevelEffectsQueue;
    public static ArrayList<AbstractGameEffect> effectList;
    public static ArrayList<AbstractGameEffect> effectsQueue;
    public static boolean turnPhaseEffectActive;
    public static float colorlessRareChance;
    protected static float shopRoomChance;
    protected static float restRoomChance;
    protected static float eventRoomChance;
    protected static float eliteRoomChance;
    protected static float treasureRoomChance;
    protected static int smallChestChance;
    protected static int mediumChestChance;
    protected static int largeChestChance;
    protected static int commonRelicChance;
    protected static int uncommonRelicChance;
    protected static int rareRelicChance;
    public static AbstractScene scene;
    public static MapRoomNode currMapNode;
    public static ArrayList<ArrayList<MapRoomNode>> map;
    public static boolean leftRoomAvailable;
    public static boolean centerRoomAvailable;
    public static boolean rightRoomAvailable;
    public static boolean firstRoomChosen;
    public static final int MAP_HEIGHT = 15;
    public static final int MAP_WIDTH = 7;
    public static final int MAP_DENSITY = 6;
    public static final int FINAL_ACT_MAP_HEIGHT = 3;
    public static RenderScene rs;
    public static ArrayList<Integer> pathX;
    public static ArrayList<Integer> pathY;
    public static Color topGradientColor;
    public static Color botGradientColor;
    public static float floorY;
    public static TopPanel topPanel;
    public static CardRewardScreen cardRewardScreen;
    public static CombatRewardScreen combatRewardScreen;
    public static BossRelicSelectScreen bossRelicScreen;
    public static MasterDeckViewScreen deckViewScreen;
    public static DiscardPileViewScreen discardPileViewScreen;
    public static DrawPileViewScreen gameDeckViewScreen;
    public static ExhaustPileViewScreen exhaustPileViewScreen;
    public static SettingsScreen settingsScreen;
    public static InputSettingsScreen inputSettingsScreen;
    public static DungeonMapScreen dungeonMapScreen;
    public static GridCardSelectScreen gridSelectScreen;
    public static HandCardSelectScreen handCardSelectScreen;
    public static ShopScreen shopScreen;
    public static CreditsScreen creditsScreen;
    public static FtueTip ftue;
    public static DeathScreen deathScreen;
    public static VictoryScreen victoryScreen;
    public static UnlockCharacterScreen unlockScreen;
    public static NeowUnlockScreen gUnlockScreen;
    public static boolean isScreenUp;
    public static OverlayMenu overlayMenu;
    public static CurrentScreen screen;
    public static CurrentScreen previousScreen;
    public static DynamicBanner dynamicBanner;
    public static boolean screenSwap;
    public static boolean isDungeonBeaten;
    public static int cardBlizzStartOffset;
    public static int cardBlizzRandomizer;
    public static int cardBlizzGrowth;
    public static int cardBlizzMaxOffset;
    public static boolean isFadingIn;
    public static boolean isFadingOut;
    public static boolean waitingOnFadeOut;
    protected static float fadeTimer;
    public static Color fadeColor;
    public static Color sourceFadeColor;
    public static MapRoomNode nextRoom;
    public static float sceneOffsetY;
    public static ArrayList<String> relicsToRemoveOnStart;
    public static int bossCount;
    public static final float SCENE_OFFSET_TIME = 1.3f;
    public static boolean isAscensionMode;
    public static int ascensionLevel;
    public static ArrayList<AbstractBlight> blightPool;
    public static boolean ascensionCheck;
    private static final Logger LOGGER;

    public AbstractDungeon(String name, String levelId, AbstractPlayer p, ArrayList<String> newSpecialOneTimeEventList) {
        ascensionCheck = UnlockTracker.isAscensionUnlocked(p);
        CardCrawlGame.dungeon = this;
        long startTime = System.currentTimeMillis();
        AbstractDungeon.name = name;
        id = levelId;
        player = p;
        topPanel.setPlayerName();
        actionManager = new GameActionManager();
        overlayMenu = new OverlayMenu(p);
        dynamicBanner = new DynamicBanner();
        unlocks.clear();
        specialOneTimeEventList = newSpecialOneTimeEventList;
        isFadingIn = false;
        isFadingOut = false;
        waitingOnFadeOut = false;
        fadeTimer = 1.0f;
        isDungeonBeaten = false;
        isScreenUp = false;
        AbstractDungeon.dungeonTransitionSetup();
        this.generateMonsters();
        this.initializeBoss();
        this.setBoss(bossList.get(0));
        this.initializeEventList();
        this.initializeEventImg();
        this.initializeShrineList();
        this.initializeCardPools();
        if (floorNum == 0) {
            p.initializeStarterDeck();
        }
        this.initializePotions();
        BlightHelper.initialize();
        if (id.equals("Exordium")) {
            screen = CurrentScreen.NONE;
            isScreenUp = false;
        } else {
            screen = CurrentScreen.MAP;
            isScreenUp = true;
        }
        logger.info("Content generation time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public AbstractDungeon(String name, AbstractPlayer p, SaveFile saveFile) {
        ascensionCheck = UnlockTracker.isAscensionUnlocked(p);
        id = saveFile.level_name;
        CardCrawlGame.dungeon = this;
        long startTime = System.currentTimeMillis();
        AbstractDungeon.name = name;
        player = p;
        topPanel.setPlayerName();
        actionManager = new GameActionManager();
        overlayMenu = new OverlayMenu(p);
        dynamicBanner = new DynamicBanner();
        isFadingIn = false;
        isFadingOut = false;
        waitingOnFadeOut = false;
        fadeTimer = 1.0f;
        isDungeonBeaten = false;
        isScreenUp = false;
        firstRoomChosen = true;
        unlocks.clear();
        try {
            this.loadSave(saveFile);
        }
        catch (Exception e) {
            logger.info("Exception occurred while loading save!");
            logger.info("Deleting save due to crash!");
            SaveAndContinue.deleteSave(player);
            ExceptionHandler.handleException(e, LOGGER);
            Gdx.app.exit();
        }
        this.initializeEventImg();
        this.initializeShrineList();
        this.initializeCardPools();
        this.initializePotions();
        BlightHelper.initialize();
        screen = CurrentScreen.NONE;
        isScreenUp = false;
        logger.info("Dungeon load time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void setBoss(String key) {
        bossKey = key;
        if (DungeonMap.boss != null && DungeonMap.bossOutline != null) {
            DungeonMap.boss.dispose();
            DungeonMap.bossOutline.dispose();
        }
        if (key.equals("The Guardian")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/guardian.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/guardian.png");
        } else if (key.equals("Hexaghost")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/hexaghost.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/hexaghost.png");
        } else if (key.equals("Slime Boss")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/slime.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/slime.png");
        } else if (key.equals("Collector")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/collector.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/collector.png");
        } else if (key.equals("Automaton")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/automaton.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/automaton.png");
        } else if (key.equals("Champ")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/champ.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/champ.png");
        } else if (key.equals("Awakened One")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/awakened.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/awakened.png");
        } else if (key.equals("Time Eater")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/timeeater.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/timeeater.png");
        } else if (key.equals("Donu and Deca")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/donu.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/donu.png");
        } else if (key.equals("The Heart")) {
            DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/heart.png");
            DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/heart.png");
        } else {
            logger.info("WARNING: UNKNOWN BOSS ICON: " + key);
            DungeonMap.boss = null;
        }
        logger.info("[BOSS] " + key);
    }

    protected abstract void initializeLevelSpecificChances();

    public static boolean isPlayerInDungeon() {
        return CardCrawlGame.dungeon != null;
    }

    public static void generateSeeds() {
        logger.info("Generating seeds: " + Settings.seed);
        monsterRng = new Random(Settings.seed);
        eventRng = new Random(Settings.seed);
        merchantRng = new Random(Settings.seed);
        cardRng = new Random(Settings.seed);
        treasureRng = new Random(Settings.seed);
        relicRng = new Random(Settings.seed);
        monsterHpRng = new Random(Settings.seed);
        potionRng = new Random(Settings.seed);
        aiRng = new Random(Settings.seed);
        shuffleRng = new Random(Settings.seed);
        cardRandomRng = new Random(Settings.seed);
        miscRng = new Random(Settings.seed);
    }

    public static void loadSeeds(SaveFile save) {
        if (save.is_daily || save.is_trial) {
            Settings.isDailyRun = save.is_daily;
            Settings.isTrial = save.is_trial;
            Settings.specialSeed = save.special_seed;
            if (save.is_daily) {
                ModHelper.setTodaysMods(save.special_seed, AbstractDungeon.player.chosenClass);
            } else {
                ModHelper.setTodaysMods(save.seed, AbstractDungeon.player.chosenClass);
            }
        }
        monsterRng = new Random(Settings.seed, save.monster_seed_count);
        eventRng = new Random(Settings.seed, save.event_seed_count);
        merchantRng = new Random(Settings.seed, save.merchant_seed_count);
        cardRng = new Random(Settings.seed, save.card_seed_count);
        cardBlizzRandomizer = save.card_random_seed_randomizer;
        treasureRng = new Random(Settings.seed, save.treasure_seed_count);
        relicRng = new Random(Settings.seed, save.relic_seed_count);
        potionRng = new Random(Settings.seed, save.potion_seed_count);
        logger.info("Loading seeds: " + Settings.seed);
        logger.info("Monster seed:  " + AbstractDungeon.monsterRng.counter);
        logger.info("Event seed:    " + AbstractDungeon.eventRng.counter);
        logger.info("Merchant seed: " + AbstractDungeon.merchantRng.counter);
        logger.info("Card seed:     " + AbstractDungeon.cardRng.counter);
        logger.info("Treasure seed: " + AbstractDungeon.treasureRng.counter);
        logger.info("Relic seed:    " + AbstractDungeon.relicRng.counter);
        logger.info("Potion seed:   " + AbstractDungeon.potionRng.counter);
    }

    public void populatePathTaken(SaveFile saveFile) {
        MapRoomNode node = null;
        if (saveFile.current_room.equals(MonsterRoomBoss.class.getName())) {
            node = new MapRoomNode(-1, 15);
            node.room = new MonsterRoomBoss();
            nextRoom = node;
        } else if (saveFile.current_room.equals(TreasureRoomBoss.class.getName())) {
            node = new MapRoomNode(-1, 15);
            node.room = new TreasureRoomBoss();
            nextRoom = node;
        } else if (saveFile.room_y == 15 && saveFile.room_x == -1) {
            node = new MapRoomNode(-1, 15);
            node.room = new VictoryRoom(VictoryRoom.EventType.HEART);
            nextRoom = node;
        } else {
            nextRoom = saveFile.current_room.equals(NeowRoom.class.getName()) ? null : map.get(saveFile.room_y).get(saveFile.room_x);
        }
        for (int i = 0; i < pathX.size(); ++i) {
            MapEdge connectedEdge;
            if (pathY.get(i) == 14) {
                MapRoomNode node2 = map.get(pathY.get(i)).get(pathX.get(i));
                for (MapEdge e : node2.getEdges()) {
                    if (e == null) continue;
                    e.markAsTaken();
                }
            }
            if (pathY.get(i) >= 15) continue;
            AbstractDungeon.map.get((int)AbstractDungeon.pathY.get((int)i).intValue()).get((int)AbstractDungeon.pathX.get((int)i).intValue()).taken = true;
            if (node != null && (connectedEdge = node.getEdgeConnectedTo(map.get(pathY.get(i)).get(pathX.get(i)))) != null) {
                connectedEdge.markAsTaken();
            }
            node = map.get(pathY.get(i)).get(pathX.get(i));
        }
        if (this.isLoadingIntoNeow(saveFile)) {
            logger.info("Loading into Neow");
            currMapNode = new MapRoomNode(0, -1);
            AbstractDungeon.currMapNode.room = new EmptyRoom();
            nextRoom = null;
        } else {
            logger.info("Loading into: " + saveFile.room_x + "," + saveFile.room_y);
            currMapNode = new MapRoomNode(0, -1);
            AbstractDungeon.currMapNode.room = new EmptyRoom();
        }
        this.nextRoomTransition(saveFile);
        if (this.isLoadingIntoNeow(saveFile)) {
            AbstractDungeon.currMapNode.room = saveFile.chose_neow_reward ? new NeowRoom(true) : new NeowRoom(false);
        }
        if (!(!(AbstractDungeon.currMapNode.room instanceof VictoryRoom) || Settings.isFinalActAvailable && Settings.hasRubyKey && Settings.hasEmeraldKey && Settings.hasSapphireKey)) {
            CardCrawlGame.stopClock = true;
        }
    }

    protected boolean isLoadingIntoNeow(SaveFile saveFile) {
        return floorNum == 0 || saveFile.current_room.equals(NeowRoom.class.getName());
    }

    public static AbstractChest getRandomChest() {
        int roll = treasureRng.random(0, 99);
        if (roll < smallChestChance) {
            return new SmallChest();
        }
        if (roll < mediumChestChance + smallChestChance) {
            return new MediumChest();
        }
        return new LargeChest();
    }

    protected static void generateMap() {
        long startTime = System.currentTimeMillis();
        int mapHeight = 15;
        int mapWidth = 7;
        int mapPathDensity = 6;
        ArrayList<AbstractRoom> roomList = new ArrayList<AbstractRoom>();
        map = MapGenerator.generateDungeon(mapHeight, mapWidth, mapPathDensity, mapRng);
        int count = 0;
        for (ArrayList<MapRoomNode> a : map) {
            for (MapRoomNode n : a) {
                if (!n.hasEdges() || n.y == map.size() - 2) continue;
                ++count;
            }
        }
        AbstractDungeon.generateRoomTypes(roomList, count);
        RoomTypeAssigner.assignRowAsRoomType(map.get(map.size() - 1), RestRoom.class);
        RoomTypeAssigner.assignRowAsRoomType(map.get(0), MonsterRoom.class);
        if (Settings.isEndless && player.hasBlight("MimicInfestation")) {
            RoomTypeAssigner.assignRowAsRoomType(map.get(8), MonsterRoomElite.class);
        } else {
            RoomTypeAssigner.assignRowAsRoomType(map.get(8), TreasureRoom.class);
        }
        map = RoomTypeAssigner.distributeRoomsAcrossMap(mapRng, map, roomList);
        logger.info("Generated the following dungeon map:");
        logger.info(MapGenerator.toString(map, true));
        logger.info("Game Seed: " + Settings.seed);
        logger.info("Map generation time: " + (System.currentTimeMillis() - startTime) + "ms");
        firstRoomChosen = false;
        AbstractDungeon.fadeIn();
        AbstractDungeon.setEmeraldElite();
    }

    protected static void setEmeraldElite() {
        if (Settings.isFinalActAvailable && !Settings.hasEmeraldKey) {
            ArrayList<MapRoomNode> eliteNodes = new ArrayList<MapRoomNode>();
            for (int i = 0; i < map.size(); ++i) {
                for (int j = 0; j < map.get(i).size(); ++j) {
                    if (!(AbstractDungeon.map.get((int)i).get((int)j).room instanceof MonsterRoomElite)) continue;
                    eliteNodes.add(map.get(i).get(j));
                }
            }
            MapRoomNode chosenNode = (MapRoomNode)eliteNodes.get(mapRng.random(0, eliteNodes.size() - 1));
            chosenNode.hasEmeraldKey = true;
            logger.info("[INFO] Elite nodes identified: " + eliteNodes.size());
            logger.info("[INFO] Emerald Key  placed in: [" + chosenNode.x + "," + chosenNode.y + "]");
        }
    }

    private static void generateRoomTypes(ArrayList<AbstractRoom> roomList, int availableRoomCount) {
        int i;
        int eliteCount;
        logger.info("Generating Room Types! There are " + availableRoomCount + " rooms:");
        int shopCount = Math.round((float)availableRoomCount * shopRoomChance);
        logger.info(" SHOP (" + AbstractDungeon.toPercentage(shopRoomChance) + "): " + shopCount);
        int restCount = Math.round((float)availableRoomCount * restRoomChance);
        logger.info(" REST (" + AbstractDungeon.toPercentage(restRoomChance) + "): " + restCount);
        int treasureCount = Math.round((float)availableRoomCount * treasureRoomChance);
        logger.info(" TRSRE (" + AbstractDungeon.toPercentage(treasureRoomChance) + "): " + treasureCount);
        if (ModHelper.isModEnabled("Elite Swarm")) {
            eliteCount = Math.round((float)availableRoomCount * (eliteRoomChance * 2.5f));
            logger.info(" ELITE (" + AbstractDungeon.toPercentage(eliteRoomChance) + "): " + eliteCount);
        } else if (ascensionLevel >= 1) {
            eliteCount = Math.round((float)availableRoomCount * eliteRoomChance * 1.6f);
            logger.info(" ELITE (" + AbstractDungeon.toPercentage(eliteRoomChance) + "): " + eliteCount);
        } else {
            eliteCount = Math.round((float)availableRoomCount * eliteRoomChance);
            logger.info(" ELITE (" + AbstractDungeon.toPercentage(eliteRoomChance) + "): " + eliteCount);
        }
        int eventCount = Math.round((float)availableRoomCount * eventRoomChance);
        logger.info(" EVNT (" + AbstractDungeon.toPercentage(eventRoomChance) + "): " + eventCount);
        int monsterCount = availableRoomCount - shopCount - restCount - treasureCount - eliteCount - eventCount;
        logger.info(" MSTR (" + AbstractDungeon.toPercentage(1.0f - shopRoomChance - restRoomChance - treasureRoomChance - eliteRoomChance - eventRoomChance) + "): " + monsterCount);
        for (i = 0; i < shopCount; ++i) {
            roomList.add(new ShopRoom());
        }
        for (i = 0; i < restCount; ++i) {
            roomList.add(new RestRoom());
        }
        for (i = 0; i < eliteCount; ++i) {
            roomList.add(new MonsterRoomElite());
        }
        for (i = 0; i < eventCount; ++i) {
            roomList.add(new EventRoom());
        }
    }

    private static String toPercentage(float n) {
        return String.format("%.0f", Float.valueOf(n * 100.0f)) + "%";
    }

    private static void firstRoomLogic() {
        AbstractDungeon.initializeFirstRoom();
        leftRoomAvailable = currMapNode.leftNodeAvailable();
        centerRoomAvailable = currMapNode.centerNodeAvailable();
        rightRoomAvailable = currMapNode.rightNodeAvailable();
    }

    private boolean passesDonutCheck(ArrayList<ArrayList<MapRoomNode>> map) {
        logger.info("CASEY'S DONUT CHECK: ");
        int width = map.get(0).size();
        int height = map.size();
        logger.info(" HEIGHT: " + height);
        logger.info(" WIDTH:  " + width);
        int nodeCount = 0;
        boolean[] roomHasNode = new boolean[width];
        for (int i = 0; i < width; ++i) {
            roomHasNode[i] = false;
        }
        ArrayList<MapRoomNode> secondToLastRow = map.get(map.size() - 2);
        for (MapRoomNode n : secondToLastRow) {
            for (MapEdge e : n.getEdges()) {
                roomHasNode[e.dstX] = true;
            }
        }
        for (int i = 0; i < width - 1; ++i) {
            if (!roomHasNode[i]) continue;
            ++nodeCount;
        }
        if (nodeCount != 1) {
            logger.info(" [FAIL] " + nodeCount + " NODES IN LAST ROW");
            return false;
        }
        logger.info(" [SUCCESS] " + nodeCount + " NODE IN LAST ROW");
        int roomCount = 0;
        for (ArrayList<MapRoomNode> rows : map) {
            for (MapRoomNode n : rows) {
                if (n.room == null) continue;
                ++roomCount;
            }
        }
        logger.info(" ROOM COUNT: " + roomCount);
        return true;
    }

    public static AbstractRoom getCurrRoom() {
        return currMapNode.getRoom();
    }

    public static MapRoomNode getCurrMapNode() {
        return currMapNode;
    }

    public static void setCurrMapNode(MapRoomNode currMapNode) {
        SoulGroup group = AbstractDungeon.currMapNode.room.souls;
        if (AbstractDungeon.currMapNode != null && AbstractDungeon.getCurrRoom() != null) {
            AbstractDungeon.getCurrRoom().dispose();
        }
        AbstractDungeon.currMapNode = currMapNode;
        if (AbstractDungeon.currMapNode.room == null) {
            logger.warn("This player loaded into a room that no longer exists (due to a new map gen?)");
            for (int i = 0; i < 5; ++i) {
                if (AbstractDungeon.map.get((int)currMapNode.y).get((int)i).room == null) continue;
                AbstractDungeon.currMapNode = map.get(currMapNode.y).get(i);
                AbstractDungeon.currMapNode.room = AbstractDungeon.map.get((int)currMapNode.y).get((int)i).room;
                AbstractDungeon.nextRoom.room = AbstractDungeon.map.get((int)currMapNode.y).get((int)i).room;
                break;
            }
        } else {
            AbstractDungeon.currMapNode.room.souls = group;
        }
    }

    public ArrayList<ArrayList<MapRoomNode>> getMap() {
        return map;
    }

    public static AbstractRelic returnRandomRelic(AbstractRelic.RelicTier tier) {
        logger.info("Returning " + tier.name() + " relic");
        return RelicLibrary.getRelic(AbstractDungeon.returnRandomRelicKey(tier)).makeCopy();
    }

    public static AbstractRelic returnRandomScreenlessRelic(AbstractRelic.RelicTier tier) {
        logger.info("Returning " + tier.name() + " relic");
        AbstractRelic tmpRelic = RelicLibrary.getRelic(AbstractDungeon.returnRandomRelicKey(tier)).makeCopy();
        while (Objects.equals(tmpRelic.relicId, "Bottled Flame") || Objects.equals(tmpRelic.relicId, "Bottled Lightning") || Objects.equals(tmpRelic.relicId, "Bottled Tornado") || Objects.equals(tmpRelic.relicId, "Whetstone")) {
            tmpRelic = RelicLibrary.getRelic(AbstractDungeon.returnRandomRelicKey(tier)).makeCopy();
        }
        return tmpRelic;
    }

    public static AbstractRelic returnRandomNonCampfireRelic(AbstractRelic.RelicTier tier) {
        logger.info("Returning " + tier.name() + " relic");
        AbstractRelic tmpRelic = RelicLibrary.getRelic(AbstractDungeon.returnRandomRelicKey(tier)).makeCopy();
        while (Objects.equals(tmpRelic.relicId, "Peace Pipe") || Objects.equals(tmpRelic.relicId, "Shovel") || Objects.equals(tmpRelic.relicId, "Girya")) {
            tmpRelic = RelicLibrary.getRelic(AbstractDungeon.returnRandomRelicKey(tier)).makeCopy();
        }
        return tmpRelic;
    }

    public static AbstractRelic returnRandomRelicEnd(AbstractRelic.RelicTier tier) {
        logger.info("Returning " + tier.name() + " relic");
        return RelicLibrary.getRelic(AbstractDungeon.returnEndRandomRelicKey(tier)).makeCopy();
    }

    public static String returnEndRandomRelicKey(AbstractRelic.RelicTier tier) {
        String retVal = null;
        switch (tier) {
            case COMMON: {
                if (commonRelicPool.isEmpty()) {
                    retVal = AbstractDungeon.returnRandomRelicKey(AbstractRelic.RelicTier.UNCOMMON);
                    break;
                }
                retVal = commonRelicPool.remove(commonRelicPool.size() - 1);
                break;
            }
            case UNCOMMON: {
                if (uncommonRelicPool.isEmpty()) {
                    retVal = AbstractDungeon.returnRandomRelicKey(AbstractRelic.RelicTier.RARE);
                    break;
                }
                retVal = uncommonRelicPool.remove(uncommonRelicPool.size() - 1);
                break;
            }
            case RARE: {
                if (rareRelicPool.isEmpty()) {
                    retVal = "Circlet";
                    break;
                }
                retVal = rareRelicPool.remove(rareRelicPool.size() - 1);
                break;
            }
            case SHOP: {
                if (shopRelicPool.isEmpty()) {
                    retVal = AbstractDungeon.returnRandomRelicKey(AbstractRelic.RelicTier.UNCOMMON);
                    break;
                }
                retVal = shopRelicPool.remove(shopRelicPool.size() - 1);
                break;
            }
            case BOSS: {
                if (bossRelicPool.isEmpty()) {
                    retVal = "Red Circlet";
                    break;
                }
                retVal = bossRelicPool.remove(0);
                break;
            }
            default: {
                logger.info("Incorrect relic tier: " + tier.name() + " was called in returnEndRandomRelicKey()");
            }
        }
        if (!RelicLibrary.getRelic(retVal).canSpawn()) {
            return AbstractDungeon.returnEndRandomRelicKey(tier);
        }
        return retVal;
    }

    public static String returnRandomRelicKey(AbstractRelic.RelicTier tier) {
        String retVal = null;
        switch (tier) {
            case COMMON: {
                if (commonRelicPool.isEmpty()) {
                    retVal = AbstractDungeon.returnRandomRelicKey(AbstractRelic.RelicTier.UNCOMMON);
                    break;
                }
                retVal = commonRelicPool.remove(0);
                break;
            }
            case UNCOMMON: {
                if (uncommonRelicPool.isEmpty()) {
                    retVal = AbstractDungeon.returnRandomRelicKey(AbstractRelic.RelicTier.RARE);
                    break;
                }
                retVal = uncommonRelicPool.remove(0);
                break;
            }
            case RARE: {
                if (rareRelicPool.isEmpty()) {
                    retVal = "Circlet";
                    break;
                }
                retVal = rareRelicPool.remove(0);
                break;
            }
            case SHOP: {
                if (shopRelicPool.isEmpty()) {
                    retVal = AbstractDungeon.returnRandomRelicKey(AbstractRelic.RelicTier.UNCOMMON);
                    break;
                }
                retVal = shopRelicPool.remove(0);
                break;
            }
            case BOSS: {
                if (bossRelicPool.isEmpty()) {
                    retVal = "Red Circlet";
                    break;
                }
                retVal = bossRelicPool.remove(0);
                break;
            }
            default: {
                logger.info("Incorrect relic tier: " + tier.name() + " was called in returnRandomRelicKey()");
            }
        }
        if (!RelicLibrary.getRelic(retVal).canSpawn()) {
            return AbstractDungeon.returnEndRandomRelicKey(tier);
        }
        return retVal;
    }

    public static AbstractRelic.RelicTier returnRandomRelicTier() {
        int roll = relicRng.random(0, 99);
        if (roll < commonRelicChance) {
            return AbstractRelic.RelicTier.COMMON;
        }
        if (roll < commonRelicChance + uncommonRelicChance) {
            return AbstractRelic.RelicTier.UNCOMMON;
        }
        return AbstractRelic.RelicTier.RARE;
    }

    public static AbstractPotion returnTotallyRandomPotion() {
        return PotionHelper.getRandomPotion();
    }

    public static AbstractPotion returnRandomPotion() {
        return AbstractDungeon.returnRandomPotion(false);
    }

    public static AbstractPotion returnRandomPotion(boolean limited) {
        int roll = potionRng.random(0, 99);
        if (roll < PotionHelper.POTION_COMMON_CHANCE) {
            return AbstractDungeon.returnRandomPotion(AbstractPotion.PotionRarity.COMMON, limited);
        }
        if (roll < PotionHelper.POTION_UNCOMMON_CHANCE + PotionHelper.POTION_COMMON_CHANCE) {
            return AbstractDungeon.returnRandomPotion(AbstractPotion.PotionRarity.UNCOMMON, limited);
        }
        return AbstractDungeon.returnRandomPotion(AbstractPotion.PotionRarity.RARE, limited);
    }

    public static AbstractPotion returnRandomPotion(AbstractPotion.PotionRarity rarity, boolean limited) {
        AbstractPotion temp = PotionHelper.getRandomPotion();
        boolean spamCheck = limited;
        while (temp.rarity != rarity || spamCheck) {
            spamCheck = limited;
            temp = PotionHelper.getRandomPotion();
            if (temp.ID == "Fruit Juice") continue;
            spamCheck = false;
        }
        return temp;
    }

    public static void transformCard(AbstractCard c) {
        AbstractDungeon.transformCard(c, false);
    }

    public static void transformCard(AbstractCard c, boolean autoUpgrade) {
        AbstractDungeon.transformCard(c, autoUpgrade, new Random());
    }

    public static void transformCard(AbstractCard c, boolean autoUpgrade, Random rng) {
        switch (c.color) {
            case COLORLESS: {
                transformedCard = AbstractDungeon.returnTrulyRandomColorlessCardFromAvailable(c, rng).makeCopy();
                break;
            }
            case CURSE: {
                transformedCard = CardLibrary.getCurse(c, rng).makeCopy();
                break;
            }
            default: {
                transformedCard = AbstractDungeon.returnTrulyRandomCardFromAvailable(c, rng).makeCopy();
            }
        }
        UnlockTracker.markCardAsSeen(AbstractDungeon.transformedCard.cardID);
        if (autoUpgrade && transformedCard.canUpgrade()) {
            transformedCard.upgrade();
        }
    }

    public static void srcTransformCard(AbstractCard c) {
        logger.info("Transform using SRC pool...");
        switch (c.rarity) {
            case BASIC: {
                transformedCard = srcCommonCardPool.getRandomCard(false).makeCopy();
                break;
            }
            case COMMON: {
                srcCommonCardPool.removeCard(c.cardID);
                transformedCard = srcCommonCardPool.getRandomCard(false).makeCopy();
                srcCommonCardPool.addToTop(c.makeCopy());
                break;
            }
            case UNCOMMON: {
                srcUncommonCardPool.removeCard(c.cardID);
                transformedCard = srcUncommonCardPool.getRandomCard(false).makeCopy();
                srcUncommonCardPool.addToTop(c.makeCopy());
                break;
            }
            case RARE: {
                srcRareCardPool.removeCard(c.cardID);
                transformedCard = srcRareCardPool.isEmpty() ? srcUncommonCardPool.getRandomCard(false).makeCopy() : srcRareCardPool.getRandomCard(false).makeCopy();
                srcRareCardPool.addToTop(c.makeCopy());
                break;
            }
            case CURSE: {
                transformedCard = !srcRareCardPool.isEmpty() ? srcRareCardPool.getRandomCard(false).makeCopy() : srcUncommonCardPool.getRandomCard(false).makeCopy();
            }
            default: {
                logger.info("Transform called on a strange card type: " + c.type.name());
                transformedCard = srcCommonCardPool.getRandomCard(false).makeCopy();
            }
        }
    }

    public static CardGroup getEachRare() {
        CardGroup everyRareCard = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (AbstractCard c : AbstractDungeon.rareCardPool.group) {
            everyRareCard.addToBottom(c.makeCopy());
        }
        return everyRareCard;
    }

    public static AbstractCard returnRandomCard() {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        AbstractCard.CardRarity rarity = AbstractDungeon.rollRarity();
        if (rarity.equals((Object)AbstractCard.CardRarity.COMMON)) {
            list.addAll(AbstractDungeon.srcCommonCardPool.group);
        } else if (rarity.equals((Object)AbstractCard.CardRarity.UNCOMMON)) {
            list.addAll(AbstractDungeon.srcUncommonCardPool.group);
        } else {
            list.addAll(AbstractDungeon.srcRareCardPool.group);
        }
        return (AbstractCard)list.get(cardRandomRng.random(list.size() - 1));
    }

    public static AbstractCard returnTrulyRandomCard() {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        list.addAll(AbstractDungeon.srcCommonCardPool.group);
        list.addAll(AbstractDungeon.srcUncommonCardPool.group);
        list.addAll(AbstractDungeon.srcRareCardPool.group);
        return (AbstractCard)list.get(cardRandomRng.random(list.size() - 1));
    }

    public static AbstractCard returnTrulyRandomCardInCombat() {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.srcCommonCardPool.group) {
            if (c.hasTag(AbstractCard.CardTags.HEALING)) continue;
            list.add(c);
            UnlockTracker.markCardAsSeen(c.cardID);
        }
        for (AbstractCard c : AbstractDungeon.srcUncommonCardPool.group) {
            if (c.hasTag(AbstractCard.CardTags.HEALING)) continue;
            list.add(c);
            UnlockTracker.markCardAsSeen(c.cardID);
        }
        for (AbstractCard c : AbstractDungeon.srcRareCardPool.group) {
            if (c.hasTag(AbstractCard.CardTags.HEALING)) continue;
            list.add(c);
            UnlockTracker.markCardAsSeen(c.cardID);
        }
        return (AbstractCard)list.get(cardRandomRng.random(list.size() - 1));
    }

    public static AbstractCard returnTrulyRandomCardInCombat(AbstractCard.CardType type) {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.srcCommonCardPool.group) {
            if (c.type != type || c.hasTag(AbstractCard.CardTags.HEALING)) continue;
            list.add(c);
        }
        for (AbstractCard c : AbstractDungeon.srcUncommonCardPool.group) {
            if (c.type != type || c.hasTag(AbstractCard.CardTags.HEALING)) continue;
            list.add(c);
        }
        for (AbstractCard c : AbstractDungeon.srcRareCardPool.group) {
            if (c.type != type || c.hasTag(AbstractCard.CardTags.HEALING)) continue;
            list.add(c);
        }
        return (AbstractCard)list.get(cardRandomRng.random(list.size() - 1));
    }

    public static AbstractCard returnTrulyRandomColorlessCardInCombat() {
        return AbstractDungeon.returnTrulyRandomColorlessCardInCombat(cardRandomRng);
    }

    public static AbstractCard returnTrulyRandomColorlessCardInCombat(String prohibitedID) {
        return AbstractDungeon.returnTrulyRandomColorlessCardFromAvailable(prohibitedID, cardRandomRng);
    }

    public static AbstractCard returnTrulyRandomColorlessCardInCombat(Random rng) {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.srcColorlessCardPool.group) {
            if (c.hasTag(AbstractCard.CardTags.HEALING)) continue;
            list.add(c);
        }
        return (AbstractCard)list.get(rng.random(list.size() - 1));
    }

    public static AbstractCard returnTrulyRandomColorlessCardFromAvailable(String prohibited, Random rng) {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.srcColorlessCardPool.group) {
            if (c.cardID == prohibited) continue;
            list.add(c);
        }
        return (AbstractCard)list.get(rng.random(list.size() - 1));
    }

    public static AbstractCard returnTrulyRandomColorlessCardFromAvailable(AbstractCard prohibited, Random rng) {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.srcColorlessCardPool.group) {
            if (Objects.equals(c.cardID, prohibited.cardID)) continue;
            list.add(c);
        }
        return (AbstractCard)list.get(rng.random(list.size() - 1));
    }

    public static AbstractCard returnTrulyRandomCardFromAvailable(AbstractCard prohibited, Random rng) {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        switch (prohibited.color) {
            case COLORLESS: {
                for (AbstractCard c : AbstractDungeon.colorlessCardPool.group) {
                    if (Objects.equals(c.cardID, prohibited.cardID)) continue;
                    list.add(c);
                }
                break;
            }
            case CURSE: {
                return CardLibrary.getCurse();
            }
            default: {
                for (AbstractCard c : AbstractDungeon.commonCardPool.group) {
                    if (Objects.equals(c.cardID, prohibited.cardID)) continue;
                    list.add(c);
                }
                for (AbstractCard c : AbstractDungeon.srcUncommonCardPool.group) {
                    if (Objects.equals(c.cardID, prohibited.cardID)) continue;
                    list.add(c);
                }
                for (AbstractCard c : AbstractDungeon.srcRareCardPool.group) {
                    if (Objects.equals(c.cardID, prohibited.cardID)) continue;
                    list.add(c);
                }
            }
        }
        return ((AbstractCard)list.get(rng.random(list.size() - 1))).makeCopy();
    }

    public static AbstractCard returnTrulyRandomCardFromAvailable(AbstractCard prohibited) {
        return AbstractDungeon.returnTrulyRandomCardFromAvailable(prohibited, new Random());
    }

    public static AbstractCard getTransformedCard() {
        AbstractCard retVal = transformedCard;
        transformedCard = null;
        return retVal;
    }

    public void populateFirstStrongEnemy(ArrayList<MonsterInfo> monsters, ArrayList<String> exclusions) {
        String m;
        while (exclusions.contains(m = MonsterInfo.roll(monsters, monsterRng.random()))) {
        }
        monsterList.add(m);
    }

    public void populateMonsterList(ArrayList<MonsterInfo> monsters, int numMonsters, boolean elites) {
        if (elites) {
            for (int i = 0; i < numMonsters; ++i) {
                if (eliteMonsterList.isEmpty()) {
                    eliteMonsterList.add(MonsterInfo.roll(monsters, monsterRng.random()));
                    continue;
                }
                String toAdd = MonsterInfo.roll(monsters, monsterRng.random());
                if (!toAdd.equals(eliteMonsterList.get(eliteMonsterList.size() - 1))) {
                    eliteMonsterList.add(toAdd);
                    continue;
                }
                --i;
            }
        } else {
            for (int i = 0; i < numMonsters; ++i) {
                if (monsterList.isEmpty()) {
                    monsterList.add(MonsterInfo.roll(monsters, monsterRng.random()));
                    continue;
                }
                String toAdd = MonsterInfo.roll(monsters, monsterRng.random());
                if (!toAdd.equals(monsterList.get(monsterList.size() - 1))) {
                    if (monsterList.size() > 1 && toAdd.equals(monsterList.get(monsterList.size() - 2))) {
                        --i;
                        continue;
                    }
                    monsterList.add(toAdd);
                    continue;
                }
                --i;
            }
        }
    }

    protected abstract ArrayList<String> generateExclusions();

    public static AbstractCard returnColorlessCard(AbstractCard.CardRarity rarity) {
        Collections.shuffle(AbstractDungeon.colorlessCardPool.group, new java.util.Random(shuffleRng.randomLong()));
        for (AbstractCard c : AbstractDungeon.colorlessCardPool.group) {
            if (c.rarity != rarity) continue;
            return c.makeCopy();
        }
        if (rarity == AbstractCard.CardRarity.RARE) {
            for (AbstractCard c : AbstractDungeon.colorlessCardPool.group) {
                if (c.rarity != AbstractCard.CardRarity.UNCOMMON) continue;
                return c.makeCopy();
            }
        }
        return new SwiftStrike();
    }

    public static AbstractCard returnColorlessCard() {
        Collections.shuffle(AbstractDungeon.colorlessCardPool.group);
        Iterator<AbstractCard> iterator = AbstractDungeon.colorlessCardPool.group.iterator();
        if (iterator.hasNext()) {
            AbstractCard c = iterator.next();
            return c.makeCopy();
        }
        return new SwiftStrike();
    }

    public static AbstractCard returnRandomCurse() {
        AbstractCard c = CardLibrary.getCurse().makeCopy();
        UnlockTracker.markCardAsSeen(c.cardID);
        return c;
    }

    public void initializePotions() {
        PotionHelper.initialize(AbstractDungeon.player.chosenClass);
    }

    public void initializeCardPools() {
        logger.info("INIT CARD POOL");
        long startTime = System.currentTimeMillis();
        commonCardPool.clear();
        uncommonCardPool.clear();
        rareCardPool.clear();
        colorlessCardPool.clear();
        curseCardPool.clear();
        ArrayList<AbstractCard> tmpPool = new ArrayList<AbstractCard>();
        if (ModHelper.isModEnabled("Colorless Cards")) {
            CardLibrary.addColorlessCards(tmpPool);
        }
        if (ModHelper.isModEnabled("Diverse")) {
            CardLibrary.addRedCards(tmpPool);
            CardLibrary.addGreenCards(tmpPool);
            CardLibrary.addBlueCards(tmpPool);
            if (!UnlockTracker.isCharacterLocked("Watcher")) {
                CardLibrary.addPurpleCards(tmpPool);
            }
        } else {
            player.getCardPool(tmpPool);
        }
        this.addColorlessCards();
        this.addCurseCards();
        block6: for (AbstractCard c : tmpPool) {
            switch (c.rarity) {
                case COMMON: {
                    commonCardPool.addToTop(c);
                    continue block6;
                }
                case UNCOMMON: {
                    uncommonCardPool.addToTop(c);
                    continue block6;
                }
                case RARE: {
                    rareCardPool.addToTop(c);
                    continue block6;
                }
                case CURSE: {
                    curseCardPool.addToTop(c);
                    continue block6;
                }
            }
            logger.info("Unspecified rarity: " + c.rarity.name() + " when creating pools! AbstractDungeon: Line 827");
        }
        srcColorlessCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcCurseCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcRareCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcUncommonCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcCommonCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        for (AbstractCard c : AbstractDungeon.colorlessCardPool.group) {
            srcColorlessCardPool.addToBottom(c);
        }
        for (AbstractCard c : AbstractDungeon.curseCardPool.group) {
            srcCurseCardPool.addToBottom(c);
        }
        for (AbstractCard c : AbstractDungeon.rareCardPool.group) {
            srcRareCardPool.addToBottom(c);
        }
        for (AbstractCard c : AbstractDungeon.uncommonCardPool.group) {
            srcUncommonCardPool.addToBottom(c);
        }
        for (AbstractCard c : AbstractDungeon.commonCardPool.group) {
            srcCommonCardPool.addToBottom(c);
        }
        logger.info("Cardpool load time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void addColorlessCards() {
        for (Map.Entry<String, AbstractCard> c : CardLibrary.cards.entrySet()) {
            AbstractCard card = c.getValue();
            if (card.color != AbstractCard.CardColor.COLORLESS || card.rarity == AbstractCard.CardRarity.BASIC || card.rarity == AbstractCard.CardRarity.SPECIAL || card.type == AbstractCard.CardType.STATUS) continue;
            colorlessCardPool.addToTop(card);
        }
        logger.info("COLORLESS CARDS: " + colorlessCardPool.size());
    }

    private void addCurseCards() {
        for (Map.Entry<String, AbstractCard> c : CardLibrary.cards.entrySet()) {
            AbstractCard card = c.getValue();
            if (card.type != AbstractCard.CardType.CURSE || Objects.equals(card.cardID, "Necronomicurse") || Objects.equals(card.cardID, "AscendersBane") || Objects.equals(card.cardID, "CurseOfTheBell") || Objects.equals(card.cardID, "Pride")) continue;
            curseCardPool.addToTop(card);
        }
        logger.info("CURSE CARDS: " + curseCardPool.size());
    }

    protected void initializeRelicList() {
        commonRelicPool.clear();
        uncommonRelicPool.clear();
        rareRelicPool.clear();
        shopRelicPool.clear();
        bossRelicPool.clear();
        RelicLibrary.populateRelicPool(commonRelicPool, AbstractRelic.RelicTier.COMMON, AbstractDungeon.player.chosenClass);
        RelicLibrary.populateRelicPool(uncommonRelicPool, AbstractRelic.RelicTier.UNCOMMON, AbstractDungeon.player.chosenClass);
        RelicLibrary.populateRelicPool(rareRelicPool, AbstractRelic.RelicTier.RARE, AbstractDungeon.player.chosenClass);
        RelicLibrary.populateRelicPool(shopRelicPool, AbstractRelic.RelicTier.SHOP, AbstractDungeon.player.chosenClass);
        RelicLibrary.populateRelicPool(bossRelicPool, AbstractRelic.RelicTier.BOSS, AbstractDungeon.player.chosenClass);
        if (floorNum >= 1) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                relicsToRemoveOnStart.add(r.relicId);
            }
        }
        Collections.shuffle(commonRelicPool, new java.util.Random(relicRng.randomLong()));
        Collections.shuffle(uncommonRelicPool, new java.util.Random(relicRng.randomLong()));
        Collections.shuffle(rareRelicPool, new java.util.Random(relicRng.randomLong()));
        Collections.shuffle(shopRelicPool, new java.util.Random(relicRng.randomLong()));
        Collections.shuffle(bossRelicPool, new java.util.Random(relicRng.randomLong()));
        if (ModHelper.isModEnabled("Flight") || ModHelper.isModEnabled("Uncertain Future")) {
            relicsToRemoveOnStart.add("WingedGreaves");
        }
        if (ModHelper.isModEnabled("Diverse")) {
            relicsToRemoveOnStart.add("PrismaticShard");
        }
        if (ModHelper.isModEnabled("DeadlyEvents")) {
            relicsToRemoveOnStart.add("Juzu Bracelet");
        }
        if (ModHelper.isModEnabled("Hoarder")) {
            relicsToRemoveOnStart.add("Smiling Mask");
        }
        if (ModHelper.isModEnabled("Draft") || ModHelper.isModEnabled("SealedDeck") || ModHelper.isModEnabled("Shiny") || ModHelper.isModEnabled("Insanity")) {
            relicsToRemoveOnStart.add("Pandora's Box");
        }
        block1: for (String remove : relicsToRemoveOnStart) {
            String derp;
            Iterator<String> s = commonRelicPool.iterator();
            while (s.hasNext()) {
                derp = s.next();
                if (!derp.equals(remove)) continue;
                s.remove();
                logger.info(derp + " removed.");
                break;
            }
            s = uncommonRelicPool.iterator();
            while (s.hasNext()) {
                derp = s.next();
                if (!derp.equals(remove)) continue;
                s.remove();
                logger.info(derp + " removed.");
                break;
            }
            s = rareRelicPool.iterator();
            while (s.hasNext()) {
                derp = s.next();
                if (!derp.equals(remove)) continue;
                s.remove();
                logger.info(derp + " removed.");
                break;
            }
            s = bossRelicPool.iterator();
            while (s.hasNext()) {
                derp = s.next();
                if (!derp.equals(remove)) continue;
                s.remove();
                logger.info(derp + " removed.");
                break;
            }
            s = shopRelicPool.iterator();
            while (s.hasNext()) {
                derp = s.next();
                if (!derp.equals(remove)) continue;
                s.remove();
                logger.info(derp + " removed.");
                continue block1;
            }
        }
        if (Settings.isDebug) {
            logger.info("Relic (Common):");
            for (String s : commonRelicPool) {
                logger.info(" " + s);
            }
            logger.info("Relic (Uncommon):");
            for (String s : uncommonRelicPool) {
                logger.info(" " + s);
            }
            logger.info("Relic (Rare):");
            for (String s : rareRelicPool) {
                logger.info(" " + s);
            }
            logger.info("Relic (Shop):");
            for (String s : shopRelicPool) {
                logger.info(" " + s);
            }
            logger.info("Relic (Boss):");
            for (String s : bossRelicPool) {
                logger.info(" " + s);
            }
        }
    }

    protected abstract void generateMonsters();

    protected abstract void generateWeakEnemies(int var1);

    protected abstract void generateStrongEnemies(int var1);

    protected abstract void generateElites(int var1);

    protected abstract void initializeBoss();

    protected abstract void initializeEventList();

    protected abstract void initializeEventImg();

    protected abstract void initializeShrineList();

    public void initializeSpecialOneTimeEventList() {
        specialOneTimeEventList.clear();
        specialOneTimeEventList.add("Accursed Blacksmith");
        specialOneTimeEventList.add("Bonfire Elementals");
        specialOneTimeEventList.add("Designer");
        specialOneTimeEventList.add("Duplicator");
        specialOneTimeEventList.add("FaceTrader");
        specialOneTimeEventList.add("Fountain of Cleansing");
        specialOneTimeEventList.add("Knowing Skull");
        specialOneTimeEventList.add("Lab");
        specialOneTimeEventList.add("N'loth");
        if (this.isNoteForYourselfAvailable()) {
            specialOneTimeEventList.add("NoteForYourself");
        }
        specialOneTimeEventList.add("SecretPortal");
        specialOneTimeEventList.add("The Joust");
        specialOneTimeEventList.add("WeMeetAgain");
        specialOneTimeEventList.add("The Woman in Blue");
    }

    private boolean isNoteForYourselfAvailable() {
        if (Settings.isDailyRun) {
            logger.info("Note For Yourself is disabled due to Daily Run");
            return false;
        }
        if (ascensionLevel >= 15) {
            logger.info("Note For Yourself is disabled beyond Ascension 15+");
            return false;
        }
        if (ascensionLevel == 0) {
            logger.info("Note For Yourself is enabled due to No Ascension");
            return true;
        }
        if (ascensionLevel < player.getPrefs().getInteger("ASCENSION_LEVEL")) {
            logger.info("Note For Yourself is enabled as it's less than Highest Unlocked Ascension");
            return true;
        }
        logger.info("Note For Yourself is disabled as requirements aren't met");
        return false;
    }

    public static ArrayList<AbstractCard> getColorlessRewardCards() {
        ArrayList<AbstractCard> retVal = new ArrayList<AbstractCard>();
        int numCards = 3;
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            numCards = r.changeNumberOfCardsInReward(numCards);
        }
        if (ModHelper.isModEnabled("Binary")) {
            --numCards;
        }
        for (int i = 0; i < numCards; ++i) {
            AbstractCard.CardRarity rarity = AbstractDungeon.rollRareOrUncommon(colorlessRareChance);
            AbstractCard card = null;
            switch (rarity) {
                case RARE: {
                    card = AbstractDungeon.getColorlessCardFromPool(rarity);
                    cardBlizzRandomizer = cardBlizzStartOffset;
                    break;
                }
                case UNCOMMON: {
                    card = AbstractDungeon.getColorlessCardFromPool(rarity);
                    break;
                }
                default: {
                    logger.info("WTF?");
                }
            }
            while (retVal.contains(card)) {
                if (card != null) {
                    logger.info("DUPE: " + card.originalName);
                }
                card = AbstractDungeon.getColorlessCardFromPool(rarity);
            }
            if (card == null) continue;
            retVal.add(card);
        }
        ArrayList<AbstractCard> retVal2 = new ArrayList<AbstractCard>();
        for (AbstractCard c : retVal) {
            retVal2.add(c.makeCopy());
        }
        return retVal2;
    }

    public static ArrayList<AbstractCard> getRewardCards() {
        ArrayList<AbstractCard> retVal = new ArrayList<AbstractCard>();
        int numCards = 3;
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            numCards = r.changeNumberOfCardsInReward(numCards);
        }
        if (ModHelper.isModEnabled("Binary")) {
            --numCards;
        }
        for (int i = 0; i < numCards; ++i) {
            AbstractCard.CardRarity rarity = AbstractDungeon.rollRarity();
            AbstractCard card = null;
            switch (rarity) {
                case RARE: {
                    cardBlizzRandomizer = cardBlizzStartOffset;
                    break;
                }
                case UNCOMMON: {
                    break;
                }
                case COMMON: {
                    if ((cardBlizzRandomizer -= cardBlizzGrowth) > cardBlizzMaxOffset) break;
                    cardBlizzRandomizer = cardBlizzMaxOffset;
                    break;
                }
                default: {
                    logger.info("WTF?");
                }
            }
            boolean containsDupe = true;
            block7: while (containsDupe) {
                containsDupe = false;
                card = player.hasRelic("PrismaticShard") ? CardLibrary.getAnyColorCard(rarity) : AbstractDungeon.getCard(rarity);
                for (AbstractCard c : retVal) {
                    if (!c.cardID.equals(card.cardID)) continue;
                    containsDupe = true;
                    continue block7;
                }
            }
            if (card == null) continue;
            retVal.add(card);
        }
        ArrayList<AbstractCard> retVal2 = new ArrayList<AbstractCard>();
        for (AbstractCard c : retVal) {
            retVal2.add(c.makeCopy());
        }
        for (AbstractCard c : retVal2) {
            if (c.rarity != AbstractCard.CardRarity.RARE && cardRng.randomBoolean(cardUpgradedChance) && c.canUpgrade()) {
                c.upgrade();
                continue;
            }
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onPreviewObtainCard(c);
            }
        }
        return retVal2;
    }

    public static AbstractCard getCard(AbstractCard.CardRarity rarity) {
        switch (rarity) {
            case RARE: {
                return rareCardPool.getRandomCard(true);
            }
            case UNCOMMON: {
                return uncommonCardPool.getRandomCard(true);
            }
            case COMMON: {
                return commonCardPool.getRandomCard(true);
            }
            case CURSE: {
                return curseCardPool.getRandomCard(true);
            }
        }
        logger.info("No rarity on getCard in Abstract Dungeon");
        return null;
    }

    public static AbstractCard getCard(AbstractCard.CardRarity rarity, Random rng) {
        switch (rarity) {
            case RARE: {
                return rareCardPool.getRandomCard(rng);
            }
            case UNCOMMON: {
                return uncommonCardPool.getRandomCard(rng);
            }
            case COMMON: {
                return commonCardPool.getRandomCard(rng);
            }
            case CURSE: {
                return curseCardPool.getRandomCard(rng);
            }
        }
        logger.info("No rarity on getCard in Abstract Dungeon");
        return null;
    }

    public static AbstractCard getCardWithoutRng(AbstractCard.CardRarity rarity) {
        switch (rarity) {
            case RARE: {
                return rareCardPool.getRandomCard(false);
            }
            case UNCOMMON: {
                return uncommonCardPool.getRandomCard(false);
            }
            case COMMON: {
                return commonCardPool.getRandomCard(false);
            }
            case CURSE: {
                return AbstractDungeon.returnRandomCurse();
            }
        }
        logger.info("Check getCardWithoutRng");
        return null;
    }

    public static AbstractCard getCardFromPool(AbstractCard.CardRarity rarity, AbstractCard.CardType type, boolean useRng) {
        switch (rarity) {
            case RARE: {
                AbstractCard retVal = rareCardPool.getRandomCard(type, useRng);
                if (retVal != null) {
                    return retVal;
                }
                logger.info("ERROR: Could not find Rare card of type: " + type.name());
            }
            case UNCOMMON: {
                AbstractCard retVal = uncommonCardPool.getRandomCard(type, useRng);
                if (retVal != null) {
                    return retVal;
                }
                if (type == AbstractCard.CardType.POWER) {
                    return AbstractDungeon.getCardFromPool(AbstractCard.CardRarity.RARE, type, useRng);
                }
                logger.info("ERROR: Could not find Uncommon card of type: " + type.name());
            }
            case COMMON: {
                AbstractCard retVal = commonCardPool.getRandomCard(type, useRng);
                if (retVal != null) {
                    return retVal;
                }
                if (type == AbstractCard.CardType.POWER) {
                    return AbstractDungeon.getCardFromPool(AbstractCard.CardRarity.UNCOMMON, type, useRng);
                }
                logger.info("ERROR: Could not find Common card of type: " + type.name());
            }
            case CURSE: {
                AbstractCard retVal = curseCardPool.getRandomCard(type, useRng);
                if (retVal != null) {
                    return retVal;
                }
                logger.info("ERROR: Could not find Curse card of type: " + type.name());
            }
        }
        logger.info("ERROR: Default in getCardFromPool");
        return null;
    }

    public static AbstractCard getColorlessCardFromPool(AbstractCard.CardRarity rarity) {
        switch (rarity) {
            case RARE: {
                AbstractCard retVal = colorlessCardPool.getRandomCard(true, rarity);
                if (retVal != null) {
                    return retVal;
                }
            }
            case UNCOMMON: {
                AbstractCard retVal = colorlessCardPool.getRandomCard(true, rarity);
                if (retVal == null) break;
                return retVal;
            }
        }
        logger.info("ERROR: getColorlessCardFromPool");
        return null;
    }

    public static AbstractCard.CardRarity rollRarity(Random rng) {
        int roll = cardRng.random(99);
        roll += cardBlizzRandomizer;
        if (currMapNode == null) {
            return AbstractDungeon.getCardRarityFallback(roll);
        }
        return AbstractDungeon.getCurrRoom().getCardRarity(roll);
    }

    private static AbstractCard.CardRarity getCardRarityFallback(int roll) {
        int rareRate = 3;
        if (roll < rareRate) {
            return AbstractCard.CardRarity.RARE;
        }
        if (roll < 40) {
            return AbstractCard.CardRarity.UNCOMMON;
        }
        return AbstractCard.CardRarity.COMMON;
    }

    public static AbstractCard.CardRarity rollRarity() {
        return AbstractDungeon.rollRarity(cardRng);
    }

    public static AbstractCard.CardRarity rollRareOrUncommon(float rareChance) {
        if (cardRng.randomBoolean(rareChance)) {
            return AbstractCard.CardRarity.RARE;
        }
        return AbstractCard.CardRarity.UNCOMMON;
    }

    public static AbstractMonster getRandomMonster() {
        return AbstractDungeon.currMapNode.room.monsters.getRandomMonster(null, true, cardRandomRng);
    }

    public static AbstractMonster getRandomMonster(AbstractMonster except) {
        return AbstractDungeon.currMapNode.room.monsters.getRandomMonster(except, true, cardRandomRng);
    }

    public static void nextRoomTransitionStart() {
        AbstractDungeon.fadeOut();
        waitingOnFadeOut = true;
        AbstractDungeon.overlayMenu.proceedButton.hide();
        if (ModHelper.isModEnabled("Terminal")) {
            player.decreaseMaxHealth(1);
        }
    }

    public static void initializeFirstRoom() {
        AbstractDungeon.fadeIn();
        ++floorNum;
        if (AbstractDungeon.currMapNode.room instanceof MonsterRoom) {
            if (!CardCrawlGame.loadingSave) {
                if (SaveHelper.shouldSave()) {
                    SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
                } else {
                    Metrics metrics = new Metrics();
                    metrics.setValues(false, false, null, Metrics.MetricRequestType.NONE);
                    metrics.gatherAllDataAndSave(false, false, null);
                }
            }
            --floorNum;
        }
        scene.nextRoom(AbstractDungeon.currMapNode.room);
    }

    public static void resetPlayer() {
        AbstractDungeon.player.orbs.clear();
        AbstractDungeon.player.animX = 0.0f;
        AbstractDungeon.player.animY = 0.0f;
        player.hideHealthBar();
        AbstractDungeon.player.hand.clear();
        AbstractDungeon.player.powers.clear();
        AbstractDungeon.player.drawPile.clear();
        AbstractDungeon.player.discardPile.clear();
        AbstractDungeon.player.exhaustPile.clear();
        AbstractDungeon.player.limbo.clear();
        player.loseBlock(true);
        AbstractDungeon.player.damagedThisCombat = 0;
        if (!AbstractDungeon.player.stance.ID.equals("Neutral")) {
            AbstractDungeon.player.stance = new NeutralStance();
            player.onStanceChange("Neutral");
        }
        GameActionManager.turn = 1;
    }

    public void nextRoomTransition() {
        this.nextRoomTransition(null);
    }

    public void nextRoomTransition(SaveFile saveFile) {
        AbstractCard tmpCard;
        AbstractDungeon.overlayMenu.proceedButton.setLabel(TEXT[0]);
        combatRewardScreen.clear();
        if (nextRoom != null && AbstractDungeon.nextRoom.room != null) {
            AbstractDungeon.nextRoom.room.rewards.clear();
        }
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomElite) {
            if (!eliteMonsterList.isEmpty()) {
                logger.info("Removing elite: " + eliteMonsterList.get(0) + " from monster list.");
                eliteMonsterList.remove(0);
            } else {
                this.generateElites(10);
            }
        } else if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {
            if (!monsterList.isEmpty()) {
                logger.info("Removing monster: " + monsterList.get(0) + " from monster list.");
                monsterList.remove(0);
            } else {
                this.generateStrongEnemies(12);
            }
        } else if (AbstractDungeon.getCurrRoom() instanceof EventRoom && AbstractDungeon.getCurrRoom().event instanceof NoteForYourself && (tmpCard = ((NoteForYourself)AbstractDungeon.getCurrRoom().event).saveCard) != null) {
            CardCrawlGame.playerPref.putString("NOTE_CARD", tmpCard.cardID);
            CardCrawlGame.playerPref.putInteger("NOTE_UPGRADE", tmpCard.timesUpgraded);
            CardCrawlGame.playerPref.flush();
        }
        if (RestRoom.lastFireSoundId != 0L) {
            CardCrawlGame.sound.fadeOut("REST_FIRE_WET", RestRoom.lastFireSoundId);
        }
        if (!AbstractDungeon.player.stance.ID.equals("Neutral") && AbstractDungeon.player.stance != null) {
            AbstractDungeon.player.stance.stopIdleSfx();
        }
        AbstractDungeon.gridSelectScreen.upgradePreviewCard = null;
        previousScreen = null;
        dynamicBanner.hide();
        dungeonMapScreen.closeInstantly();
        AbstractDungeon.closeCurrentScreen();
        topPanel.unhoverHitboxes();
        AbstractDungeon.fadeIn();
        player.resetControllerValues();
        effectList.clear();
        Iterator<AbstractGameEffect> i = topLevelEffects.iterator();
        while (i.hasNext()) {
            AbstractGameEffect e = i.next();
            if (e instanceof ObtainKeyEffect) continue;
            i.remove();
        }
        topLevelEffectsQueue.clear();
        effectsQueue.clear();
        AbstractDungeon.dungeonMapScreen.dismissable = true;
        AbstractDungeon.dungeonMapScreen.map.legend.isLegendHighlighted = false;
        AbstractDungeon.resetPlayer();
        if (!CardCrawlGame.loadingSave) {
            this.incrementFloorBasedMetrics();
            if (!TipTracker.tips.get("INTENT_TIP").booleanValue() && ++floorNum == 6) {
                TipTracker.neverShowAgain("INTENT_TIP");
            }
            StatsScreen.incrementFloorClimbed();
            SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
        }
        monsterHpRng = new Random(Settings.seed + (long)floorNum);
        aiRng = new Random(Settings.seed + (long)floorNum);
        shuffleRng = new Random(Settings.seed + (long)floorNum);
        cardRandomRng = new Random(Settings.seed + (long)floorNum);
        miscRng = new Random(Settings.seed + (long)floorNum);
        boolean isLoadingPostCombatSave = CardCrawlGame.loadingSave && saveFile != null && saveFile.post_combat;
        boolean isLoadingCompletedEvent = false;
        if (nextRoom != null && !isLoadingPostCombatSave) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onEnterRoom(AbstractDungeon.nextRoom.room);
            }
        }
        if (!AbstractDungeon.actionManager.actions.isEmpty()) {
            logger.info("[WARNING] Line:1904: Action Manager was NOT clear! Clearing");
            actionManager.clear();
        }
        if (nextRoom != null) {
            Object roomMetricKey = AbstractDungeon.nextRoom.room.getMapSymbol();
            if (AbstractDungeon.nextRoom.room instanceof EventRoom) {
                Random eventRngDuplicate = new Random(Settings.seed, AbstractDungeon.eventRng.counter);
                EventHelper.RoomResult roomResult = EventHelper.roll(eventRngDuplicate);
                boolean bl = isLoadingCompletedEvent = isLoadingPostCombatSave && roomResult == EventHelper.RoomResult.EVENT;
                if (!isLoadingCompletedEvent) {
                    eventRng = eventRngDuplicate;
                    AbstractDungeon.nextRoom.room = this.generateRoom(roomResult);
                }
                roomMetricKey = AbstractDungeon.nextRoom.room.getMapSymbol();
                if (AbstractDungeon.nextRoom.room instanceof MonsterRoom || AbstractDungeon.nextRoom.room instanceof MonsterRoomElite) {
                    AbstractDungeon.nextRoom.room.combatEvent = true;
                }
                AbstractDungeon.nextRoom.room.setMapSymbol("?");
                AbstractDungeon.nextRoom.room.setMapImg(ImageMaster.MAP_NODE_EVENT, ImageMaster.MAP_NODE_EVENT_OUTLINE);
            }
            if (!isLoadingPostCombatSave) {
                CardCrawlGame.metricData.path_per_floor.add((String)roomMetricKey);
            }
            AbstractDungeon.setCurrMapNode(nextRoom);
        }
        if (AbstractDungeon.getCurrRoom() != null && !isLoadingPostCombatSave) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.justEnteredRoom(AbstractDungeon.getCurrRoom());
            }
        }
        if (isLoadingCompletedEvent) {
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
            String eventKey = (String)saveFile.metric_event_choices.get(saveFile.metric_event_choices.size() - 1).get("event_name");
            ((EventRoom)AbstractDungeon.getCurrRoom()).event = EventHelper.getEvent(eventKey);
        } else {
            if (isAscensionMode) {
                CardCrawlGame.publisherIntegration.setRichPresenceDisplayPlaying(floorNum, ascensionLevel, player.getLocalizedCharacterName());
            } else {
                CardCrawlGame.publisherIntegration.setRichPresenceDisplayPlaying(floorNum, player.getLocalizedCharacterName());
            }
            AbstractDungeon.getCurrRoom().onPlayerEntry();
        }
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom && lastCombatMetricKey.equals("Shield and Spear")) {
            player.movePosition((float)Settings.WIDTH / 2.0f, floorY);
        } else {
            player.movePosition((float)Settings.WIDTH * 0.25f, floorY);
            AbstractDungeon.player.flipHorizontal = false;
        }
        if (AbstractDungeon.currMapNode.room instanceof MonsterRoom && !isLoadingPostCombatSave) {
            player.preBattlePrep();
        }
        scene.nextRoom(AbstractDungeon.currMapNode.room);
        rs = AbstractDungeon.currMapNode.room instanceof RestRoom ? RenderScene.CAMPFIRE : (AbstractDungeon.currMapNode.room.event instanceof AbstractImageEvent ? RenderScene.EVENT : RenderScene.NORMAL);
    }

    private void incrementFloorBasedMetrics() {
        if (floorNum != 0) {
            CardCrawlGame.metricData.current_hp_per_floor.add(AbstractDungeon.player.currentHealth);
            CardCrawlGame.metricData.max_hp_per_floor.add(AbstractDungeon.player.maxHealth);
            CardCrawlGame.metricData.gold_per_floor.add(AbstractDungeon.player.gold);
        }
    }

    private AbstractRoom generateRoom(EventHelper.RoomResult roomType) {
        logger.info("GENERATING ROOM: " + roomType.name());
        switch (roomType) {
            case ELITE: {
                return new MonsterRoomElite();
            }
            case MONSTER: {
                return new MonsterRoom();
            }
            case SHOP: {
                return new ShopRoom();
            }
            case TREASURE: {
                return new TreasureRoom();
            }
        }
        return new EventRoom();
    }

    public static MonsterGroup getMonsters() {
        return AbstractDungeon.getCurrRoom().monsters;
    }

    public MonsterGroup getMonsterForRoomCreation() {
        if (monsterList.isEmpty()) {
            this.generateStrongEnemies(12);
        }
        logger.info("MONSTER: " + monsterList.get(0));
        lastCombatMetricKey = monsterList.get(0);
        return MonsterHelper.getEncounter(monsterList.get(0));
    }

    public MonsterGroup getEliteMonsterForRoomCreation() {
        if (eliteMonsterList.isEmpty()) {
            this.generateElites(10);
        }
        logger.info("ELITE: " + eliteMonsterList.get(0));
        lastCombatMetricKey = eliteMonsterList.get(0);
        return MonsterHelper.getEncounter(eliteMonsterList.get(0));
    }

    public static AbstractEvent generateEvent(Random rng) {
        if (rng.random(1.0f) < shrineChance) {
            if (!shrineList.isEmpty() || !specialOneTimeEventList.isEmpty()) {
                return AbstractDungeon.getShrine(rng);
            }
            if (!eventList.isEmpty()) {
                return AbstractDungeon.getEvent(rng);
            }
            logger.info("No events or shrines left");
            return null;
        }
        AbstractEvent retVal = AbstractDungeon.getEvent(rng);
        if (retVal == null) {
            return AbstractDungeon.getShrine(rng);
        }
        return retVal;
    }

    public static AbstractEvent getShrine(Random rng) {
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.addAll(shrineList);
        Iterator<String> iterator = specialOneTimeEventList.iterator();
        block22: while (iterator.hasNext()) {
            String e;
            switch (e = iterator.next()) {
                case "Fountain of Cleansing": {
                    if (!player.isCursed()) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "Designer": {
                    if (!id.equals("TheCity") && !id.equals("TheBeyond") || AbstractDungeon.player.gold < 75) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "Duplicator": {
                    if (!id.equals("TheCity") && !id.equals("TheBeyond")) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "FaceTrader": {
                    if (!id.equals("TheCity") && !id.equals("Exordium")) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "Knowing Skull": {
                    if (!id.equals("TheCity") || AbstractDungeon.player.currentHealth <= 12) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "N'loth": {
                    if (!id.equals("TheCity") && !id.equals("TheCity") || AbstractDungeon.player.relics.size() < 2) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "The Joust": {
                    if (!id.equals("TheCity") || AbstractDungeon.player.gold < 50) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "The Woman in Blue": {
                    if (AbstractDungeon.player.gold < 50) continue block22;
                    tmp.add(e);
                    continue block22;
                }
                case "SecretPortal": {
                    if (!(CardCrawlGame.playtime >= 800.0f) || !id.equals("TheBeyond")) continue block22;
                    tmp.add(e);
                    continue block22;
                }
            }
            tmp.add(e);
        }
        String tmpKey = (String)tmp.get(rng.random(tmp.size() - 1));
        shrineList.remove(tmpKey);
        specialOneTimeEventList.remove(tmpKey);
        logger.info("Removed event: " + tmpKey + " from pool.");
        return EventHelper.getEvent(tmpKey);
    }

    public static AbstractEvent getEvent(Random rng) {
        ArrayList<String> tmp = new ArrayList<String>();
        Iterator<String> iterator = eventList.iterator();
        block16: while (iterator.hasNext()) {
            String e;
            switch (e = iterator.next()) {
                case "Dead Adventurer": {
                    if (floorNum <= 6) continue block16;
                    tmp.add(e);
                    continue block16;
                }
                case "Mushrooms": {
                    if (floorNum <= 6) continue block16;
                    tmp.add(e);
                    continue block16;
                }
                case "The Moai Head": {
                    if (!player.hasRelic("Golden Idol") && (float)AbstractDungeon.player.currentHealth / (float)AbstractDungeon.player.maxHealth > 0.5f) continue block16;
                    tmp.add(e);
                    continue block16;
                }
                case "The Cleric": {
                    if (AbstractDungeon.player.gold < 35) continue block16;
                    tmp.add(e);
                    continue block16;
                }
                case "Beggar": {
                    if (AbstractDungeon.player.gold < 75) continue block16;
                    tmp.add(e);
                    continue block16;
                }
                case "Colosseum": {
                    if (currMapNode == null || AbstractDungeon.currMapNode.y <= map.size() / 2) continue block16;
                    tmp.add(e);
                    continue block16;
                }
            }
            tmp.add(e);
        }
        if (tmp.isEmpty()) {
            return AbstractDungeon.getShrine(rng);
        }
        String tmpKey = (String)tmp.get(rng.random(tmp.size() - 1));
        eventList.remove(tmpKey);
        logger.info("Removed event: " + tmpKey + " from pool.");
        return EventHelper.getEvent(tmpKey);
    }

    public MonsterGroup getBoss() {
        lastCombatMetricKey = bossKey;
        AbstractDungeon.dungeonMapScreen.map.atBoss = true;
        return MonsterHelper.getEncounter(bossKey);
    }

    public void update() {
        AbstractGameEffect e;
        if (!CardCrawlGame.stopClock) {
            CardCrawlGame.playtime += Gdx.graphics.getDeltaTime();
        }
        if (CardCrawlGame.screenTimer > 0.0f) {
            InputHelper.justClickedLeft = false;
            CInputActionSet.select.unpress();
        }
        topPanel.update();
        dynamicBanner.update();
        this.updateFading();
        AbstractDungeon.currMapNode.room.updateObjects();
        if (isScreenUp) {
            AbstractDungeon.topGradientColor.a = MathHelper.fadeLerpSnap(AbstractDungeon.topGradientColor.a, 0.25f);
            AbstractDungeon.botGradientColor.a = MathHelper.fadeLerpSnap(AbstractDungeon.botGradientColor.a, 0.25f);
        } else {
            AbstractDungeon.topGradientColor.a = MathHelper.fadeLerpSnap(AbstractDungeon.topGradientColor.a, 0.1f);
            AbstractDungeon.botGradientColor.a = MathHelper.fadeLerpSnap(AbstractDungeon.botGradientColor.a, 0.1f);
        }
        switch (screen) {
            case NO_INTERACT: 
            case NONE: {
                dungeonMapScreen.update();
                AbstractDungeon.currMapNode.room.update();
                scene.update();
                AbstractDungeon.currMapNode.room.eventControllerInput();
                break;
            }
            case FTUE: {
                ftue.update();
                InputHelper.justClickedRight = false;
                InputHelper.justClickedLeft = false;
                AbstractDungeon.currMapNode.room.update();
                break;
            }
            case MASTER_DECK_VIEW: {
                deckViewScreen.update();
                break;
            }
            case GAME_DECK_VIEW: {
                gameDeckViewScreen.update();
                break;
            }
            case DISCARD_VIEW: {
                discardPileViewScreen.update();
                break;
            }
            case EXHAUST_VIEW: {
                exhaustPileViewScreen.update();
                break;
            }
            case SETTINGS: {
                settingsScreen.update();
                break;
            }
            case INPUT_SETTINGS: {
                inputSettingsScreen.update();
                break;
            }
            case MAP: {
                dungeonMapScreen.update();
                break;
            }
            case GRID: {
                gridSelectScreen.update();
                if (!PeekButton.isPeeking) break;
                AbstractDungeon.currMapNode.room.update();
                break;
            }
            case CARD_REWARD: {
                cardRewardScreen.update();
                if (!PeekButton.isPeeking) break;
                AbstractDungeon.currMapNode.room.update();
                break;
            }
            case COMBAT_REWARD: {
                combatRewardScreen.update();
                break;
            }
            case BOSS_REWARD: {
                bossRelicScreen.update();
                AbstractDungeon.currMapNode.room.update();
                break;
            }
            case HAND_SELECT: {
                handCardSelectScreen.update();
                AbstractDungeon.currMapNode.room.update();
                break;
            }
            case SHOP: {
                shopScreen.update();
                break;
            }
            case DEATH: {
                deathScreen.update();
                break;
            }
            case VICTORY: {
                victoryScreen.update();
                break;
            }
            case UNLOCK: {
                unlockScreen.update();
                break;
            }
            case NEOW_UNLOCK: {
                gUnlockScreen.update();
                break;
            }
            case CREDITS: {
                creditsScreen.update();
                break;
            }
            case DOOR_UNLOCK: {
                CardCrawlGame.mainMenuScreen.doorUnlockScreen.update();
                break;
            }
            default: {
                logger.info("ERROR: UNKNOWN SCREEN TO UPDATE: " + screen.name());
            }
        }
        turnPhaseEffectActive = false;
        Iterator<AbstractGameEffect> i = topLevelEffects.iterator();
        while (i.hasNext()) {
            e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
        i = effectList.iterator();
        while (i.hasNext()) {
            e = i.next();
            e.update();
            if (e instanceof PlayerTurnEffect) {
                turnPhaseEffectActive = true;
            }
            if (!e.isDone) continue;
            i.remove();
        }
        i = effectsQueue.iterator();
        while (i.hasNext()) {
            e = i.next();
            effectList.add(e);
            i.remove();
        }
        i = topLevelEffectsQueue.iterator();
        while (i.hasNext()) {
            e = i.next();
            topLevelEffects.add(e);
            i.remove();
        }
        overlayMenu.update();
    }

    public void render(SpriteBatch sb) {
        AbstractRoom room;
        switch (rs) {
            case NORMAL: {
                scene.renderCombatRoomBg(sb);
                break;
            }
            case CAMPFIRE: {
                scene.renderCampfireRoom(sb);
                this.renderLetterboxGradient(sb);
                break;
            }
            case EVENT: {
                scene.renderEventRoom(sb);
            }
        }
        for (AbstractGameEffect e : effectList) {
            if (!e.renderBehind) continue;
            e.render(sb);
        }
        AbstractDungeon.currMapNode.room.render(sb);
        if (rs == RenderScene.NORMAL) {
            scene.renderCombatRoomFg(sb);
        }
        if (rs != RenderScene.CAMPFIRE) {
            this.renderLetterboxGradient(sb);
        }
        if ((room = AbstractDungeon.getCurrRoom()) instanceof EventRoom || room instanceof NeowRoom || room instanceof VictoryRoom) {
            room.renderEventTexts(sb);
        }
        for (AbstractGameEffect e : effectList) {
            if (e.renderBehind) continue;
            e.render(sb);
        }
        overlayMenu.render(sb);
        overlayMenu.renderBlackScreen(sb);
        switch (screen) {
            case NONE: {
                dungeonMapScreen.render(sb);
                break;
            }
            case MASTER_DECK_VIEW: {
                deckViewScreen.render(sb);
                break;
            }
            case DISCARD_VIEW: {
                discardPileViewScreen.render(sb);
                break;
            }
            case GAME_DECK_VIEW: {
                gameDeckViewScreen.render(sb);
                break;
            }
            case EXHAUST_VIEW: {
                exhaustPileViewScreen.render(sb);
                break;
            }
            case SETTINGS: {
                settingsScreen.render(sb);
                break;
            }
            case INPUT_SETTINGS: {
                inputSettingsScreen.render(sb);
                break;
            }
            case MAP: {
                dungeonMapScreen.render(sb);
                break;
            }
            case GRID: {
                gridSelectScreen.render(sb);
                break;
            }
            case CARD_REWARD: {
                cardRewardScreen.render(sb);
                break;
            }
            case COMBAT_REWARD: {
                combatRewardScreen.render(sb);
                break;
            }
            case BOSS_REWARD: {
                bossRelicScreen.render(sb);
                break;
            }
            case HAND_SELECT: {
                handCardSelectScreen.render(sb);
                break;
            }
            case SHOP: {
                shopScreen.render(sb);
                break;
            }
            case DEATH: {
                deathScreen.render(sb);
                break;
            }
            case VICTORY: {
                victoryScreen.render(sb);
                break;
            }
            case UNLOCK: {
                unlockScreen.render(sb);
                break;
            }
            case DOOR_UNLOCK: {
                CardCrawlGame.mainMenuScreen.doorUnlockScreen.render(sb);
                break;
            }
            case NEOW_UNLOCK: {
                gUnlockScreen.render(sb);
                break;
            }
            case CREDITS: {
                creditsScreen.render(sb);
            }
            case FTUE: {
                break;
            }
            case NO_INTERACT: {
                break;
            }
            default: {
                logger.info("ERROR: UNKNOWN SCREEN TO RENDER: " + screen.name());
            }
        }
        if (screen != CurrentScreen.UNLOCK) {
            sb.setColor(topGradientColor);
            if (!Settings.hideTopBar) {
                sb.draw(ImageMaster.SCROLL_GRADIENT, 0.0f, (float)Settings.HEIGHT - 128.0f * Settings.scale, (float)Settings.WIDTH, 64.0f * Settings.scale);
            }
            sb.setColor(botGradientColor);
            if (!Settings.hideTopBar) {
                sb.draw(ImageMaster.SCROLL_GRADIENT, 0.0f, 64.0f * Settings.scale, (float)Settings.WIDTH, -64.0f * Settings.scale);
            }
        }
        if (screen == CurrentScreen.FTUE) {
            ftue.render(sb);
        }
        AbstractDungeon.overlayMenu.cancelButton.render(sb);
        dynamicBanner.render(sb);
        if (screen != CurrentScreen.UNLOCK) {
            topPanel.render(sb);
        }
        AbstractDungeon.currMapNode.room.renderAboveTopPanel(sb);
        for (AbstractGameEffect e : topLevelEffects) {
            if (e.renderBehind) continue;
            e.render(sb);
        }
        sb.setColor(fadeColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    private void renderLetterboxGradient(SpriteBatch sb) {
    }

    public void updateFading() {
        if (isFadingIn) {
            AbstractDungeon.fadeColor.a = Interpolation.fade.apply(0.0f, 1.0f, (fadeTimer -= Gdx.graphics.getDeltaTime()) / 0.8f);
            if (fadeTimer < 0.0f) {
                isFadingIn = false;
                AbstractDungeon.fadeColor.a = 0.0f;
                fadeTimer = 0.0f;
            }
        } else if (isFadingOut) {
            AbstractDungeon.fadeColor.a = Interpolation.fade.apply(1.0f, 0.0f, (fadeTimer -= Gdx.graphics.getDeltaTime()) / 0.8f);
            if (fadeTimer < 0.0f) {
                fadeTimer = 0.0f;
                isFadingOut = false;
                AbstractDungeon.fadeColor.a = 1.0f;
                if (!isDungeonBeaten) {
                    this.nextRoomTransition();
                }
            }
        }
    }

    public static void closeCurrentScreen() {
        PeekButton.isPeeking = false;
        if (previousScreen == screen) {
            previousScreen = null;
        }
        switch (screen) {
            case MASTER_DECK_VIEW: {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.genericScreenOverlayReset();
                for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                    c.unhover();
                    c.untip();
                }
                break;
            }
            case DISCARD_VIEW: {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.genericScreenOverlayReset();
                for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
                    c.drawScale = 0.12f;
                    c.targetDrawScale = 0.12f;
                    c.teleportToDiscardPile();
                    c.darken(true);
                    c.unhover();
                }
                break;
            }
            case FTUE: {
                AbstractDungeon.genericScreenOverlayReset();
                break;
            }
            case GAME_DECK_VIEW: {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.genericScreenOverlayReset();
                break;
            }
            case EXHAUST_VIEW: {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.genericScreenOverlayReset();
                break;
            }
            case SETTINGS: {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.genericScreenOverlayReset();
                AbstractDungeon.settingsScreen.abandonPopup.hide();
                AbstractDungeon.settingsScreen.exitPopup.hide();
                break;
            }
            case INPUT_SETTINGS: {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.genericScreenOverlayReset();
                AbstractDungeon.settingsScreen.abandonPopup.hide();
                AbstractDungeon.settingsScreen.exitPopup.hide();
                break;
            }
            case NEOW_UNLOCK: {
                AbstractDungeon.genericScreenOverlayReset();
                CardCrawlGame.sound.stop("UNLOCK_SCREEN", AbstractDungeon.gUnlockScreen.id);
                break;
            }
            case GRID: {
                AbstractDungeon.genericScreenOverlayReset();
                if (AbstractDungeon.combatRewardScreen.rewards.isEmpty()) break;
                previousScreen = CurrentScreen.COMBAT_REWARD;
                break;
            }
            case CARD_REWARD: {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                dynamicBanner.hide();
                AbstractDungeon.genericScreenOverlayReset();
                if (screenSwap) break;
                cardRewardScreen.onClose();
                break;
            }
            case COMBAT_REWARD: {
                dynamicBanner.hide();
                AbstractDungeon.genericScreenOverlayReset();
                break;
            }
            case BOSS_REWARD: {
                AbstractDungeon.genericScreenOverlayReset();
                dynamicBanner.hide();
                break;
            }
            case HAND_SELECT: {
                AbstractDungeon.genericScreenOverlayReset();
                overlayMenu.showCombatPanels();
                break;
            }
            case MAP: {
                AbstractDungeon.genericScreenOverlayReset();
                dungeonMapScreen.close();
                if (firstRoomChosen || nextRoom == null || AbstractDungeon.dungeonMapScreen.dismissable) break;
                firstRoomChosen = true;
                AbstractDungeon.firstRoomLogic();
                break;
            }
            case SHOP: {
                CardCrawlGame.sound.play("SHOP_CLOSE");
                AbstractDungeon.genericScreenOverlayReset();
                AbstractDungeon.overlayMenu.cancelButton.hide();
                break;
            }
            case TRANSFORM: {
                CardCrawlGame.sound.play("ATTACK_MAGIC_SLOW_1");
                AbstractDungeon.genericScreenOverlayReset();
                AbstractDungeon.overlayMenu.cancelButton.hide();
                break;
            }
            default: {
                logger.info("UNSPECIFIED CASE: " + screen.name());
            }
        }
        if (previousScreen == null) {
            screen = CurrentScreen.NONE;
        } else if (screenSwap) {
            screenSwap = false;
        } else {
            screen = previousScreen;
            previousScreen = null;
            if (AbstractDungeon.getCurrRoom().rewardTime) {
                previousScreen = CurrentScreen.COMBAT_REWARD;
            }
            isScreenUp = true;
            AbstractDungeon.openPreviousScreen(screen);
        }
    }

    private static void openPreviousScreen(CurrentScreen s) {
        switch (s) {
            case DEATH: {
                deathScreen.reopen();
                break;
            }
            case VICTORY: {
                victoryScreen.reopen();
                break;
            }
            case MASTER_DECK_VIEW: {
                deckViewScreen.open();
                break;
            }
            case CARD_REWARD: {
                cardRewardScreen.reopen();
                if (AbstractDungeon.cardRewardScreen.rItem == null) break;
                previousScreen = CurrentScreen.COMBAT_REWARD;
                break;
            }
            case DISCARD_VIEW: {
                discardPileViewScreen.reopen();
                break;
            }
            case EXHAUST_VIEW: {
                exhaustPileViewScreen.reopen();
                break;
            }
            case GAME_DECK_VIEW: {
                gameDeckViewScreen.reopen();
                break;
            }
            case HAND_SELECT: {
                overlayMenu.hideBlackScreen();
                handCardSelectScreen.reopen();
                break;
            }
            case COMBAT_REWARD: {
                combatRewardScreen.reopen();
                break;
            }
            case BOSS_REWARD: {
                bossRelicScreen.reopen();
                break;
            }
            case SHOP: {
                shopScreen.open();
                break;
            }
            case GRID: {
                overlayMenu.hideBlackScreen();
                if (AbstractDungeon.gridSelectScreen.isJustForConfirming) {
                    dynamicBanner.appear();
                }
                gridSelectScreen.reopen();
                break;
            }
            case NEOW_UNLOCK: {
                gUnlockScreen.reOpen();
                break;
            }
            case MAP: {
                if (AbstractDungeon.dungeonMapScreen.dismissable) {
                    AbstractDungeon.overlayMenu.cancelButton.show(DungeonMapScreen.TEXT[1]);
                    break;
                }
                AbstractDungeon.overlayMenu.cancelButton.hide();
                break;
            }
        }
    }

    private static void genericScreenOverlayReset() {
        if (previousScreen == null) {
            if (AbstractDungeon.player.isDead) {
                previousScreen = CurrentScreen.DEATH;
            } else {
                isScreenUp = false;
                overlayMenu.hideBlackScreen();
            }
        }
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !AbstractDungeon.player.isDead) {
            overlayMenu.showCombatPanels();
        }
    }

    public static void fadeIn() {
        if (AbstractDungeon.fadeColor.a != 1.0f) {
            logger.info("WARNING: Attempting to fade in even though screen is not black");
        }
        isFadingIn = true;
        fadeTimer = Settings.FAST_MODE ? 0.001f : 0.8f;
    }

    public static void fadeOut() {
        if (fadeTimer == 0.0f) {
            if (AbstractDungeon.fadeColor.a != 0.0f) {
                logger.info("WARNING: Attempting to fade out even though screen is not transparent");
            }
            isFadingOut = true;
            fadeTimer = Settings.FAST_MODE ? 0.001f : 0.8f;
        }
    }

    public static void dungeonTransitionSetup() {
        ++actNum;
        if (AbstractDungeon.cardRng.counter > 0 && AbstractDungeon.cardRng.counter < 250) {
            cardRng.setCounter(250);
        } else if (AbstractDungeon.cardRng.counter > 250 && AbstractDungeon.cardRng.counter < 500) {
            cardRng.setCounter(500);
        } else if (AbstractDungeon.cardRng.counter > 500 && AbstractDungeon.cardRng.counter < 750) {
            cardRng.setCounter(750);
        }
        logger.info("CardRng Counter: " + AbstractDungeon.cardRng.counter);
        topPanel.unhoverHitboxes();
        pathX.clear();
        pathY.clear();
        EventHelper.resetProbabilities();
        eventList.clear();
        shrineList.clear();
        monsterList.clear();
        eliteMonsterList.clear();
        bossList.clear();
        AbstractRoom.blizzardPotionMod = 0;
        if (ascensionLevel >= 5) {
            player.heal(MathUtils.round((float)(AbstractDungeon.player.maxHealth - AbstractDungeon.player.currentHealth) * 0.75f), false);
        } else {
            player.heal(AbstractDungeon.player.maxHealth, false);
        }
        if (floorNum > 1) {
            topPanel.panelHealEffect();
        }
        if (floorNum <= 1 && CardCrawlGame.dungeon instanceof Exordium) {
            if (ascensionLevel >= 14) {
                player.decreaseMaxHealth(player.getAscensionMaxHPLoss());
            }
            if (ascensionLevel >= 6) {
                AbstractDungeon.player.currentHealth = MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.9f);
            }
            if (ascensionLevel >= 10) {
                AbstractDungeon.player.masterDeck.addToTop(new AscendersBane());
                UnlockTracker.markCardAsSeen("AscendersBane");
            }
            CardCrawlGame.playtime = 0.0f;
        }
        AbstractDungeon.dungeonMapScreen.map.atBoss = false;
    }

    public static void reset() {
        logger.info("Resetting variables...");
        CardCrawlGame.resetScoreVars();
        ModHelper.setModsFalse();
        floorNum = 0;
        actNum = 0;
        if (currMapNode != null && AbstractDungeon.getCurrRoom() != null) {
            AbstractDungeon.getCurrRoom().dispose();
            if (AbstractDungeon.getCurrRoom().monsters != null) {
                for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    m.dispose();
                }
            }
        }
        currMapNode = null;
        shrineList.clear();
        relicsToRemoveOnStart.clear();
        previousScreen = null;
        actionManager.clear();
        actionManager.clearNextRoomCombatActions();
        combatRewardScreen.clear();
        cardRewardScreen.reset();
        if (dungeonMapScreen != null) {
            dungeonMapScreen.closeInstantly();
        }
        effectList.clear();
        effectsQueue.clear();
        topLevelEffectsQueue.clear();
        topLevelEffects.clear();
        cardBlizzRandomizer = cardBlizzStartOffset;
        if (player != null) {
            AbstractDungeon.player.relics.clear();
        }
        rs = RenderScene.NORMAL;
        blightPool.clear();
    }

    protected void removeRelicFromPool(ArrayList<String> pool, String name) {
        Iterator<String> i = pool.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (!s.equals(name)) continue;
            i.remove();
            logger.info("Relic" + s + " removed from relic pool.");
        }
    }

    public static void onModifyPower() {
        if (player != null) {
            AbstractDungeon.player.hand.applyPowers();
            if (player.hasPower("Focus")) {
                for (AbstractOrb o : AbstractDungeon.player.orbs) {
                    o.updateDescription();
                }
            }
        }
        if (AbstractDungeon.getCurrRoom().monsters != null) {
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                m.applyPowers();
            }
        }
    }

    public void checkForPactAchievement() {
        if (player != null && AbstractDungeon.player.exhaustPile.size() >= 20) {
            UnlockTracker.unlockAchievement("THE_PACT");
        }
    }

    public void loadSave(SaveFile saveFile) {
        floorNum = saveFile.floor_num;
        actNum = saveFile.act_num;
        Settings.seed = saveFile.seed;
        AbstractDungeon.loadSeeds(saveFile);
        monsterList = saveFile.monster_list;
        eliteMonsterList = saveFile.elite_monster_list;
        bossList = saveFile.boss_list;
        this.setBoss(saveFile.boss);
        commonRelicPool = saveFile.common_relics;
        uncommonRelicPool = saveFile.uncommon_relics;
        rareRelicPool = saveFile.rare_relics;
        shopRelicPool = saveFile.shop_relics;
        bossRelicPool = saveFile.boss_relics;
        pathX = saveFile.path_x;
        pathY = saveFile.path_y;
        bossCount = saveFile.spirit_count;
        eventList = saveFile.event_list;
        specialOneTimeEventList = saveFile.one_time_event_list;
        EventHelper.setChances(saveFile.event_chances);
        AbstractRoom.blizzardPotionMod = saveFile.potion_chance;
        ShopScreen.purgeCost = saveFile.purgeCost;
        CardHelper.obtainedCards = saveFile.obtained_cards;
        if (saveFile.daily_mods != null) {
            ModHelper.setMods(saveFile.daily_mods);
        }
    }

    public static AbstractBlight getBlight(String targetID) {
        for (AbstractBlight b : blightPool) {
            if (!b.blightID.equals(targetID)) continue;
            return b;
        }
        return null;
    }

    static {
        floorNum = 0;
        actNum = 0;
        unlocks = new ArrayList();
        shrineChance = 0.25f;
        loading_post_combat = false;
        is_victory = false;
        srcColorlessCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcCurseCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcCommonCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcUncommonCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        srcRareCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        colorlessCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        curseCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        commonCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        uncommonCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        rareCardPool = new CardGroup(CardGroup.CardGroupType.CARD_POOL);
        commonRelicPool = new ArrayList();
        uncommonRelicPool = new ArrayList();
        rareRelicPool = new ArrayList();
        shopRelicPool = new ArrayList();
        bossRelicPool = new ArrayList();
        lastCombatMetricKey = null;
        monsterList = new ArrayList();
        eliteMonsterList = new ArrayList();
        bossList = new ArrayList();
        eventList = new ArrayList();
        shrineList = new ArrayList();
        specialOneTimeEventList = new ArrayList();
        actionManager = new GameActionManager();
        topLevelEffects = new ArrayList();
        topLevelEffectsQueue = new ArrayList();
        effectList = new ArrayList();
        effectsQueue = new ArrayList();
        turnPhaseEffectActive = false;
        firstRoomChosen = false;
        rs = RenderScene.NORMAL;
        pathX = new ArrayList();
        pathY = new ArrayList();
        topGradientColor = new Color(1.0f, 1.0f, 1.0f, 0.25f);
        botGradientColor = new Color(1.0f, 1.0f, 1.0f, 0.25f);
        floorY = 340.0f * Settings.yScale;
        topPanel = new TopPanel();
        cardRewardScreen = new CardRewardScreen();
        combatRewardScreen = new CombatRewardScreen();
        bossRelicScreen = new BossRelicSelectScreen();
        deckViewScreen = new MasterDeckViewScreen();
        discardPileViewScreen = new DiscardPileViewScreen();
        gameDeckViewScreen = new DrawPileViewScreen();
        exhaustPileViewScreen = new ExhaustPileViewScreen();
        settingsScreen = new SettingsScreen();
        inputSettingsScreen = new InputSettingsScreen();
        dungeonMapScreen = new DungeonMapScreen();
        gridSelectScreen = new GridCardSelectScreen();
        handCardSelectScreen = new HandCardSelectScreen();
        shopScreen = new ShopScreen();
        creditsScreen = null;
        ftue = null;
        unlockScreen = new UnlockCharacterScreen();
        gUnlockScreen = new NeowUnlockScreen();
        isScreenUp = false;
        screenSwap = false;
        cardBlizzRandomizer = cardBlizzStartOffset = 5;
        cardBlizzGrowth = 1;
        cardBlizzMaxOffset = -40;
        sceneOffsetY = 0.0f;
        relicsToRemoveOnStart = new ArrayList();
        bossCount = 0;
        isAscensionMode = false;
        ascensionLevel = 0;
        blightPool = new ArrayList();
        LOGGER = LogManager.getLogger(AbstractDungeon.class.getName());
    }

    public static enum RenderScene {
        NORMAL,
        EVENT,
        CAMPFIRE;

    }

    public static enum CurrentScreen {
        NONE,
        MASTER_DECK_VIEW,
        SETTINGS,
        INPUT_SETTINGS,
        GRID,
        MAP,
        FTUE,
        CHOOSE_ONE,
        HAND_SELECT,
        SHOP,
        COMBAT_REWARD,
        DISCARD_VIEW,
        EXHAUST_VIEW,
        GAME_DECK_VIEW,
        BOSS_REWARD,
        DEATH,
        CARD_REWARD,
        TRANSFORM,
        VICTORY,
        UNLOCK,
        DOOR_UNLOCK,
        CREDITS,
        NO_INTERACT,
        NEOW_UNLOCK;

    }
}

