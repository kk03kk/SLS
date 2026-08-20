/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.beyond.MysteriousSphere;
import com.megacrit.cardcrawl.helpers.EnemyData;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.beyond.Darkling;
import com.megacrit.cardcrawl.monsters.beyond.Deca;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.monsters.beyond.Exploder;
import com.megacrit.cardcrawl.monsters.beyond.GiantHead;
import com.megacrit.cardcrawl.monsters.beyond.Maw;
import com.megacrit.cardcrawl.monsters.beyond.Nemesis;
import com.megacrit.cardcrawl.monsters.beyond.OrbWalker;
import com.megacrit.cardcrawl.monsters.beyond.Reptomancer;
import com.megacrit.cardcrawl.monsters.beyond.Repulsor;
import com.megacrit.cardcrawl.monsters.beyond.SnakeDagger;
import com.megacrit.cardcrawl.monsters.beyond.Spiker;
import com.megacrit.cardcrawl.monsters.beyond.SpireGrowth;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.beyond.Transient;
import com.megacrit.cardcrawl.monsters.beyond.WrithingMass;
import com.megacrit.cardcrawl.monsters.city.BanditBear;
import com.megacrit.cardcrawl.monsters.city.BanditLeader;
import com.megacrit.cardcrawl.monsters.city.BanditPointy;
import com.megacrit.cardcrawl.monsters.city.BookOfStabbing;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import com.megacrit.cardcrawl.monsters.city.Centurion;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.Chosen;
import com.megacrit.cardcrawl.monsters.city.GremlinLeader;
import com.megacrit.cardcrawl.monsters.city.Healer;
import com.megacrit.cardcrawl.monsters.city.Mugger;
import com.megacrit.cardcrawl.monsters.city.ShelledParasite;
import com.megacrit.cardcrawl.monsters.city.SnakePlant;
import com.megacrit.cardcrawl.monsters.city.Snecko;
import com.megacrit.cardcrawl.monsters.city.SphericGuardian;
import com.megacrit.cardcrawl.monsters.city.Taskmaster;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.monsters.ending.SpireShield;
import com.megacrit.cardcrawl.monsters.ending.SpireSpear;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_S;
import com.megacrit.cardcrawl.monsters.exordium.ApologySlime;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import com.megacrit.cardcrawl.monsters.exordium.FungiBeast;
import com.megacrit.cardcrawl.monsters.exordium.GremlinFat;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.monsters.exordium.GremlinThief;
import com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWizard;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.JawWorm;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.monsters.exordium.Looter;
import com.megacrit.cardcrawl.monsters.exordium.LouseDefensive;
import com.megacrit.cardcrawl.monsters.exordium.LouseNormal;
import com.megacrit.cardcrawl.monsters.exordium.Sentry;
import com.megacrit.cardcrawl.monsters.exordium.SlaverBlue;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_M;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_S;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonsterHelper {
    private static final Logger logger = LogManager.getLogger(MonsterHelper.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("RunHistoryMonsterNames");
    public static final String[] MIXED_COMBAT_NAMES = MonsterHelper.uiStrings.TEXT;
    public static final String BLUE_SLAVER_ENC = "Blue Slaver";
    public static final String CULTIST_ENC = "Cultist";
    public static final String JAW_WORM_ENC = "Jaw Worm";
    public static final String LOOTER_ENC = "Looter";
    public static final String TWO_LOUSE_ENC = "2 Louse";
    public static final String SMALL_SLIMES_ENC = "Small Slimes";
    public static final String GREMLIN_GANG_ENC = "Gremlin Gang";
    public static final String RED_SLAVER_ENC = "Red Slaver";
    public static final String LARGE_SLIME_ENC = "Large Slime";
    public static final String LVL_1_THUGS_ENC = "Exordium Thugs";
    public static final String LVL_1_WILDLIFE_ENC = "Exordium Wildlife";
    public static final String THREE_LOUSE_ENC = "3 Louse";
    public static final String TWO_FUNGI_ENC = "2 Fungi Beasts";
    public static final String LOTS_OF_SLIMES_ENC = "Lots of Slimes";
    public static final String GREMLIN_NOB_ENC = "Gremlin Nob";
    public static final String LAGAVULIN_ENC = "Lagavulin";
    public static final String THREE_SENTRY_ENC = "3 Sentries";
    public static final String LAGAVULIN_EVENT_ENC = "Lagavulin Event";
    public static final String MUSHROOMS_EVENT_ENC = "The Mushroom Lair";
    public static final String GUARDIAN_ENC = "The Guardian";
    public static final String HEXAGHOST_ENC = "Hexaghost";
    public static final String SLIME_BOSS_ENC = "Slime Boss";
    public static final String TWO_THIEVES_ENC = "2 Thieves";
    public static final String THREE_BYRDS_ENC = "3 Byrds";
    public static final String CHOSEN_ENC = "Chosen";
    public static final String SHELL_PARASITE_ENC = "Shell Parasite";
    public static final String SPHERE_GUARDIAN_ENC = "Spheric Guardian";
    public static final String CULTIST_CHOSEN_ENC = "Cultist and Chosen";
    public static final String THREE_CULTISTS_ENC = "3 Cultists";
    public static final String FOUR_BYRDS_ENC = "4 Byrds";
    public static final String CHOSEN_FLOCK_ENC = "Chosen and Byrds";
    public static final String SENTRY_SPHERE_ENC = "Sentry and Sphere";
    public static final String SNAKE_PLANT_ENC = "Snake Plant";
    public static final String SNECKO_ENC = "Snecko";
    public static final String TANK_HEALER_ENC = "Centurion and Healer";
    public static final String PARASITE_AND_FUNGUS = "Shelled Parasite and Fungi";
    public static final String STAB_BOOK_ENC = "Book of Stabbing";
    public static final String GREMLIN_LEADER_ENC = "Gremlin Leader";
    public static final String SLAVERS_ENC = "Slavers";
    public static final String MASKED_BANDITS_ENC = "Masked Bandits";
    public static final String COLOSSEUM_SLAVER_ENC = "Colosseum Slavers";
    public static final String COLOSSEUM_NOB_ENC = "Colosseum Nobs";
    public static final String AUTOMATON_ENC = "Automaton";
    public static final String CHAMP_ENC = "Champ";
    public static final String COLLECTOR_ENC = "Collector";
    public static final String THREE_DARKLINGS_ENC = "3 Darklings";
    public static final String THREE_SHAPES_ENC = "3 Shapes";
    public static final String ORB_WALKER_ENC = "Orb Walker";
    public static final String TRANSIENT_ENC = "Transient";
    public static final String REPTOMANCER_ENC = "Reptomancer";
    public static final String SPIRE_GROWTH_ENC = "Spire Growth";
    public static final String MAW_ENC = "Maw";
    public static final String FOUR_SHAPES_ENC = "4 Shapes";
    public static final String SPHERE_TWO_SHAPES_ENC = "Sphere and 2 Shapes";
    public static final String JAW_WORMS_HORDE = "Jaw Worm Horde";
    public static final String SNECKO_WITH_MYSTICS = "Snecko and Mystics";
    public static final String WRITHING_MASS_ENC = "Writhing Mass";
    public static final String TWO_ORB_WALKER_ENC = "2 Orb Walkers";
    public static final String NEMESIS_ENC = "Nemesis";
    public static final String GIANT_HEAD_ENC = "Giant Head";
    public static final String MYSTERIOUS_SPHERE_ENC = "Mysterious Sphere";
    public static final String MIND_BLOOM_BOSS = "Mind Bloom Boss Battle";
    public static final String TIME_EATER_ENC = "Time Eater";
    public static final String AWAKENED_ENC = "Awakened One";
    public static final String DONU_DECA_ENC = "Donu and Deca";
    public static final String THE_HEART_ENC = "The Heart";
    public static final String SHIELD_SPEAR_ENC = "Shield and Spear";
    public static final String EYES_ENC = "The Eyes";
    public static final String APOLOGY_SLIME_ENC = "Apologetic Slime";
    public static final String OLD_REPTO_ONE_ENC = "Flame Bruiser 1 Orb";
    public static final String OLD_REPTO_TWO_ENC = "Flame Bruiser 2 Orb";
    public static final String OLD_SLAVER_PARASITE = "Slaver and Parasite";
    public static final String OLD_SNECKO_MYSTICS = "Snecko and Mystics";

    public static String getEncounterName(String key) {
        if (key == null) {
            return "";
        }
        switch (key) {
            case "Flame Bruiser 1 Orb": 
            case "Flame Bruiser 2 Orb": {
                return MIXED_COMBAT_NAMES[25];
            }
            case "Slaver and Parasite": {
                return MIXED_COMBAT_NAMES[26];
            }
            case "Snecko and Mystics": {
                return MIXED_COMBAT_NAMES[27];
            }
        }
        switch (key) {
            case "Blue Slaver": {
                return SlaverBlue.NAME;
            }
            case "Cultist": {
                return Cultist.NAME;
            }
            case "Jaw Worm": {
                return JawWorm.NAME;
            }
            case "Looter": {
                return Looter.NAME;
            }
            case "Gremlin Gang": {
                return MIXED_COMBAT_NAMES[0];
            }
            case "Red Slaver": {
                return SlaverRed.NAME;
            }
            case "Large Slime": {
                return MIXED_COMBAT_NAMES[1];
            }
            case "Exordium Thugs": {
                return MIXED_COMBAT_NAMES[2];
            }
            case "Exordium Wildlife": {
                return MIXED_COMBAT_NAMES[3];
            }
            case "3 Louse": {
                return LouseNormal.NAME;
            }
            case "2 Louse": {
                return LouseNormal.NAME;
            }
            case "2 Fungi Beasts": {
                return FungiBeast.NAME;
            }
            case "Lots of Slimes": {
                return MIXED_COMBAT_NAMES[4];
            }
            case "Small Slimes": {
                return MIXED_COMBAT_NAMES[5];
            }
            case "Gremlin Nob": {
                return GremlinNob.NAME;
            }
            case "Lagavulin": {
                return Lagavulin.NAME;
            }
            case "3 Sentries": {
                return MIXED_COMBAT_NAMES[23];
            }
            case "Lagavulin Event": {
                return Lagavulin.NAME;
            }
            case "The Mushroom Lair": {
                return FungiBeast.NAME;
            }
            case "The Guardian": {
                return TheGuardian.NAME;
            }
            case "Hexaghost": {
                return Hexaghost.NAME;
            }
            case "Slime Boss": {
                return SlimeBoss.NAME;
            }
        }
        switch (key) {
            case "2 Thieves": {
                return MIXED_COMBAT_NAMES[6];
            }
            case "3 Byrds": {
                return MIXED_COMBAT_NAMES[7];
            }
            case "4 Byrds": {
                return MIXED_COMBAT_NAMES[8];
            }
            case "Chosen": {
                return Chosen.NAME;
            }
            case "Shell Parasite": {
                return ShelledParasite.NAME;
            }
            case "Spheric Guardian": {
                return SphericGuardian.NAME;
            }
            case "Cultist and Chosen": {
                return MIXED_COMBAT_NAMES[24];
            }
            case "3 Cultists": {
                return MIXED_COMBAT_NAMES[9];
            }
            case "Chosen and Byrds": {
                return MIXED_COMBAT_NAMES[10];
            }
            case "Sentry and Sphere": {
                return MIXED_COMBAT_NAMES[11];
            }
            case "Snake Plant": {
                return SnakePlant.NAME;
            }
            case "Snecko": {
                return Snecko.NAME;
            }
            case "Centurion and Healer": {
                return MIXED_COMBAT_NAMES[12];
            }
            case "Shelled Parasite and Fungi": {
                return MIXED_COMBAT_NAMES[13];
            }
            case "Book of Stabbing": {
                return BookOfStabbing.NAME;
            }
            case "Gremlin Leader": {
                return GremlinLeader.NAME;
            }
            case "Slavers": {
                return Taskmaster.NAME;
            }
            case "Masked Bandits": {
                return MIXED_COMBAT_NAMES[14];
            }
            case "Colosseum Nobs": {
                return MIXED_COMBAT_NAMES[15];
            }
            case "Colosseum Slavers": {
                return MIXED_COMBAT_NAMES[16];
            }
            case "Automaton": {
                return BronzeAutomaton.NAME;
            }
            case "Champ": {
                return Champ.NAME;
            }
            case "Collector": {
                return TheCollector.NAME;
            }
        }
        switch (key) {
            case "Reptomancer": {
                return Reptomancer.NAME;
            }
            case "Transient": {
                return Transient.NAME;
            }
            case "3 Darklings": {
                return Darkling.NAME;
            }
            case "3 Shapes": {
                return MIXED_COMBAT_NAMES[17];
            }
            case "Jaw Worm Horde": {
                return MIXED_COMBAT_NAMES[18];
            }
            case "Orb Walker": {
                return OrbWalker.NAME;
            }
            case "Spire Growth": {
                return SpireGrowth.NAME;
            }
            case "Maw": {
                return Maw.NAME;
            }
            case "4 Shapes": {
                return MIXED_COMBAT_NAMES[19];
            }
            case "Sphere and 2 Shapes": {
                return MIXED_COMBAT_NAMES[20];
            }
            case "2 Orb Walkers": {
                return MIXED_COMBAT_NAMES[21];
            }
            case "Nemesis": {
                return Nemesis.NAME;
            }
            case "Writhing Mass": {
                return WrithingMass.NAME;
            }
            case "Giant Head": {
                return GiantHead.NAME;
            }
            case "Mysterious Sphere": {
                return MysteriousSphere.NAME;
            }
            case "Time Eater": {
                return TimeEater.NAME;
            }
            case "Awakened One": {
                return AwakenedOne.NAME;
            }
            case "Donu and Deca": {
                return MIXED_COMBAT_NAMES[22];
            }
        }
        switch (key) {
            case "The Heart": {
                return CorruptHeart.NAME;
            }
            case "Shield and Spear": {
                return MIXED_COMBAT_NAMES[28];
            }
        }
        return "";
    }

    public static MonsterGroup getEncounter(String key) {
        switch (key) {
            case "Blue Slaver": {
                return new MonsterGroup(new SlaverBlue(0.0f, 0.0f));
            }
            case "Cultist": {
                return new MonsterGroup(new Cultist(0.0f, -10.0f));
            }
            case "Jaw Worm": {
                return new MonsterGroup(new JawWorm(0.0f, 25.0f));
            }
            case "Looter": {
                return new MonsterGroup(new Looter(0.0f, 0.0f));
            }
            case "Gremlin Gang": {
                return MonsterHelper.spawnGremlins();
            }
            case "Red Slaver": {
                return new MonsterGroup(new SlaverRed(0.0f, 0.0f));
            }
            case "Large Slime": {
                if (AbstractDungeon.miscRng.randomBoolean()) {
                    return new MonsterGroup(new AcidSlime_L(0.0f, 0.0f));
                }
                return new MonsterGroup(new SpikeSlime_L(0.0f, 0.0f));
            }
            case "Exordium Thugs": {
                return MonsterHelper.bottomHumanoid();
            }
            case "Exordium Wildlife": {
                return MonsterHelper.bottomWildlife();
            }
            case "3 Louse": {
                return new MonsterGroup(new AbstractMonster[]{MonsterHelper.getLouse(-350.0f, 25.0f), MonsterHelper.getLouse(-125.0f, 10.0f), MonsterHelper.getLouse(80.0f, 30.0f)});
            }
            case "2 Louse": {
                return new MonsterGroup(new AbstractMonster[]{MonsterHelper.getLouse(-200.0f, 10.0f), MonsterHelper.getLouse(80.0f, 30.0f)});
            }
            case "2 Fungi Beasts": {
                return new MonsterGroup(new AbstractMonster[]{new FungiBeast(-400.0f, 30.0f), new FungiBeast(-40.0f, 20.0f)});
            }
            case "Lots of Slimes": {
                return MonsterHelper.spawnManySmallSlimes();
            }
            case "Small Slimes": {
                return MonsterHelper.spawnSmallSlimes();
            }
            case "Gremlin Nob": {
                return new MonsterGroup(new GremlinNob(0.0f, 0.0f));
            }
            case "Lagavulin": {
                return new MonsterGroup(new Lagavulin(true));
            }
            case "3 Sentries": {
                return new MonsterGroup(new AbstractMonster[]{new Sentry(-330.0f, 25.0f), new Sentry(-85.0f, 10.0f), new Sentry(140.0f, 30.0f)});
            }
            case "Lagavulin Event": {
                return new MonsterGroup(new Lagavulin(false));
            }
            case "The Mushroom Lair": {
                return new MonsterGroup(new AbstractMonster[]{new FungiBeast(-450.0f, 30.0f), new FungiBeast(-145.0f, 20.0f), new FungiBeast(180.0f, 15.0f)});
            }
            case "The Guardian": {
                return new MonsterGroup(new TheGuardian());
            }
            case "Hexaghost": {
                return new MonsterGroup(new Hexaghost());
            }
            case "Slime Boss": {
                return new MonsterGroup(new SlimeBoss());
            }
        }
        switch (key) {
            case "2 Thieves": {
                return new MonsterGroup(new AbstractMonster[]{new Looter(-200.0f, 15.0f), new Mugger(80.0f, 0.0f)});
            }
            case "3 Byrds": {
                return new MonsterGroup(new AbstractMonster[]{new Byrd(-360.0f, MathUtils.random(25.0f, 70.0f)), new Byrd(-80.0f, MathUtils.random(25.0f, 70.0f)), new Byrd(200.0f, MathUtils.random(25.0f, 70.0f))});
            }
            case "4 Byrds": {
                return new MonsterGroup(new AbstractMonster[]{new Byrd(-470.0f, MathUtils.random(25.0f, 70.0f)), new Byrd(-210.0f, MathUtils.random(25.0f, 70.0f)), new Byrd(50.0f, MathUtils.random(25.0f, 70.0f)), new Byrd(310.0f, MathUtils.random(25.0f, 70.0f))});
            }
            case "Chosen": {
                return new MonsterGroup(new Chosen());
            }
            case "Shell Parasite": {
                return new MonsterGroup(new ShelledParasite());
            }
            case "Spheric Guardian": {
                return new MonsterGroup(new SphericGuardian());
            }
            case "Cultist and Chosen": {
                return new MonsterGroup(new AbstractMonster[]{new Cultist(-230.0f, 15.0f, false), new Chosen(100.0f, 25.0f)});
            }
            case "3 Cultists": {
                return new MonsterGroup(new AbstractMonster[]{new Cultist(-465.0f, -20.0f, false), new Cultist(-130.0f, 15.0f, false), new Cultist(200.0f, -5.0f)});
            }
            case "Chosen and Byrds": {
                return new MonsterGroup(new AbstractMonster[]{new Byrd(-170.0f, MathUtils.random(25.0f, 70.0f)), new Chosen(80.0f, 0.0f)});
            }
            case "Sentry and Sphere": {
                return new MonsterGroup(new AbstractMonster[]{new Sentry(-305.0f, 30.0f), new SphericGuardian()});
            }
            case "Snake Plant": {
                return new MonsterGroup(new SnakePlant(-30.0f, -30.0f));
            }
            case "Snecko": {
                return new MonsterGroup(new Snecko());
            }
            case "Centurion and Healer": {
                return new MonsterGroup(new AbstractMonster[]{new Centurion(-200.0f, 15.0f), new Healer(120.0f, 0.0f)});
            }
            case "Shelled Parasite and Fungi": {
                return new MonsterGroup(new AbstractMonster[]{new ShelledParasite(-260.0f, 15.0f), new FungiBeast(120.0f, 0.0f)});
            }
            case "Book of Stabbing": {
                return new MonsterGroup(new BookOfStabbing());
            }
            case "Gremlin Leader": {
                return new MonsterGroup(new AbstractMonster[]{MonsterHelper.spawnGremlin(GremlinLeader.POSX[0], GremlinLeader.POSY[0]), MonsterHelper.spawnGremlin(GremlinLeader.POSX[1], GremlinLeader.POSY[1]), new GremlinLeader()});
            }
            case "Slavers": {
                return new MonsterGroup(new AbstractMonster[]{new SlaverBlue(-385.0f, -15.0f), new Taskmaster(-133.0f, 0.0f), new SlaverRed(125.0f, -30.0f)});
            }
            case "Masked Bandits": {
                return new MonsterGroup(new AbstractMonster[]{new BanditPointy(-320.0f, 0.0f), new BanditLeader(-75.0f, -6.0f), new BanditBear(150.0f, -6.0f)});
            }
            case "Colosseum Nobs": {
                return new MonsterGroup(new AbstractMonster[]{new Taskmaster(-270.0f, 15.0f), new GremlinNob(130.0f, 0.0f)});
            }
            case "Colosseum Slavers": {
                return new MonsterGroup(new AbstractMonster[]{new SlaverBlue(-270.0f, 15.0f), new SlaverRed(130.0f, 0.0f)});
            }
            case "Automaton": {
                return new MonsterGroup(new BronzeAutomaton());
            }
            case "Champ": {
                return new MonsterGroup(new Champ());
            }
            case "Collector": {
                return new MonsterGroup(new TheCollector());
            }
        }
        switch (key) {
            case "Flame Bruiser 1 Orb": {
                return new MonsterGroup(new AbstractMonster[]{new Reptomancer(), new SnakeDagger(Reptomancer.POSX[0], Reptomancer.POSY[0])});
            }
            case "Flame Bruiser 2 Orb": 
            case "Reptomancer": {
                return new MonsterGroup(new AbstractMonster[]{new SnakeDagger(Reptomancer.POSX[1], Reptomancer.POSY[1]), new Reptomancer(), new SnakeDagger(Reptomancer.POSX[0], Reptomancer.POSY[0])});
            }
            case "Transient": {
                return new MonsterGroup(new Transient());
            }
            case "3 Darklings": {
                return new MonsterGroup(new AbstractMonster[]{new Darkling(-440.0f, 10.0f), new Darkling(-140.0f, 30.0f), new Darkling(180.0f, -5.0f)});
            }
            case "3 Shapes": {
                return MonsterHelper.spawnShapes(true);
            }
            case "Jaw Worm Horde": {
                return new MonsterGroup(new AbstractMonster[]{new JawWorm(-490.0f, -5.0f, true), new JawWorm(-150.0f, 20.0f, true), new JawWorm(175.0f, 5.0f, true)});
            }
            case "Snecko and Mystics": {
                return new MonsterGroup(new AbstractMonster[]{new Healer(-475.0f, -10.0f), new Snecko(-130.0f, -13.0f), new Healer(175.0f, -10.0f)});
            }
            case "Orb Walker": {
                return new MonsterGroup(new OrbWalker(-30.0f, 20.0f));
            }
            case "Spire Growth": {
                return new MonsterGroup(new SpireGrowth());
            }
            case "Maw": {
                return new MonsterGroup(new Maw(-70.0f, 20.0f));
            }
            case "4 Shapes": {
                return MonsterHelper.spawnShapes(false);
            }
            case "Sphere and 2 Shapes": {
                return new MonsterGroup(new AbstractMonster[]{MonsterHelper.getAncientShape(-435.0f, 10.0f), MonsterHelper.getAncientShape(-210.0f, 0.0f), new SphericGuardian(110.0f, 10.0f)});
            }
            case "2 Orb Walkers": {
                return new MonsterGroup(new AbstractMonster[]{new OrbWalker(-250.0f, 32.0f), new OrbWalker(150.0f, 26.0f)});
            }
            case "Nemesis": {
                return new MonsterGroup(new Nemesis());
            }
            case "Writhing Mass": {
                return new MonsterGroup(new WrithingMass());
            }
            case "Giant Head": {
                return new MonsterGroup(new GiantHead());
            }
            case "Mysterious Sphere": {
                return new MonsterGroup(new AbstractMonster[]{MonsterHelper.getAncientShape(-475.0f, 10.0f), MonsterHelper.getAncientShape(-250.0f, 0.0f), new OrbWalker(150.0f, 30.0f)});
            }
            case "Time Eater": {
                return new MonsterGroup(new TimeEater());
            }
            case "Awakened One": {
                return new MonsterGroup(new AbstractMonster[]{new Cultist(-590.0f, 10.0f, false), new Cultist(-298.0f, -10.0f, false), new AwakenedOne(100.0f, 15.0f)});
            }
            case "Donu and Deca": {
                return new MonsterGroup(new AbstractMonster[]{new Deca(), new Donu()});
            }
        }
        switch (key) {
            case "The Heart": {
                return new MonsterGroup(new CorruptHeart());
            }
            case "Shield and Spear": {
                return new MonsterGroup(new AbstractMonster[]{new SpireShield(), new SpireSpear()});
            }
        }
        return new MonsterGroup(new ApologySlime());
    }

    private static float randomYOffset(float y) {
        return y + MathUtils.random(-20.0f, 20.0f);
    }

    private static float randomXOffset(float x) {
        return x + MathUtils.random(-20.0f, 20.0f);
    }

    public static AbstractMonster getGremlin(String key, float xPos, float yPos) {
        switch (key) {
            case "GremlinWarrior": {
                return new GremlinWarrior(xPos, yPos);
            }
            case "GremlinThief": {
                return new GremlinThief(xPos, yPos);
            }
            case "GremlinFat": {
                return new GremlinFat(xPos, yPos);
            }
            case "GremlinTsundere": {
                return new GremlinTsundere(xPos, yPos);
            }
            case "GremlinWizard": {
                return new GremlinWizard(xPos, yPos);
            }
        }
        logger.info("UNKNOWN GREMLIN: " + key);
        return null;
    }

    public static AbstractMonster getAncientShape(float x, float y) {
        switch (AbstractDungeon.miscRng.random(2)) {
            case 0: {
                return new Spiker(x, y);
            }
            case 1: {
                return new Repulsor(x, y);
            }
        }
        return new Exploder(x, y);
    }

    public static AbstractMonster getShape(String key, float xPos, float yPos) {
        switch (key) {
            case "Repulsor": {
                return new Repulsor(xPos, yPos);
            }
            case "Spiker": {
                return new Spiker(xPos, yPos);
            }
            case "Exploder": {
                return new Exploder(xPos, yPos);
            }
        }
        logger.info("UNKNOWN SHAPE: " + key);
        return null;
    }

    private static MonsterGroup spawnShapes(boolean weak) {
        ArrayList<String> shapePool = new ArrayList<String>();
        shapePool.add("Repulsor");
        shapePool.add("Repulsor");
        shapePool.add("Exploder");
        shapePool.add("Exploder");
        shapePool.add("Spiker");
        shapePool.add("Spiker");
        AbstractMonster[] retVal = weak ? new AbstractMonster[3] : new AbstractMonster[4];
        int index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
        String key = (String)shapePool.get(index);
        shapePool.remove(index);
        retVal[0] = MonsterHelper.getShape(key, -480.0f, 6.0f);
        index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
        key = (String)shapePool.get(index);
        shapePool.remove(index);
        retVal[1] = MonsterHelper.getShape(key, -240.0f, -6.0f);
        index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
        key = (String)shapePool.get(index);
        shapePool.remove(index);
        retVal[2] = MonsterHelper.getShape(key, 0.0f, -12.0f);
        if (!weak) {
            index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
            key = (String)shapePool.get(index);
            shapePool.remove(index);
            retVal[3] = MonsterHelper.getShape(key, 240.0f, 12.0f);
        }
        return new MonsterGroup(retVal);
    }

    private static MonsterGroup spawnSmallSlimes() {
        AbstractMonster[] retVal = new AbstractMonster[2];
        if (AbstractDungeon.miscRng.randomBoolean()) {
            retVal[0] = new SpikeSlime_S(-230.0f, 32.0f, 0);
            retVal[1] = new AcidSlime_M(35.0f, 8.0f);
        } else {
            retVal[0] = new AcidSlime_S(-230.0f, 32.0f, 0);
            retVal[1] = new SpikeSlime_M(35.0f, 8.0f);
        }
        return new MonsterGroup(retVal);
    }

    private static MonsterGroup spawnManySmallSlimes() {
        ArrayList<String> slimePool = new ArrayList<String>();
        slimePool.add("SpikeSlime_S");
        slimePool.add("SpikeSlime_S");
        slimePool.add("SpikeSlime_S");
        slimePool.add("AcidSlime_S");
        slimePool.add("AcidSlime_S");
        AbstractMonster[] retVal = new AbstractMonster[5];
        int index = AbstractDungeon.miscRng.random(slimePool.size() - 1);
        String key = (String)slimePool.get(index);
        slimePool.remove(index);
        retVal[0] = key.equals("SpikeSlime_S") ? new SpikeSlime_S(-480.0f, 30.0f, 0) : new AcidSlime_S(-480.0f, 30.0f, 0);
        index = AbstractDungeon.miscRng.random(slimePool.size() - 1);
        key = (String)slimePool.get(index);
        slimePool.remove(index);
        retVal[1] = key.equals("SpikeSlime_S") ? new SpikeSlime_S(-320.0f, 2.0f, 0) : new AcidSlime_S(-320.0f, 2.0f, 0);
        index = AbstractDungeon.miscRng.random(slimePool.size() - 1);
        key = (String)slimePool.get(index);
        slimePool.remove(index);
        retVal[2] = key.equals("SpikeSlime_S") ? new SpikeSlime_S(-160.0f, 32.0f, 0) : new AcidSlime_S(-160.0f, 32.0f, 0);
        index = AbstractDungeon.miscRng.random(slimePool.size() - 1);
        key = (String)slimePool.get(index);
        slimePool.remove(index);
        retVal[3] = key.equals("SpikeSlime_S") ? new SpikeSlime_S(10.0f, -12.0f, 0) : new AcidSlime_S(10.0f, -12.0f, 0);
        index = AbstractDungeon.miscRng.random(slimePool.size() - 1);
        key = (String)slimePool.get(index);
        slimePool.remove(index);
        retVal[4] = key.equals("SpikeSlime_S") ? new SpikeSlime_S(200.0f, 9.0f, 0) : new AcidSlime_S(200.0f, 9.0f, 0);
        return new MonsterGroup(retVal);
    }

    private static MonsterGroup spawnGremlins() {
        ArrayList<String> gremlinPool = new ArrayList<String>();
        gremlinPool.add("GremlinWarrior");
        gremlinPool.add("GremlinWarrior");
        gremlinPool.add("GremlinThief");
        gremlinPool.add("GremlinThief");
        gremlinPool.add("GremlinFat");
        gremlinPool.add("GremlinFat");
        gremlinPool.add("GremlinTsundere");
        gremlinPool.add("GremlinWizard");
        AbstractMonster[] retVal = new AbstractMonster[4];
        int index = AbstractDungeon.miscRng.random(gremlinPool.size() - 1);
        String key = (String)gremlinPool.get(index);
        gremlinPool.remove(index);
        retVal[0] = MonsterHelper.getGremlin(key, -320.0f, 25.0f);
        index = AbstractDungeon.miscRng.random(gremlinPool.size() - 1);
        key = (String)gremlinPool.get(index);
        gremlinPool.remove(index);
        retVal[1] = MonsterHelper.getGremlin(key, -160.0f, -12.0f);
        index = AbstractDungeon.miscRng.random(gremlinPool.size() - 1);
        key = (String)gremlinPool.get(index);
        gremlinPool.remove(index);
        retVal[2] = MonsterHelper.getGremlin(key, 25.0f, -35.0f);
        index = AbstractDungeon.miscRng.random(gremlinPool.size() - 1);
        key = (String)gremlinPool.get(index);
        gremlinPool.remove(index);
        retVal[3] = MonsterHelper.getGremlin(key, 205.0f, 40.0f);
        return new MonsterGroup(retVal);
    }

    private static AbstractMonster spawnGremlin(float x, float y) {
        ArrayList<String> gremlinPool = new ArrayList<String>();
        gremlinPool.add("GremlinWarrior");
        gremlinPool.add("GremlinWarrior");
        gremlinPool.add("GremlinThief");
        gremlinPool.add("GremlinThief");
        gremlinPool.add("GremlinFat");
        gremlinPool.add("GremlinFat");
        gremlinPool.add("GremlinTsundere");
        gremlinPool.add("GremlinWizard");
        return MonsterHelper.getGremlin((String)gremlinPool.get(AbstractDungeon.miscRng.random(0, gremlinPool.size() - 1)), x, y);
    }

    private static MonsterGroup bottomHumanoid() {
        AbstractMonster[] monsters = new AbstractMonster[]{MonsterHelper.bottomGetWeakWildlife(MonsterHelper.randomXOffset(-160.0f), MonsterHelper.randomYOffset(20.0f)), MonsterHelper.bottomGetStrongHumanoid(MonsterHelper.randomXOffset(130.0f), MonsterHelper.randomYOffset(20.0f))};
        return new MonsterGroup(monsters);
    }

    private static MonsterGroup bottomWildlife() {
        int numMonster = 2;
        AbstractMonster[] monsters = new AbstractMonster[numMonster];
        if (numMonster == 2) {
            monsters[0] = MonsterHelper.bottomGetStrongWildlife(MonsterHelper.randomXOffset(-150.0f), MonsterHelper.randomYOffset(20.0f));
            monsters[1] = MonsterHelper.bottomGetWeakWildlife(MonsterHelper.randomXOffset(150.0f), MonsterHelper.randomYOffset(20.0f));
        } else if (numMonster == 3) {
            monsters[0] = MonsterHelper.bottomGetWeakWildlife(MonsterHelper.randomXOffset(-200.0f), MonsterHelper.randomYOffset(20.0f));
            monsters[1] = MonsterHelper.bottomGetWeakWildlife(MonsterHelper.randomXOffset(0.0f), MonsterHelper.randomYOffset(20.0f));
            monsters[2] = MonsterHelper.bottomGetWeakWildlife(MonsterHelper.randomXOffset(200.0f), MonsterHelper.randomYOffset(20.0f));
        }
        return new MonsterGroup(monsters);
    }

    private static AbstractMonster bottomGetStrongHumanoid(float x, float y) {
        ArrayList<AbstractMonster> monsters = new ArrayList<AbstractMonster>();
        monsters.add(new Cultist(x, y));
        monsters.add(MonsterHelper.getSlaver(x, y));
        monsters.add(new Looter(x, y));
        AbstractMonster output = (AbstractMonster)monsters.get(AbstractDungeon.miscRng.random(0, monsters.size() - 1));
        return output;
    }

    private static AbstractMonster bottomGetStrongWildlife(float x, float y) {
        ArrayList<AbstractMonster> monsters = new ArrayList<AbstractMonster>();
        monsters.add(new FungiBeast(x, y));
        monsters.add(new JawWorm(x, y));
        AbstractMonster output = (AbstractMonster)monsters.get(AbstractDungeon.miscRng.random(0, monsters.size() - 1));
        return output;
    }

    private static AbstractMonster bottomGetWeakWildlife(float x, float y) {
        ArrayList<AbstractMonster> monsters = new ArrayList<AbstractMonster>();
        monsters.add(MonsterHelper.getLouse(x, y));
        monsters.add(new SpikeSlime_M(x, y));
        monsters.add(new AcidSlime_M(x, y));
        return (AbstractMonster)monsters.get(AbstractDungeon.miscRng.random(0, monsters.size() - 1));
    }

    private static AbstractMonster getSlaver(float x, float y) {
        if (AbstractDungeon.miscRng.randomBoolean()) {
            return new SlaverRed(x, y);
        }
        return new SlaverBlue(x, y);
    }

    private static AbstractMonster getLouse(float x, float y) {
        if (AbstractDungeon.miscRng.randomBoolean()) {
            return new LouseNormal(x, y);
        }
        return new LouseDefensive(x, y);
    }

    public static void uploadEnemyData() {
        ArrayList<String> derp = new ArrayList<String>();
        ArrayList<EnemyData> data = new ArrayList<EnemyData>();
        data.add(new EnemyData(BLUE_SLAVER_ENC, 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(CULTIST_ENC, 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(JAW_WORM_ENC, 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(TWO_LOUSE_ENC, 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(SMALL_SLIMES_ENC, 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(GREMLIN_GANG_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(LARGE_SLIME_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(LOOTER_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(LOTS_OF_SLIMES_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(LVL_1_THUGS_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(LVL_1_WILDLIFE_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(RED_SLAVER_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(THREE_LOUSE_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(TWO_FUNGI_ENC, 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(GREMLIN_NOB_ENC, 1, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(LAGAVULIN_ENC, 1, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(THREE_SENTRY_ENC, 1, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(LAGAVULIN_EVENT_ENC, 1, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(MUSHROOMS_EVENT_ENC, 1, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(GUARDIAN_ENC, 1, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(HEXAGHOST_ENC, 1, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(SLIME_BOSS_ENC, 1, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(CHOSEN_ENC, 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(SHELL_PARASITE_ENC, 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(SPHERE_GUARDIAN_ENC, 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(THREE_BYRDS_ENC, 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(TWO_THIEVES_ENC, 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(CHOSEN_FLOCK_ENC, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(SENTRY_SPHERE_ENC, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(SNAKE_PLANT_ENC, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(SNECKO_ENC, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(TANK_HEALER_ENC, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(CULTIST_CHOSEN_ENC, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(THREE_CULTISTS_ENC, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(PARASITE_AND_FUNGUS, 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(GREMLIN_LEADER_ENC, 2, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(SLAVERS_ENC, 2, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(STAB_BOOK_ENC, 2, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(MASKED_BANDITS_ENC, 2, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(COLOSSEUM_NOB_ENC, 2, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(COLOSSEUM_SLAVER_ENC, 2, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(AUTOMATON_ENC, 2, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(CHAMP_ENC, 2, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(COLLECTOR_ENC, 2, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(ORB_WALKER_ENC, 3, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(THREE_DARKLINGS_ENC, 3, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(THREE_SHAPES_ENC, 3, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData(TRANSIENT_ENC, 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(FOUR_SHAPES_ENC, 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(MAW_ENC, 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(JAW_WORMS_HORDE, 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(SPHERE_TWO_SHAPES_ENC, 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(SPIRE_GROWTH_ENC, 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(WRITHING_MASS_ENC, 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData(GIANT_HEAD_ENC, 3, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(NEMESIS_ENC, 3, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(REPTOMANCER_ENC, 3, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(MYSTERIOUS_SPHERE_ENC, 3, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(MIND_BLOOM_BOSS, 3, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(TWO_ORB_WALKER_ENC, 3, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData(AWAKENED_ENC, 3, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(DONU_DECA_ENC, 3, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(TIME_EATER_ENC, 3, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData(SHIELD_SPEAR_ENC, 4, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData(THE_HEART_ENC, 4, EnemyData.MonsterType.BOSS));
        for (EnemyData d : data) {
            derp.add(d.gameDataUploadData());
        }
        BotDataUploader.uploadDataAsync(BotDataUploader.GameDataType.ENEMY_DATA, EnemyData.gameDataUploadHeader(), derp);
    }
}

