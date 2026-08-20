/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.Ambrosia;
import com.megacrit.cardcrawl.potions.AncientPotion;
import com.megacrit.cardcrawl.potions.AttackPotion;
import com.megacrit.cardcrawl.potions.BlessingOfTheForge;
import com.megacrit.cardcrawl.potions.BlockPotion;
import com.megacrit.cardcrawl.potions.BloodPotion;
import com.megacrit.cardcrawl.potions.BottledMiracle;
import com.megacrit.cardcrawl.potions.ColorlessPotion;
import com.megacrit.cardcrawl.potions.CultistPotion;
import com.megacrit.cardcrawl.potions.CunningPotion;
import com.megacrit.cardcrawl.potions.DexterityPotion;
import com.megacrit.cardcrawl.potions.DistilledChaosPotion;
import com.megacrit.cardcrawl.potions.DuplicationPotion;
import com.megacrit.cardcrawl.potions.Elixir;
import com.megacrit.cardcrawl.potions.EnergyPotion;
import com.megacrit.cardcrawl.potions.EntropicBrew;
import com.megacrit.cardcrawl.potions.EssenceOfDarkness;
import com.megacrit.cardcrawl.potions.EssenceOfSteel;
import com.megacrit.cardcrawl.potions.ExplosivePotion;
import com.megacrit.cardcrawl.potions.FairyPotion;
import com.megacrit.cardcrawl.potions.FearPotion;
import com.megacrit.cardcrawl.potions.FirePotion;
import com.megacrit.cardcrawl.potions.FocusPotion;
import com.megacrit.cardcrawl.potions.FruitJuice;
import com.megacrit.cardcrawl.potions.GamblersBrew;
import com.megacrit.cardcrawl.potions.GhostInAJar;
import com.megacrit.cardcrawl.potions.HeartOfIron;
import com.megacrit.cardcrawl.potions.LiquidBronze;
import com.megacrit.cardcrawl.potions.LiquidMemories;
import com.megacrit.cardcrawl.potions.PoisonPotion;
import com.megacrit.cardcrawl.potions.PotionOfCapacity;
import com.megacrit.cardcrawl.potions.PowerPotion;
import com.megacrit.cardcrawl.potions.RegenPotion;
import com.megacrit.cardcrawl.potions.SkillPotion;
import com.megacrit.cardcrawl.potions.SmokeBomb;
import com.megacrit.cardcrawl.potions.SneckoOil;
import com.megacrit.cardcrawl.potions.SpeedPotion;
import com.megacrit.cardcrawl.potions.StancePotion;
import com.megacrit.cardcrawl.potions.SteroidPotion;
import com.megacrit.cardcrawl.potions.StrengthPotion;
import com.megacrit.cardcrawl.potions.SwiftPotion;
import com.megacrit.cardcrawl.potions.WeakenPotion;
import com.megacrit.cardcrawl.random.Random;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PotionHelper {
    private static final Logger logger = LogManager.getLogger(PotionHelper.class.getName());
    public static ArrayList<String> potions = new ArrayList();
    public static int POTION_COMMON_CHANCE = 65;
    public static int POTION_UNCOMMON_CHANCE = 25;

    public static void initialize(AbstractPlayer.PlayerClass chosenClass) {
        potions.clear();
        potions = PotionHelper.getPotions(chosenClass, false);
    }

    public static ArrayList<AbstractPotion> getPotionsByRarity(AbstractPotion.PotionRarity rarity) {
        ArrayList<AbstractPotion> retVal = new ArrayList<AbstractPotion>();
        for (String s : PotionHelper.getPotions(null, true)) {
            AbstractPotion p = PotionHelper.getPotion(s);
            if (p.rarity != rarity) continue;
            retVal.add(p);
        }
        return retVal;
    }

    public static ArrayList<String> getPotions(AbstractPlayer.PlayerClass c, boolean getAll) {
        ArrayList<String> retVal = new ArrayList<String>();
        if (!getAll) {
            switch (c) {
                case IRONCLAD: {
                    retVal.add("BloodPotion");
                    retVal.add("ElixirPotion");
                    retVal.add("HeartOfIron");
                    break;
                }
                case THE_SILENT: {
                    retVal.add("Poison Potion");
                    retVal.add("CunningPotion");
                    retVal.add("GhostInAJar");
                    break;
                }
                case DEFECT: {
                    retVal.add("FocusPotion");
                    retVal.add("PotionOfCapacity");
                    retVal.add("EssenceOfDarkness");
                    break;
                }
                case WATCHER: {
                    retVal.add("BottledMiracle");
                    retVal.add("StancePotion");
                    retVal.add("Ambrosia");
                    break;
                }
            }
        } else {
            retVal.add("BloodPotion");
            retVal.add("ElixirPotion");
            retVal.add("HeartOfIron");
            retVal.add("Poison Potion");
            retVal.add("CunningPotion");
            retVal.add("GhostInAJar");
            retVal.add("FocusPotion");
            retVal.add("PotionOfCapacity");
            retVal.add("EssenceOfDarkness");
            retVal.add("BottledMiracle");
            retVal.add("StancePotion");
            retVal.add("Ambrosia");
        }
        retVal.add("Block Potion");
        retVal.add("Dexterity Potion");
        retVal.add("Energy Potion");
        retVal.add("Explosive Potion");
        retVal.add("Fire Potion");
        retVal.add("Strength Potion");
        retVal.add("Swift Potion");
        retVal.add("Weak Potion");
        retVal.add("FearPotion");
        retVal.add("AttackPotion");
        retVal.add("SkillPotion");
        retVal.add("PowerPotion");
        retVal.add("ColorlessPotion");
        retVal.add("SteroidPotion");
        retVal.add("SpeedPotion");
        retVal.add("BlessingOfTheForge");
        retVal.add("Regen Potion");
        retVal.add("Ancient Potion");
        retVal.add("LiquidBronze");
        retVal.add("GamblersBrew");
        retVal.add("EssenceOfSteel");
        retVal.add("DuplicationPotion");
        retVal.add("DistilledChaos");
        retVal.add("LiquidMemories");
        retVal.add("CultistPotion");
        retVal.add("Fruit Juice");
        retVal.add("SneckoOil");
        retVal.add("FairyPotion");
        retVal.add("SmokeBomb");
        retVal.add("EntropicBrew");
        return retVal;
    }

    public static AbstractPotion getRandomPotion(Random rng) {
        String randomKey = potions.get(rng.random(potions.size() - 1));
        return PotionHelper.getPotion(randomKey);
    }

    public static AbstractPotion getRandomPotion() {
        String randomKey = potions.get(AbstractDungeon.potionRng.random(potions.size() - 1));
        return PotionHelper.getPotion(randomKey);
    }

    public static boolean isAPotion(String key) {
        return PotionHelper.getPotions(null, true).contains(key);
    }

    public static AbstractPotion getPotion(String name) {
        if (name == null || name.equals("")) {
            return null;
        }
        switch (name) {
            case "Ambrosia": {
                return new Ambrosia();
            }
            case "BottledMiracle": {
                return new BottledMiracle();
            }
            case "EssenceOfDarkness": {
                return new EssenceOfDarkness();
            }
            case "Block Potion": {
                return new BlockPotion();
            }
            case "Dexterity Potion": {
                return new DexterityPotion();
            }
            case "Energy Potion": {
                return new EnergyPotion();
            }
            case "Explosive Potion": {
                return new ExplosivePotion();
            }
            case "Fire Potion": {
                return new FirePotion();
            }
            case "Strength Potion": {
                return new StrengthPotion();
            }
            case "Swift Potion": {
                return new SwiftPotion();
            }
            case "Poison Potion": {
                return new PoisonPotion();
            }
            case "Weak Potion": {
                return new WeakenPotion();
            }
            case "FearPotion": {
                return new FearPotion();
            }
            case "SkillPotion": {
                return new SkillPotion();
            }
            case "PowerPotion": {
                return new PowerPotion();
            }
            case "AttackPotion": {
                return new AttackPotion();
            }
            case "ColorlessPotion": {
                return new ColorlessPotion();
            }
            case "SteroidPotion": {
                return new SteroidPotion();
            }
            case "SpeedPotion": {
                return new SpeedPotion();
            }
            case "BlessingOfTheForge": {
                return new BlessingOfTheForge();
            }
            case "PotionOfCapacity": {
                return new PotionOfCapacity();
            }
            case "CunningPotion": {
                return new CunningPotion();
            }
            case "DistilledChaos": {
                return new DistilledChaosPotion();
            }
            case "Ancient Potion": {
                return new AncientPotion();
            }
            case "Regen Potion": {
                return new RegenPotion();
            }
            case "GhostInAJar": {
                return new GhostInAJar();
            }
            case "FocusPotion": {
                return new FocusPotion();
            }
            case "LiquidBronze": {
                return new LiquidBronze();
            }
            case "LiquidMemories": {
                return new LiquidMemories();
            }
            case "GamblersBrew": {
                return new GamblersBrew();
            }
            case "EssenceOfSteel": {
                return new EssenceOfSteel();
            }
            case "BloodPotion": {
                return new BloodPotion();
            }
            case "StancePotion": {
                return new StancePotion();
            }
            case "DuplicationPotion": {
                return new DuplicationPotion();
            }
            case "ElixirPotion": {
                return new Elixir();
            }
            case "CultistPotion": {
                return new CultistPotion();
            }
            case "Fruit Juice": {
                return new FruitJuice();
            }
            case "SneckoOil": {
                return new SneckoOil();
            }
            case "FairyPotion": {
                return new FairyPotion();
            }
            case "SmokeBomb": {
                return new SmokeBomb();
            }
            case "EntropicBrew": {
                return new EntropicBrew();
            }
            case "HeartOfIron": {
                return new HeartOfIron();
            }
            case "Potion Slot": {
                return null;
            }
        }
        logger.info("MISSING KEY: POTIONHELPER 37: " + name);
        return new FirePotion();
    }

    public static void uploadPotionData() {
        PotionHelper.initialize(AbstractPlayer.PlayerClass.IRONCLAD);
        HashSet<String> ironcladPotions = new HashSet<String>(potions);
        HashSet<String> sharedPotions = new HashSet<String>(potions);
        PotionHelper.initialize(AbstractPlayer.PlayerClass.THE_SILENT);
        HashSet<String> silentPotions = new HashSet<String>(potions);
        sharedPotions.retainAll(potions);
        PotionHelper.initialize(AbstractPlayer.PlayerClass.DEFECT);
        HashSet<String> defectPotions = new HashSet<String>(potions);
        sharedPotions.retainAll(potions);
        PotionHelper.initialize(AbstractPlayer.PlayerClass.WATCHER);
        HashSet<String> watcherPotions = new HashSet<String>(potions);
        sharedPotions.retainAll(potions);
        ironcladPotions.removeAll(sharedPotions);
        silentPotions.removeAll(sharedPotions);
        defectPotions.removeAll(sharedPotions);
        watcherPotions.removeAll(sharedPotions);
        potions.clear();
        ArrayList<String> data = new ArrayList<String>();
        for (String id : ironcladPotions) {
            data.add(PotionHelper.getPotion(id).getUploadData("RED"));
        }
        for (String id : silentPotions) {
            data.add(PotionHelper.getPotion(id).getUploadData("GREEN"));
        }
        for (String id : defectPotions) {
            data.add(PotionHelper.getPotion(id).getUploadData("BLUE"));
        }
        for (String id : watcherPotions) {
            data.add(PotionHelper.getPotion(id).getUploadData("PURPLE"));
        }
        for (String id : sharedPotions) {
            data.add(PotionHelper.getPotion(id).getUploadData("ALL"));
        }
        BotDataUploader.uploadDataAsync(BotDataUploader.GameDataType.POTION_DATA, AbstractPotion.gameDataUploadHeader(), data);
    }
}

