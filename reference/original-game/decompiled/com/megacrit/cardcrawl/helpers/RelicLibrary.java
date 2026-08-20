/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import com.megacrit.cardcrawl.relics.Abacus;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.Akabeko;
import com.megacrit.cardcrawl.relics.Anchor;
import com.megacrit.cardcrawl.relics.AncientTeaSet;
import com.megacrit.cardcrawl.relics.ArtOfWar;
import com.megacrit.cardcrawl.relics.Astrolabe;
import com.megacrit.cardcrawl.relics.BagOfMarbles;
import com.megacrit.cardcrawl.relics.BagOfPreparation;
import com.megacrit.cardcrawl.relics.BirdFacedUrn;
import com.megacrit.cardcrawl.relics.BlackBlood;
import com.megacrit.cardcrawl.relics.BlackStar;
import com.megacrit.cardcrawl.relics.BloodVial;
import com.megacrit.cardcrawl.relics.BloodyIdol;
import com.megacrit.cardcrawl.relics.BlueCandle;
import com.megacrit.cardcrawl.relics.Boot;
import com.megacrit.cardcrawl.relics.BottledFlame;
import com.megacrit.cardcrawl.relics.BottledLightning;
import com.megacrit.cardcrawl.relics.BottledTornado;
import com.megacrit.cardcrawl.relics.Brimstone;
import com.megacrit.cardcrawl.relics.BronzeScales;
import com.megacrit.cardcrawl.relics.BurningBlood;
import com.megacrit.cardcrawl.relics.BustedCrown;
import com.megacrit.cardcrawl.relics.Calipers;
import com.megacrit.cardcrawl.relics.CallingBell;
import com.megacrit.cardcrawl.relics.CaptainsWheel;
import com.megacrit.cardcrawl.relics.Cauldron;
import com.megacrit.cardcrawl.relics.CentennialPuzzle;
import com.megacrit.cardcrawl.relics.CeramicFish;
import com.megacrit.cardcrawl.relics.ChampionsBelt;
import com.megacrit.cardcrawl.relics.CharonsAshes;
import com.megacrit.cardcrawl.relics.ChemicalX;
import com.megacrit.cardcrawl.relics.Circlet;
import com.megacrit.cardcrawl.relics.CloakClasp;
import com.megacrit.cardcrawl.relics.ClockworkSouvenir;
import com.megacrit.cardcrawl.relics.CoffeeDripper;
import com.megacrit.cardcrawl.relics.Courier;
import com.megacrit.cardcrawl.relics.CrackedCore;
import com.megacrit.cardcrawl.relics.CultistMask;
import com.megacrit.cardcrawl.relics.CursedKey;
import com.megacrit.cardcrawl.relics.Damaru;
import com.megacrit.cardcrawl.relics.DarkstonePeriapt;
import com.megacrit.cardcrawl.relics.DataDisk;
import com.megacrit.cardcrawl.relics.DeadBranch;
import com.megacrit.cardcrawl.relics.DollysMirror;
import com.megacrit.cardcrawl.relics.DreamCatcher;
import com.megacrit.cardcrawl.relics.DuVuDoll;
import com.megacrit.cardcrawl.relics.Duality;
import com.megacrit.cardcrawl.relics.Ectoplasm;
import com.megacrit.cardcrawl.relics.EmotionChip;
import com.megacrit.cardcrawl.relics.EmptyCage;
import com.megacrit.cardcrawl.relics.Enchiridion;
import com.megacrit.cardcrawl.relics.EternalFeather;
import com.megacrit.cardcrawl.relics.FaceOfCleric;
import com.megacrit.cardcrawl.relics.FossilizedHelix;
import com.megacrit.cardcrawl.relics.FrozenCore;
import com.megacrit.cardcrawl.relics.FrozenEgg2;
import com.megacrit.cardcrawl.relics.FrozenEye;
import com.megacrit.cardcrawl.relics.FusionHammer;
import com.megacrit.cardcrawl.relics.GamblingChip;
import com.megacrit.cardcrawl.relics.Ginger;
import com.megacrit.cardcrawl.relics.Girya;
import com.megacrit.cardcrawl.relics.GoldPlatedCables;
import com.megacrit.cardcrawl.relics.GoldenEye;
import com.megacrit.cardcrawl.relics.GoldenIdol;
import com.megacrit.cardcrawl.relics.GremlinHorn;
import com.megacrit.cardcrawl.relics.GremlinMask;
import com.megacrit.cardcrawl.relics.HandDrill;
import com.megacrit.cardcrawl.relics.HappyFlower;
import com.megacrit.cardcrawl.relics.HolyWater;
import com.megacrit.cardcrawl.relics.HornCleat;
import com.megacrit.cardcrawl.relics.HoveringKite;
import com.megacrit.cardcrawl.relics.IceCream;
import com.megacrit.cardcrawl.relics.IncenseBurner;
import com.megacrit.cardcrawl.relics.InkBottle;
import com.megacrit.cardcrawl.relics.Inserter;
import com.megacrit.cardcrawl.relics.JuzuBracelet;
import com.megacrit.cardcrawl.relics.Kunai;
import com.megacrit.cardcrawl.relics.Lantern;
import com.megacrit.cardcrawl.relics.LetterOpener;
import com.megacrit.cardcrawl.relics.LizardTail;
import com.megacrit.cardcrawl.relics.MagicFlower;
import com.megacrit.cardcrawl.relics.Mango;
import com.megacrit.cardcrawl.relics.MarkOfPain;
import com.megacrit.cardcrawl.relics.MarkOfTheBloom;
import com.megacrit.cardcrawl.relics.Matryoshka;
import com.megacrit.cardcrawl.relics.MawBank;
import com.megacrit.cardcrawl.relics.MealTicket;
import com.megacrit.cardcrawl.relics.MeatOnTheBone;
import com.megacrit.cardcrawl.relics.MedicalKit;
import com.megacrit.cardcrawl.relics.Melange;
import com.megacrit.cardcrawl.relics.MembershipCard;
import com.megacrit.cardcrawl.relics.MercuryHourglass;
import com.megacrit.cardcrawl.relics.MoltenEgg2;
import com.megacrit.cardcrawl.relics.MummifiedHand;
import com.megacrit.cardcrawl.relics.MutagenicStrength;
import com.megacrit.cardcrawl.relics.Necronomicon;
import com.megacrit.cardcrawl.relics.NeowsLament;
import com.megacrit.cardcrawl.relics.NilrysCodex;
import com.megacrit.cardcrawl.relics.NinjaScroll;
import com.megacrit.cardcrawl.relics.NlothsGift;
import com.megacrit.cardcrawl.relics.NlothsMask;
import com.megacrit.cardcrawl.relics.NuclearBattery;
import com.megacrit.cardcrawl.relics.Nunchaku;
import com.megacrit.cardcrawl.relics.OddMushroom;
import com.megacrit.cardcrawl.relics.OddlySmoothStone;
import com.megacrit.cardcrawl.relics.OldCoin;
import com.megacrit.cardcrawl.relics.Omamori;
import com.megacrit.cardcrawl.relics.OrangePellets;
import com.megacrit.cardcrawl.relics.Orichalcum;
import com.megacrit.cardcrawl.relics.OrnamentalFan;
import com.megacrit.cardcrawl.relics.Orrery;
import com.megacrit.cardcrawl.relics.PandorasBox;
import com.megacrit.cardcrawl.relics.Pantograph;
import com.megacrit.cardcrawl.relics.PaperCrane;
import com.megacrit.cardcrawl.relics.PaperFrog;
import com.megacrit.cardcrawl.relics.PeacePipe;
import com.megacrit.cardcrawl.relics.Pear;
import com.megacrit.cardcrawl.relics.PenNib;
import com.megacrit.cardcrawl.relics.PhilosopherStone;
import com.megacrit.cardcrawl.relics.Pocketwatch;
import com.megacrit.cardcrawl.relics.PotionBelt;
import com.megacrit.cardcrawl.relics.PrayerWheel;
import com.megacrit.cardcrawl.relics.PreservedInsect;
import com.megacrit.cardcrawl.relics.PrismaticShard;
import com.megacrit.cardcrawl.relics.PureWater;
import com.megacrit.cardcrawl.relics.QuestionCard;
import com.megacrit.cardcrawl.relics.RedMask;
import com.megacrit.cardcrawl.relics.RedSkull;
import com.megacrit.cardcrawl.relics.RegalPillow;
import com.megacrit.cardcrawl.relics.RingOfTheSerpent;
import com.megacrit.cardcrawl.relics.RunicCapacitor;
import com.megacrit.cardcrawl.relics.RunicCube;
import com.megacrit.cardcrawl.relics.RunicDome;
import com.megacrit.cardcrawl.relics.RunicPyramid;
import com.megacrit.cardcrawl.relics.SacredBark;
import com.megacrit.cardcrawl.relics.SelfFormingClay;
import com.megacrit.cardcrawl.relics.Shovel;
import com.megacrit.cardcrawl.relics.Shuriken;
import com.megacrit.cardcrawl.relics.SingingBowl;
import com.megacrit.cardcrawl.relics.SlaversCollar;
import com.megacrit.cardcrawl.relics.Sling;
import com.megacrit.cardcrawl.relics.SmilingMask;
import com.megacrit.cardcrawl.relics.SnakeRing;
import com.megacrit.cardcrawl.relics.SneckoEye;
import com.megacrit.cardcrawl.relics.SneckoSkull;
import com.megacrit.cardcrawl.relics.Sozu;
import com.megacrit.cardcrawl.relics.SpiritPoop;
import com.megacrit.cardcrawl.relics.SsserpentHead;
import com.megacrit.cardcrawl.relics.StoneCalendar;
import com.megacrit.cardcrawl.relics.StrangeSpoon;
import com.megacrit.cardcrawl.relics.Strawberry;
import com.megacrit.cardcrawl.relics.StrikeDummy;
import com.megacrit.cardcrawl.relics.Sundial;
import com.megacrit.cardcrawl.relics.SymbioticVirus;
import com.megacrit.cardcrawl.relics.TeardropLocket;
import com.megacrit.cardcrawl.relics.TheSpecimen;
import com.megacrit.cardcrawl.relics.ThreadAndNeedle;
import com.megacrit.cardcrawl.relics.Tingsha;
import com.megacrit.cardcrawl.relics.TinyChest;
import com.megacrit.cardcrawl.relics.TinyHouse;
import com.megacrit.cardcrawl.relics.Toolbox;
import com.megacrit.cardcrawl.relics.Torii;
import com.megacrit.cardcrawl.relics.ToughBandages;
import com.megacrit.cardcrawl.relics.ToxicEgg2;
import com.megacrit.cardcrawl.relics.ToyOrnithopter;
import com.megacrit.cardcrawl.relics.TungstenRod;
import com.megacrit.cardcrawl.relics.Turnip;
import com.megacrit.cardcrawl.relics.TwistedFunnel;
import com.megacrit.cardcrawl.relics.UnceasingTop;
import com.megacrit.cardcrawl.relics.Vajra;
import com.megacrit.cardcrawl.relics.VelvetChoker;
import com.megacrit.cardcrawl.relics.VioletLotus;
import com.megacrit.cardcrawl.relics.Waffle;
import com.megacrit.cardcrawl.relics.WarPaint;
import com.megacrit.cardcrawl.relics.WarpedTongs;
import com.megacrit.cardcrawl.relics.Whetstone;
import com.megacrit.cardcrawl.relics.WhiteBeast;
import com.megacrit.cardcrawl.relics.WingBoots;
import com.megacrit.cardcrawl.relics.WristBlade;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RelicLibrary {
    private static final Logger logger = LogManager.getLogger(RelicLibrary.class.getName());
    public static int totalRelicCount = 0;
    public static int seenRelics = 0;
    private static HashMap<String, AbstractRelic> sharedRelics = new HashMap();
    private static HashMap<String, AbstractRelic> redRelics = new HashMap();
    private static HashMap<String, AbstractRelic> greenRelics = new HashMap();
    private static HashMap<String, AbstractRelic> blueRelics = new HashMap();
    private static HashMap<String, AbstractRelic> purpleRelics = new HashMap();
    public static ArrayList<AbstractRelic> starterList = new ArrayList();
    public static ArrayList<AbstractRelic> commonList = new ArrayList();
    public static ArrayList<AbstractRelic> uncommonList = new ArrayList();
    public static ArrayList<AbstractRelic> rareList = new ArrayList();
    public static ArrayList<AbstractRelic> bossList = new ArrayList();
    public static ArrayList<AbstractRelic> specialList = new ArrayList();
    public static ArrayList<AbstractRelic> shopList = new ArrayList();
    public static ArrayList<AbstractRelic> redList = new ArrayList();
    public static ArrayList<AbstractRelic> greenList = new ArrayList();
    public static ArrayList<AbstractRelic> blueList = new ArrayList();
    public static ArrayList<AbstractRelic> whiteList = new ArrayList();

    public static void initialize() {
        long startTime = System.currentTimeMillis();
        RelicLibrary.add(new Abacus());
        RelicLibrary.add(new Akabeko());
        RelicLibrary.add(new Anchor());
        RelicLibrary.add(new AncientTeaSet());
        RelicLibrary.add(new ArtOfWar());
        RelicLibrary.add(new Astrolabe());
        RelicLibrary.add(new BagOfMarbles());
        RelicLibrary.add(new BagOfPreparation());
        RelicLibrary.add(new BirdFacedUrn());
        RelicLibrary.add(new BlackStar());
        RelicLibrary.add(new BloodVial());
        RelicLibrary.add(new BloodyIdol());
        RelicLibrary.add(new BlueCandle());
        RelicLibrary.add(new Boot());
        RelicLibrary.add(new BottledFlame());
        RelicLibrary.add(new BottledLightning());
        RelicLibrary.add(new BottledTornado());
        RelicLibrary.add(new BronzeScales());
        RelicLibrary.add(new BustedCrown());
        RelicLibrary.add(new Calipers());
        RelicLibrary.add(new CallingBell());
        RelicLibrary.add(new CaptainsWheel());
        RelicLibrary.add(new Cauldron());
        RelicLibrary.add(new CentennialPuzzle());
        RelicLibrary.add(new CeramicFish());
        RelicLibrary.add(new ChemicalX());
        RelicLibrary.add(new ClockworkSouvenir());
        RelicLibrary.add(new CoffeeDripper());
        RelicLibrary.add(new Courier());
        RelicLibrary.add(new CultistMask());
        RelicLibrary.add(new CursedKey());
        RelicLibrary.add(new DarkstonePeriapt());
        RelicLibrary.add(new DeadBranch());
        RelicLibrary.add(new DollysMirror());
        RelicLibrary.add(new DreamCatcher());
        RelicLibrary.add(new DuVuDoll());
        RelicLibrary.add(new Ectoplasm());
        RelicLibrary.add(new EmptyCage());
        RelicLibrary.add(new Enchiridion());
        RelicLibrary.add(new EternalFeather());
        RelicLibrary.add(new FaceOfCleric());
        RelicLibrary.add(new FossilizedHelix());
        RelicLibrary.add(new FrozenEgg2());
        RelicLibrary.add(new FrozenEye());
        RelicLibrary.add(new FusionHammer());
        RelicLibrary.add(new GamblingChip());
        RelicLibrary.add(new Ginger());
        RelicLibrary.add(new Girya());
        RelicLibrary.add(new GoldenIdol());
        RelicLibrary.add(new GremlinHorn());
        RelicLibrary.add(new GremlinMask());
        RelicLibrary.add(new HandDrill());
        RelicLibrary.add(new HappyFlower());
        RelicLibrary.add(new HornCleat());
        RelicLibrary.add(new IceCream());
        RelicLibrary.add(new IncenseBurner());
        RelicLibrary.add(new InkBottle());
        RelicLibrary.add(new JuzuBracelet());
        RelicLibrary.add(new Kunai());
        RelicLibrary.add(new Lantern());
        RelicLibrary.add(new LetterOpener());
        RelicLibrary.add(new LizardTail());
        RelicLibrary.add(new Mango());
        RelicLibrary.add(new MarkOfTheBloom());
        RelicLibrary.add(new Matryoshka());
        RelicLibrary.add(new MawBank());
        RelicLibrary.add(new MealTicket());
        RelicLibrary.add(new MeatOnTheBone());
        RelicLibrary.add(new MedicalKit());
        RelicLibrary.add(new MembershipCard());
        RelicLibrary.add(new MercuryHourglass());
        RelicLibrary.add(new MoltenEgg2());
        RelicLibrary.add(new MummifiedHand());
        RelicLibrary.add(new MutagenicStrength());
        RelicLibrary.add(new Necronomicon());
        RelicLibrary.add(new NeowsLament());
        RelicLibrary.add(new NilrysCodex());
        RelicLibrary.add(new NlothsGift());
        RelicLibrary.add(new NlothsMask());
        RelicLibrary.add(new Nunchaku());
        RelicLibrary.add(new OddlySmoothStone());
        RelicLibrary.add(new OddMushroom());
        RelicLibrary.add(new OldCoin());
        RelicLibrary.add(new Omamori());
        RelicLibrary.add(new OrangePellets());
        RelicLibrary.add(new Orichalcum());
        RelicLibrary.add(new OrnamentalFan());
        RelicLibrary.add(new Orrery());
        RelicLibrary.add(new PandorasBox());
        RelicLibrary.add(new Pantograph());
        RelicLibrary.add(new PeacePipe());
        RelicLibrary.add(new Pear());
        RelicLibrary.add(new PenNib());
        RelicLibrary.add(new PhilosopherStone());
        RelicLibrary.add(new Pocketwatch());
        RelicLibrary.add(new PotionBelt());
        RelicLibrary.add(new PrayerWheel());
        RelicLibrary.add(new PreservedInsect());
        RelicLibrary.add(new PrismaticShard());
        RelicLibrary.add(new QuestionCard());
        RelicLibrary.add(new RedMask());
        RelicLibrary.add(new RegalPillow());
        RelicLibrary.add(new RunicDome());
        RelicLibrary.add(new RunicPyramid());
        RelicLibrary.add(new SacredBark());
        RelicLibrary.add(new Shovel());
        RelicLibrary.add(new Shuriken());
        RelicLibrary.add(new SingingBowl());
        RelicLibrary.add(new SlaversCollar());
        RelicLibrary.add(new Sling());
        RelicLibrary.add(new SmilingMask());
        RelicLibrary.add(new SneckoEye());
        RelicLibrary.add(new Sozu());
        RelicLibrary.add(new SpiritPoop());
        RelicLibrary.add(new SsserpentHead());
        RelicLibrary.add(new StoneCalendar());
        RelicLibrary.add(new StrangeSpoon());
        RelicLibrary.add(new Strawberry());
        RelicLibrary.add(new StrikeDummy());
        RelicLibrary.add(new Sundial());
        RelicLibrary.add(new ThreadAndNeedle());
        RelicLibrary.add(new TinyChest());
        RelicLibrary.add(new TinyHouse());
        RelicLibrary.add(new Toolbox());
        RelicLibrary.add(new Torii());
        RelicLibrary.add(new ToxicEgg2());
        RelicLibrary.add(new ToyOrnithopter());
        RelicLibrary.add(new TungstenRod());
        RelicLibrary.add(new Turnip());
        RelicLibrary.add(new UnceasingTop());
        RelicLibrary.add(new Vajra());
        RelicLibrary.add(new VelvetChoker());
        RelicLibrary.add(new Waffle());
        RelicLibrary.add(new WarPaint());
        RelicLibrary.add(new WarpedTongs());
        RelicLibrary.add(new Whetstone());
        RelicLibrary.add(new WhiteBeast());
        RelicLibrary.add(new WingBoots());
        RelicLibrary.addGreen(new HoveringKite());
        RelicLibrary.addGreen(new NinjaScroll());
        RelicLibrary.addGreen(new PaperCrane());
        RelicLibrary.addGreen(new RingOfTheSerpent());
        RelicLibrary.addGreen(new SnakeRing());
        RelicLibrary.addGreen(new SneckoSkull());
        RelicLibrary.addGreen(new TheSpecimen());
        RelicLibrary.addGreen(new Tingsha());
        RelicLibrary.addGreen(new ToughBandages());
        RelicLibrary.addGreen(new TwistedFunnel());
        RelicLibrary.addGreen(new WristBlade());
        RelicLibrary.addRed(new BlackBlood());
        RelicLibrary.addRed(new Brimstone());
        RelicLibrary.addRed(new BurningBlood());
        RelicLibrary.addRed(new ChampionsBelt());
        RelicLibrary.addRed(new CharonsAshes());
        RelicLibrary.addRed(new MagicFlower());
        RelicLibrary.addRed(new MarkOfPain());
        RelicLibrary.addRed(new PaperFrog());
        RelicLibrary.addRed(new RedSkull());
        RelicLibrary.addRed(new RunicCube());
        RelicLibrary.addRed(new SelfFormingClay());
        RelicLibrary.addBlue(new CrackedCore());
        RelicLibrary.addBlue(new DataDisk());
        RelicLibrary.addBlue(new EmotionChip());
        RelicLibrary.addBlue(new FrozenCore());
        RelicLibrary.addBlue(new GoldPlatedCables());
        RelicLibrary.addBlue(new Inserter());
        RelicLibrary.addBlue(new NuclearBattery());
        RelicLibrary.addBlue(new RunicCapacitor());
        RelicLibrary.addBlue(new SymbioticVirus());
        RelicLibrary.addPurple(new CloakClasp());
        RelicLibrary.addPurple(new Damaru());
        RelicLibrary.addPurple(new GoldenEye());
        RelicLibrary.addPurple(new HolyWater());
        RelicLibrary.addPurple(new Melange());
        RelicLibrary.addPurple(new PureWater());
        RelicLibrary.addPurple(new VioletLotus());
        RelicLibrary.addPurple(new TeardropLocket());
        RelicLibrary.addPurple(new Duality());
        if (Settings.isBeta) {
            // empty if block
        }
        logger.info("Relic load time: " + (System.currentTimeMillis() - startTime) + "ms");
        RelicLibrary.sortLists();
    }

    public static void resetForReload() {
        totalRelicCount = 0;
        seenRelics = 0;
        sharedRelics.clear();
        redRelics.clear();
        greenRelics.clear();
        blueRelics.clear();
        purpleRelics.clear();
        starterList.clear();
        commonList.clear();
        uncommonList.clear();
        rareList.clear();
        bossList.clear();
        specialList.clear();
        shopList.clear();
        redList.clear();
        greenList.clear();
        blueList.clear();
        whiteList.clear();
    }

    private static void sortLists() {
        Collections.sort(starterList);
        Collections.sort(commonList);
        Collections.sort(uncommonList);
        Collections.sort(rareList);
        Collections.sort(bossList);
        Collections.sort(specialList);
        Collections.sort(shopList);
        if (Settings.isDev) {
            logger.info(starterList);
            logger.info(commonList);
            logger.info(uncommonList);
            logger.info(rareList);
            logger.info(bossList);
        }
    }

    private static void printRelicsMissingLargeArt() {
        boolean common = false;
        boolean uncommon = false;
        boolean rare = false;
        boolean boss = false;
        boolean shop = false;
        boolean other = false;
        logger.info("[ART] START DISPLAYING RELICS WITH MISSING HIGH RES ART");
        for (Map.Entry<String, AbstractRelic> r : sharedRelics.entrySet()) {
            AbstractRelic relic = r.getValue();
            if (ImageMaster.loadImage("images/largeRelics/" + relic.imgUrl) != null) continue;
            logger.info(relic.name);
        }
    }

    private static void printRelicCount() {
        int common = 0;
        int uncommon = 0;
        int rare = 0;
        int boss = 0;
        int shop = 0;
        int other = 0;
        block7: for (Map.Entry<String, AbstractRelic> r : sharedRelics.entrySet()) {
            switch (r.getValue().tier) {
                case COMMON: {
                    ++common;
                    continue block7;
                }
                case UNCOMMON: {
                    ++uncommon;
                    continue block7;
                }
                case RARE: {
                    ++rare;
                    continue block7;
                }
                case BOSS: {
                    ++boss;
                    continue block7;
                }
                case SHOP: {
                    ++shop;
                    continue block7;
                }
            }
            ++other;
        }
        if (Settings.isDev) {
            logger.info("RELIC COUNTS");
            logger.info("Common: " + common);
            logger.info("Uncommon: " + uncommon);
            logger.info("Rare: " + rare);
            logger.info("Boss: " + boss);
            logger.info("Shop: " + shop);
            logger.info("Other: " + other);
            logger.info("Red: " + redRelics.size());
            logger.info("Green: " + greenRelics.size());
            logger.info("Blue: " + blueRelics.size());
            logger.info("Purple: " + purpleRelics.size());
        }
    }

    public static void add(AbstractRelic relic) {
        if (UnlockTracker.isRelicSeen(relic.relicId)) {
            ++seenRelics;
        }
        relic.isSeen = UnlockTracker.isRelicSeen(relic.relicId);
        sharedRelics.put(relic.relicId, relic);
        RelicLibrary.addToTierList(relic);
        ++totalRelicCount;
    }

    public static void addRed(AbstractRelic relic) {
        if (UnlockTracker.isRelicSeen(relic.relicId)) {
            ++seenRelics;
        }
        relic.isSeen = UnlockTracker.isRelicSeen(relic.relicId);
        redRelics.put(relic.relicId, relic);
        RelicLibrary.addToTierList(relic);
        redList.add(relic);
        ++totalRelicCount;
    }

    public static void addGreen(AbstractRelic relic) {
        if (UnlockTracker.isRelicSeen(relic.relicId)) {
            ++seenRelics;
        }
        relic.isSeen = UnlockTracker.isRelicSeen(relic.relicId);
        greenRelics.put(relic.relicId, relic);
        RelicLibrary.addToTierList(relic);
        greenList.add(relic);
        ++totalRelicCount;
    }

    public static void addBlue(AbstractRelic relic) {
        if (UnlockTracker.isRelicSeen(relic.relicId)) {
            ++seenRelics;
        }
        relic.isSeen = UnlockTracker.isRelicSeen(relic.relicId);
        blueRelics.put(relic.relicId, relic);
        RelicLibrary.addToTierList(relic);
        blueList.add(relic);
        ++totalRelicCount;
    }

    public static void addPurple(AbstractRelic relic) {
        if (UnlockTracker.isRelicSeen(relic.relicId)) {
            ++seenRelics;
        }
        relic.isSeen = UnlockTracker.isRelicSeen(relic.relicId);
        purpleRelics.put(relic.relicId, relic);
        RelicLibrary.addToTierList(relic);
        whiteList.add(relic);
        ++totalRelicCount;
    }

    public static void addToTierList(AbstractRelic relic) {
        switch (relic.tier) {
            case STARTER: {
                starterList.add(relic);
                break;
            }
            case COMMON: {
                commonList.add(relic);
                break;
            }
            case UNCOMMON: {
                uncommonList.add(relic);
                break;
            }
            case RARE: {
                rareList.add(relic);
                break;
            }
            case SHOP: {
                shopList.add(relic);
                break;
            }
            case SPECIAL: {
                specialList.add(relic);
                break;
            }
            case BOSS: {
                bossList.add(relic);
                break;
            }
            case DEPRECATED: {
                logger.info(relic.relicId + " is deprecated.");
                break;
            }
            default: {
                logger.info(relic.relicId + " is undefined tier.");
            }
        }
    }

    public static AbstractRelic getRelic(String key) {
        if (sharedRelics.containsKey(key)) {
            return sharedRelics.get(key);
        }
        if (redRelics.containsKey(key)) {
            return redRelics.get(key);
        }
        if (greenRelics.containsKey(key)) {
            return greenRelics.get(key);
        }
        if (blueRelics.containsKey(key)) {
            return blueRelics.get(key);
        }
        if (purpleRelics.containsKey(key)) {
            return purpleRelics.get(key);
        }
        return new Circlet();
    }

    public static boolean isARelic(String key) {
        return sharedRelics.containsKey(key) || redRelics.containsKey(key) || greenRelics.containsKey(key) || blueRelics.containsKey(key) || purpleRelics.containsKey(key);
    }

    public static void populateRelicPool(ArrayList<String> pool, AbstractRelic.RelicTier tier, AbstractPlayer.PlayerClass c) {
        for (Map.Entry<String, AbstractRelic> r : sharedRelics.entrySet()) {
            if (r.getValue().tier != tier || UnlockTracker.isRelicLocked(r.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
            pool.add(r.getKey());
        }
        switch (c) {
            case IRONCLAD: {
                for (Map.Entry<String, AbstractRelic> r : redRelics.entrySet()) {
                    if (r.getValue().tier != tier || UnlockTracker.isRelicLocked(r.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
                    pool.add(r.getKey());
                }
                break;
            }
            case THE_SILENT: {
                for (Map.Entry<String, AbstractRelic> r : greenRelics.entrySet()) {
                    if (r.getValue().tier != tier || UnlockTracker.isRelicLocked(r.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
                    pool.add(r.getKey());
                }
                break;
            }
            case DEFECT: {
                for (Map.Entry<String, AbstractRelic> r : blueRelics.entrySet()) {
                    if (r.getValue().tier != tier || UnlockTracker.isRelicLocked(r.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
                    pool.add(r.getKey());
                }
                break;
            }
            case WATCHER: {
                for (Map.Entry<String, AbstractRelic> r : purpleRelics.entrySet()) {
                    if (r.getValue().tier != tier || UnlockTracker.isRelicLocked(r.getKey()) && !Settings.treatEverythingAsUnlocked()) continue;
                    pool.add(r.getKey());
                }
                break;
            }
        }
    }

    public static void addSharedRelics(ArrayList<AbstractRelic> relicPool) {
        if (Settings.isDev) {
            logger.info("[RELIC] Adding " + sharedRelics.size() + " shared relics...");
        }
        for (Map.Entry<String, AbstractRelic> r : sharedRelics.entrySet()) {
            relicPool.add(r.getValue());
        }
    }

    public static void addClassSpecificRelics(ArrayList<AbstractRelic> relicPool) {
        switch (AbstractDungeon.player.chosenClass) {
            case IRONCLAD: {
                if (Settings.isDev) {
                    logger.info("[RELIC] Adding " + redRelics.size() + " red relics...");
                }
                for (Map.Entry<String, AbstractRelic> r : redRelics.entrySet()) {
                    relicPool.add(r.getValue());
                }
                break;
            }
            case THE_SILENT: {
                if (Settings.isDev) {
                    logger.info("[RELIC] Adding " + greenRelics.size() + " green relics...");
                }
                for (Map.Entry<String, AbstractRelic> r : greenRelics.entrySet()) {
                    relicPool.add(r.getValue());
                }
                break;
            }
            case DEFECT: {
                if (Settings.isDev) {
                    logger.info("[RELIC] Adding " + blueRelics.size() + " blue relics...");
                }
                for (Map.Entry<String, AbstractRelic> r : blueRelics.entrySet()) {
                    relicPool.add(r.getValue());
                }
                break;
            }
            case WATCHER: {
                if (Settings.isDev) {
                    logger.info("[RELIC] Adding " + purpleRelics.size() + " purple relics...");
                }
                for (Map.Entry<String, AbstractRelic> r : purpleRelics.entrySet()) {
                    relicPool.add(r.getValue());
                }
                break;
            }
        }
    }

    public static void uploadRelicData() {
        ArrayList<String> data = new ArrayList<String>();
        for (Map.Entry<String, AbstractRelic> r : sharedRelics.entrySet()) {
            data.add(r.getValue().gameDataUploadData("All"));
        }
        for (Map.Entry<String, AbstractRelic> r : redRelics.entrySet()) {
            data.add(r.getValue().gameDataUploadData("Red"));
        }
        for (Map.Entry<String, AbstractRelic> r : greenRelics.entrySet()) {
            data.add(r.getValue().gameDataUploadData("Green"));
        }
        for (Map.Entry<String, AbstractRelic> r : blueRelics.entrySet()) {
            data.add(r.getValue().gameDataUploadData("Blue"));
        }
        for (Map.Entry<String, AbstractRelic> r : purpleRelics.entrySet()) {
            data.add(r.getValue().gameDataUploadData("Purple"));
        }
        BotDataUploader.uploadDataAsync(BotDataUploader.GameDataType.RELIC_DATA, AbstractRelic.gameDataUploadHeader(), data);
    }

    public static ArrayList<AbstractRelic> sortByName(ArrayList<AbstractRelic> group, boolean ascending) {
        ArrayList<AbstractRelic> tmp = new ArrayList<AbstractRelic>();
        for (AbstractRelic r : group) {
            int addIndex = 0;
            for (AbstractRelic r2 : tmp) {
                if (!ascending ? r.name.compareTo(r2.name) < 0 : r.name.compareTo(r2.name) > 0) break;
                ++addIndex;
            }
            tmp.add(addIndex, r);
        }
        return tmp;
    }

    public static ArrayList<AbstractRelic> sortByStatus(ArrayList<AbstractRelic> group, boolean ascending) {
        ArrayList<AbstractRelic> tmp = new ArrayList<AbstractRelic>();
        for (AbstractRelic r : group) {
            int addIndex = 0;
            for (AbstractRelic r2 : tmp) {
                String b;
                String a;
                if (!ascending ? (a = UnlockTracker.isRelicLocked(r.relicId) ? "LOCKED" : (UnlockTracker.isRelicSeen(r.relicId) ? "UNSEEN" : "SEEN")).compareTo(b = UnlockTracker.isRelicLocked(r2.relicId) ? "LOCKED" : (UnlockTracker.isRelicSeen(r2.relicId) ? "UNSEEN" : "SEEN")) > 0 : (a = UnlockTracker.isRelicLocked(r.relicId) ? "LOCKED" : (UnlockTracker.isRelicSeen(r.relicId) ? "UNSEEN" : "SEEN")).compareTo(b = UnlockTracker.isRelicLocked(r2.relicId) ? "LOCKED" : (UnlockTracker.isRelicSeen(r2.relicId) ? "UNSEEN" : "SEEN")) < 0) break;
                ++addIndex;
            }
            tmp.add(addIndex, r);
        }
        return tmp;
    }

    public static void unlockAndSeeAllRelics() {
        for (String string : UnlockTracker.lockedRelics) {
            UnlockTracker.hardUnlockOverride(string);
        }
        for (Map.Entry entry : sharedRelics.entrySet()) {
            UnlockTracker.markRelicAsSeen((String)entry.getKey());
        }
        for (Map.Entry entry : redRelics.entrySet()) {
            UnlockTracker.markRelicAsSeen((String)entry.getKey());
        }
        for (Map.Entry entry : greenRelics.entrySet()) {
            UnlockTracker.markRelicAsSeen((String)entry.getKey());
        }
        for (Map.Entry entry : blueRelics.entrySet()) {
            UnlockTracker.markRelicAsSeen((String)entry.getKey());
        }
        for (Map.Entry entry : purpleRelics.entrySet()) {
            UnlockTracker.markRelicAsSeen((String)entry.getKey());
        }
    }
}

