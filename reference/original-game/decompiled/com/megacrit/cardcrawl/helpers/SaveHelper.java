/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.AsyncSaver;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.integrations.DistributorFactory;
import com.megacrit.cardcrawl.rooms.TrueVictoryRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.screens.mainMenu.SaveSlotScreen;
import com.megacrit.cardcrawl.vfx.GameSavedEffect;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SaveHelper {
    private static final Logger logger = LogManager.getLogger(SaveHelper.class.getName());

    public static void initialize() {
    }

    private static Boolean isGog() {
        return CardCrawlGame.publisherIntegration.getType() == DistributorFactory.Distributor.GOG;
    }

    private static String getSaveDir() {
        if (Settings.isBeta || SaveHelper.isGog().booleanValue()) {
            return "betaPreferences";
        }
        return "preferences";
    }

    public static boolean doesPrefExist(String name) {
        return Gdx.files.local(SaveHelper.getSaveDir() + File.separator + name).exists();
    }

    public static void deletePrefs(int slot) {
        String dir = SaveHelper.getSaveDir() + File.separator;
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSDataVagabond", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSDataTheSilent", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSDataDefect", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSDataWatcher", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSAchievements", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSDaily", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSSeenBosses", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSSeenCards", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSBetaCardPreference", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSSeenRelics", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSUnlockProgress", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSUnlocks", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSGameplaySettings", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSInputSettings", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSInputSettings_Controller", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSSound", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSPlayer", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("STSTips", slot));
        dir = "runs" + File.separator;
        SaveHelper.deleteFolder(dir + SaveHelper.slotName("IRONCLAD", slot));
        SaveHelper.deleteFolder(dir + SaveHelper.slotName("THE_SILENT", slot));
        SaveHelper.deleteFolder(dir + SaveHelper.slotName("DEFECT", slot));
        SaveHelper.deleteFolder(dir + SaveHelper.slotName("WATCHER", slot));
        SaveHelper.deleteFolder(dir + SaveHelper.slotName("DAILY", slot));
        dir = "saves" + File.separator;
        SaveHelper.deleteFile(dir + SaveHelper.slotName("IRONCLAD.autosave", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("DEFECT.autosave", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("THE_SILENT.autosave", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("WATCHER.autosave", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("IRONCLAD.autosave.backUp", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("DEFECT.autosave.backUp", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("THE_SILENT.autosave.backUp", slot));
        SaveHelper.deleteFile(dir + SaveHelper.slotName("WATCHER.autosave.backUp", slot));
        if (Settings.isBeta || SaveHelper.isGog().booleanValue()) {
            SaveHelper.deleteFile(dir + SaveHelper.slotName("IRONCLAD.autosaveBETA", slot));
            SaveHelper.deleteFile(dir + SaveHelper.slotName("DEFECT.autosaveBETA", slot));
            SaveHelper.deleteFile(dir + SaveHelper.slotName("THE_SILENT.autosaveBETA", slot));
            SaveHelper.deleteFile(dir + SaveHelper.slotName("WATCHER.autosaveBETA", slot));
            SaveHelper.deleteFile(dir + SaveHelper.slotName("IRONCLAD.autosaveBETA.backUp", slot));
            SaveHelper.deleteFile(dir + SaveHelper.slotName("DEFECT.autosaveBETA.backUp", slot));
            SaveHelper.deleteFile(dir + SaveHelper.slotName("THE_SILENT.autosaveBETA.backUp", slot));
            SaveHelper.deleteFile(dir + SaveHelper.slotName("WATCHER.autosaveBETA.backUp", slot));
        }
        CardCrawlGame.saveSlotPref.putString(SaveHelper.slotName("PROFILE_NAME", slot), "");
        CardCrawlGame.saveSlotPref.putFloat(SaveHelper.slotName("COMPLETION", slot), 0.0f);
        CardCrawlGame.saveSlotPref.putLong(SaveHelper.slotName("PLAYTIME", slot), 0L);
        CardCrawlGame.saveSlotPref.flush();
        if (slot == CardCrawlGame.saveSlot || CardCrawlGame.saveSlot == -1) {
            String name = "";
            boolean newDefaultSet = false;
            for (int i = 0; i < 3; ++i) {
                name = CardCrawlGame.saveSlotPref.getString(SaveHelper.slotName("PROFILE_NAME", i), "");
                if (name.equals("")) continue;
                logger.info("Current slot deleted, DEFAULT_SLOT is now " + i);
                CardCrawlGame.saveSlotPref.putInteger("DEFAULT_SLOT", i);
                newDefaultSet = true;
                SaveSlotScreen.slotDeleted = true;
                break;
            }
            if (!newDefaultSet) {
                logger.info("All slots deleted, DEFAULT_SLOT is now -1");
                CardCrawlGame.saveSlotPref.putInteger("DEFAULT_SLOT", -1);
            }
            CardCrawlGame.saveSlotPref.flush();
        }
    }

    private static void deleteFile(String fileName) {
        logger.info("Deleting " + fileName);
        if (Gdx.files.local(fileName).delete()) {
            logger.info(fileName + " deleted.");
        }
        if (Gdx.files.local(fileName + ".backUp").delete()) {
            logger.info(fileName + ".backUp deleted.");
        }
    }

    private static void deleteFolder(String dirName) {
        logger.info("Deleting " + dirName);
        if (Gdx.files.local(dirName).deleteDirectory()) {
            logger.info(dirName + " deleted.");
        }
    }

    public static String slotName(String name, int slot) {
        switch (slot) {
            case 0: {
                break;
            }
            default: {
                name = slot + "_" + name;
            }
        }
        return name;
    }

    public static Prefs getPrefs(String name) {
        switch (CardCrawlGame.saveSlot) {
            case 0: {
                break;
            }
            default: {
                name = CardCrawlGame.saveSlot + "_" + name;
            }
        }
        Gson gson = new Gson();
        Prefs retVal = new Prefs();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        String filepath = SaveHelper.getSaveDir() + File.separator + name;
        String jsonStr = null;
        try {
            jsonStr = SaveHelper.loadJson(filepath);
            if (jsonStr.isEmpty()) {
                logger.error("Empty Pref file: name=" + name + ", filepath=" + filepath);
                SaveHelper.handleCorruption(jsonStr, filepath, name);
                retVal = SaveHelper.getPrefs(name);
            } else {
                retVal.data = (Map)gson.fromJson(jsonStr, type);
            }
        }
        catch (JsonSyntaxException e) {
            logger.error("Corrupt Pref file", (Throwable)e);
            SaveHelper.handleCorruption(jsonStr, filepath, name);
            retVal = SaveHelper.getPrefs(name);
        }
        retVal.filepath = filepath;
        return retVal;
    }

    private static void handleCorruption(String jsonStr, String filepath, String name) {
        SaveHelper.preserveCorruptFile(filepath);
        FileHandle backup = Gdx.files.local(filepath + ".backUp");
        if (backup.exists()) {
            backup.moveTo(Gdx.files.local(filepath));
            logger.info("Original corrupted, backup loaded for " + filepath);
        }
    }

    public static void preserveCorruptFile(String filePath) {
        FileHandle file = Gdx.files.local(filePath);
        FileHandle corruptFile = Gdx.files.local("sendToDevs" + File.separator + filePath + ".corrupt");
        file.moveTo(corruptFile);
    }

    private static String loadJson(String filepath) {
        if (Gdx.files.local(filepath).exists()) {
            return Gdx.files.local(filepath).readString(String.valueOf(StandardCharsets.UTF_8));
        }
        HashMap map = new HashMap();
        Gson gson = new Gson();
        AsyncSaver.save(filepath, gson.toJson(map));
        return "{}";
    }

    public static boolean saveExists() {
        StringBuilder retVal = new StringBuilder();
        retVal.append(SaveHelper.getSaveDir()).append(File.separator);
        switch (CardCrawlGame.saveSlot) {
            case 0: {
                retVal.append("STSPlayer");
                break;
            }
            case 1: 
            case 2: 
            case 3: {
                retVal.append(CardCrawlGame.saveSlot).append("_STSPlayer");
                break;
            }
            default: {
                retVal.append("STSPlayer");
            }
        }
        return Gdx.files.local(retVal.toString()).exists();
    }

    public static void saveIfAppropriate(SaveFile.SaveType saveType) {
        if (!SaveHelper.shouldSave()) {
            return;
        }
        SaveFile saveFile = new SaveFile(saveType);
        SaveAndContinue.save(saveFile);
        AbstractDungeon.effectList.add(new GameSavedEffect());
    }

    public static boolean shouldSave() {
        if (AbstractDungeon.nextRoom != null && AbstractDungeon.nextRoom.getRoom() instanceof TrueVictoryRoom) {
            return false;
        }
        return !Settings.isDemo;
    }

    public static boolean shouldDeleteSave() {
        return !Settings.isDemo;
    }
}

