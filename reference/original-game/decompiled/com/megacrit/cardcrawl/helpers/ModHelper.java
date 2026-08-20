/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.daily.mods.Allstar;
import com.megacrit.cardcrawl.daily.mods.BigGameHunter;
import com.megacrit.cardcrawl.daily.mods.Binary;
import com.megacrit.cardcrawl.daily.mods.BlueCards;
import com.megacrit.cardcrawl.daily.mods.Brewmaster;
import com.megacrit.cardcrawl.daily.mods.CertainFuture;
import com.megacrit.cardcrawl.daily.mods.Chimera;
import com.megacrit.cardcrawl.daily.mods.ColorlessCards;
import com.megacrit.cardcrawl.daily.mods.Colossus;
import com.megacrit.cardcrawl.daily.mods.ControlledChaos;
import com.megacrit.cardcrawl.daily.mods.CursedRun;
import com.megacrit.cardcrawl.daily.mods.DeadlyEvents;
import com.megacrit.cardcrawl.daily.mods.Diverse;
import com.megacrit.cardcrawl.daily.mods.Draft;
import com.megacrit.cardcrawl.daily.mods.Flight;
import com.megacrit.cardcrawl.daily.mods.GreenCards;
import com.megacrit.cardcrawl.daily.mods.Heirloom;
import com.megacrit.cardcrawl.daily.mods.Hoarder;
import com.megacrit.cardcrawl.daily.mods.Insanity;
import com.megacrit.cardcrawl.daily.mods.Lethality;
import com.megacrit.cardcrawl.daily.mods.Midas;
import com.megacrit.cardcrawl.daily.mods.NightTerrors;
import com.megacrit.cardcrawl.daily.mods.PurpleCards;
import com.megacrit.cardcrawl.daily.mods.RedCards;
import com.megacrit.cardcrawl.daily.mods.SealedDeck;
import com.megacrit.cardcrawl.daily.mods.Shiny;
import com.megacrit.cardcrawl.daily.mods.Specialized;
import com.megacrit.cardcrawl.daily.mods.Terminal;
import com.megacrit.cardcrawl.daily.mods.TimeDilation;
import com.megacrit.cardcrawl.daily.mods.Vintage;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ModHelper {
    private static HashMap<String, AbstractDailyMod> starterMods = new HashMap();
    private static HashMap<String, AbstractDailyMod> genericMods = new HashMap();
    private static HashMap<String, AbstractDailyMod> difficultyMods = new HashMap();
    private static HashMap<String, AbstractDailyMod> legacyMods = new HashMap();
    public static ArrayList<AbstractDailyMod> enabledMods = new ArrayList();

    public static void initialize() {
        ModHelper.addStarterMod(new Shiny());
        ModHelper.addStarterMod(new Allstar());
        ModHelper.addStarterMod(new Draft());
        ModHelper.addStarterMod(new SealedDeck());
        ModHelper.addStarterMod(new Insanity());
        ModHelper.addStarterMod(new Heirloom());
        ModHelper.addStarterMod(new Specialized());
        ModHelper.addStarterMod(new Chimera());
        ModHelper.addStarterMod(new CursedRun());
        ModHelper.addGenericMod(new Diverse());
        ModHelper.addGenericMod(new RedCards());
        ModHelper.addGenericMod(new GreenCards());
        ModHelper.addGenericMod(new BlueCards());
        ModHelper.addGenericMod(new PurpleCards());
        ModHelper.addGenericMod(new ColorlessCards());
        ModHelper.addGenericMod(new TimeDilation());
        ModHelper.addGenericMod(new Vintage());
        ModHelper.addGenericMod(new Hoarder());
        ModHelper.addGenericMod(new Flight());
        ModHelper.addGenericMod(new CertainFuture());
        ModHelper.addGenericMod(new ControlledChaos());
        ModHelper.addDifficultyMod(new BigGameHunter());
        ModHelper.addDifficultyMod(new Lethality());
        ModHelper.addDifficultyMod(new NightTerrors());
        ModHelper.addDifficultyMod(new Binary());
        ModHelper.addDifficultyMod(new Midas());
        ModHelper.addDifficultyMod(new Terminal());
        ModHelper.addDifficultyMod(new DeadlyEvents());
        ModHelper.addLegacyMod(new Brewmaster());
        ModHelper.addLegacyMod(new Colossus());
    }

    private static void addStarterMod(AbstractDailyMod mod) {
        starterMods.put(mod.modID, mod);
    }

    private static void addGenericMod(AbstractDailyMod mod) {
        genericMods.put(mod.modID, mod);
    }

    private static void addDifficultyMod(AbstractDailyMod mod) {
        difficultyMods.put(mod.modID, mod);
    }

    private static void addLegacyMod(AbstractDailyMod mod) {
        legacyMods.put(mod.modID, mod);
    }

    public static void setMods(List<String> modIDs) {
        ModHelper.setModsFalse();
        for (String m : modIDs) {
            if (m.equals("Endless")) continue;
            enabledMods.add(ModHelper.getMod(m));
        }
    }

    public static AbstractDailyMod getMod(String key) {
        AbstractDailyMod mod = starterMods.get(key);
        if (mod == null) {
            mod = genericMods.get(key);
        }
        if (mod == null) {
            mod = difficultyMods.get(key);
        }
        if (mod == null) {
            mod = legacyMods.get(key);
        }
        return mod;
    }

    public static ArrayList<String> getEnabledModIDs() {
        ArrayList<String> enabled = new ArrayList<String>();
        for (AbstractDailyMod m : enabledMods) {
            if (m == null) continue;
            enabled.add(m.modID);
        }
        return enabled;
    }

    private static void setTheMods(HashMap<String, AbstractDailyMod> modMap, long daysSince1970, AbstractPlayer.PlayerClass characterClass) {
        ArrayList<AbstractDailyMod> shuffledList = new ArrayList<AbstractDailyMod>();
        for (Map.Entry<String, AbstractDailyMod> m : modMap.entrySet()) {
            if (m.getValue().classToExclude == characterClass) continue;
            shuffledList.add(m.getValue());
        }
        int rotationConstant = 5;
        int modSelectionIndex = (int)(daysSince1970 % (long)rotationConstant);
        if (modSelectionIndex < 0) {
            modSelectionIndex += rotationConstant;
        }
        int shuffleInterval = (int)(daysSince1970 / (long)rotationConstant);
        Collections.shuffle(shuffledList, new Random(shuffleInterval));
        enabledMods.add((AbstractDailyMod)shuffledList.get(modSelectionIndex));
    }

    public static void setTodaysMods(long daysSince1970, AbstractPlayer.PlayerClass chosenClass) {
        ModHelper.setModsFalse();
        ModHelper.setTheMods(starterMods, daysSince1970, chosenClass);
        ModHelper.setTheMods(genericMods, daysSince1970, chosenClass);
        ModHelper.setTheMods(difficultyMods, daysSince1970, chosenClass);
    }

    public static boolean isModEnabled(String modID) {
        for (AbstractDailyMod m : enabledMods) {
            if (m == null || m.modID == null || !m.modID.equals(modID)) continue;
            return true;
        }
        return false;
    }

    public static void setModsFalse() {
        enabledMods.clear();
    }

    public static void uploadModData() {
        ArrayList<String> data = new ArrayList<String>();
        for (Map.Entry<String, AbstractDailyMod> m : starterMods.entrySet()) {
            data.add(m.getValue().gameDataUploadData());
        }
        for (Map.Entry<String, AbstractDailyMod> m : genericMods.entrySet()) {
            data.add(m.getValue().gameDataUploadData());
        }
        for (Map.Entry<String, AbstractDailyMod> m : difficultyMods.entrySet()) {
            data.add(m.getValue().gameDataUploadData());
        }
        BotDataUploader.uploadDataAsync(BotDataUploader.GameDataType.DAILY_MOD_DATA, AbstractDailyMod.gameDataUploadHeader(), data);
    }

    public static void clearNulls() {
        Iterator<AbstractDailyMod> m = enabledMods.iterator();
        while (m.hasNext()) {
            AbstractDailyMod derp = m.next();
            if (derp != null) continue;
            m.remove();
        }
    }
}

