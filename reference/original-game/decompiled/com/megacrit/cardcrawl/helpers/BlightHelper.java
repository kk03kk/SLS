/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.blights.Accursed;
import com.megacrit.cardcrawl.blights.AncientAugmentation;
import com.megacrit.cardcrawl.blights.Durian;
import com.megacrit.cardcrawl.blights.GrotesqueTrophy;
import com.megacrit.cardcrawl.blights.Hauntings;
import com.megacrit.cardcrawl.blights.MimicInfestation;
import com.megacrit.cardcrawl.blights.Muzzle;
import com.megacrit.cardcrawl.blights.Scatterbrain;
import com.megacrit.cardcrawl.blights.Shield;
import com.megacrit.cardcrawl.blights.Spear;
import com.megacrit.cardcrawl.blights.TimeMaze;
import com.megacrit.cardcrawl.blights.TwistingMind;
import com.megacrit.cardcrawl.blights.VoidEssence;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import com.megacrit.cardcrawl.random.Random;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BlightHelper {
    private static final Logger logger = LogManager.getLogger(BlightHelper.class.getName());
    public static ArrayList<String> blights = new ArrayList();
    public static ArrayList<String> chestBlights = new ArrayList();

    public static void initialize() {
        blights.clear();
        blights.add("DeadlyEnemies");
        blights.add("ToughEnemies");
        blights.add("TimeMaze");
        blights.add("MimicInfestation");
        blights.add("FullBelly");
        blights.add("GrotesqueTrophy");
        blights.add("Accursed");
        blights.add("Scatterbrain");
        blights.add("TwistingMind");
        blights.add("BlightedDurian");
        blights.add("VoidEssence");
        blights.add("GraspOfShadows");
        blights.add("MetallicRebirth");
        chestBlights.clear();
        chestBlights.add("Accursed");
        chestBlights.add("Scatterbrain");
        chestBlights.add("TwistingMind");
        chestBlights.add("BlightedDurian");
        chestBlights.add("VoidEssence");
        chestBlights.add("GraspOfShadows");
        chestBlights.add("MetallicRebirth");
    }

    public static AbstractBlight getBlight(String id) {
        switch (id) {
            case "DeadlyEnemies": {
                return new Spear();
            }
            case "ToughEnemies": {
                return new Shield();
            }
            case "TimeMaze": {
                return new TimeMaze();
            }
            case "MimicInfestation": {
                return new MimicInfestation();
            }
            case "FullBelly": {
                return new Muzzle();
            }
            case "GrotesqueTrophy": {
                return new GrotesqueTrophy();
            }
            case "TwistingMind": {
                return new TwistingMind();
            }
            case "Scatterbrain": {
                return new Scatterbrain();
            }
            case "Accursed": {
                return new Accursed();
            }
            case "BlightedDurian": {
                return new Durian();
            }
            case "VoidEssence": {
                return new VoidEssence();
            }
            case "GraspOfShadows": {
                return new Hauntings();
            }
            case "MetallicRebirth": {
                return new AncientAugmentation();
            }
        }
        logger.info("MISSING KEY: " + id);
        return null;
    }

    public static AbstractBlight getRandomChestBlight(ArrayList<String> exclusion) {
        ArrayList<String> blightTmp = new ArrayList<String>();
        for (String s : chestBlights) {
            boolean exclude = false;
            for (String s2 : exclusion) {
                if (!s.equals(s2)) continue;
                logger.info(s + " EXCLUDED");
                exclude = true;
            }
            if (exclude) continue;
            blightTmp.add(s);
        }
        String randomKey = (String)blightTmp.get(AbstractDungeon.relicRng.random(blightTmp.size() - 1));
        return BlightHelper.getBlight(randomKey);
    }

    public static AbstractBlight getRandomBlight(Random rng) {
        String randomKey = blights.get(rng.random(blights.size() - 1));
        return BlightHelper.getBlight(randomKey);
    }

    public static AbstractBlight getRandomBlight() {
        String randomKey = chestBlights.get(AbstractDungeon.relicRng.random(chestBlights.size() - 1));
        if (AbstractDungeon.player.maxHealth <= 20) {
            while (randomKey.equals("BlightedDurian")) {
                randomKey = chestBlights.get(AbstractDungeon.relicRng.random(chestBlights.size() - 1));
            }
        }
        return BlightHelper.getBlight(randomKey);
    }

    public static void uploadBlightData() {
        ArrayList<String> data = new ArrayList<String>();
        if (blights.isEmpty()) {
            BlightHelper.initialize();
        }
        ArrayList<String> allBlights = new ArrayList<String>(blights);
        allBlights.addAll(chestBlights);
        for (String b : allBlights) {
            AbstractBlight blight = BlightHelper.getBlight(b);
            if (blight == null) continue;
            data.add(blight.gameDataUploadData());
        }
        BotDataUploader.uploadDataAsync(BotDataUploader.GameDataType.BLIGHT_DATA, AbstractBlight.gameDataUploadHeader(), data);
    }
}

