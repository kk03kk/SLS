/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.characters;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Defect;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.characters.TheSilent;
import com.megacrit.cardcrawl.characters.Watcher;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CharacterManager {
    private static final Logger logger = LogManager.getLogger(CharacterManager.class.getName());
    private static ArrayList<AbstractPlayer> masterCharacterList = new ArrayList();

    public CharacterManager() {
        if (masterCharacterList.isEmpty()) {
            masterCharacterList.add(new Ironclad(CardCrawlGame.playerName));
            masterCharacterList.add(new TheSilent(CardCrawlGame.playerName));
            masterCharacterList.add(new Defect(CardCrawlGame.playerName));
            masterCharacterList.add(new Watcher(CardCrawlGame.playerName));
        } else {
            for (AbstractPlayer c : masterCharacterList) {
                c.loadPrefs();
            }
        }
    }

    public AbstractPlayer setChosenCharacter(AbstractPlayer.PlayerClass c) {
        for (AbstractPlayer character : masterCharacterList) {
            if (character.chosenClass != c) continue;
            AbstractDungeon.player = character;
            return character;
        }
        logger.error("The character " + c.name() + " does not exist in the CharacterManager's master character list");
        return null;
    }

    public boolean anySaveFileExists() {
        for (AbstractPlayer character : masterCharacterList) {
            if (!character.saveFileExists()) continue;
            return true;
        }
        return false;
    }

    public AbstractPlayer loadChosenCharacter() {
        for (AbstractPlayer character : masterCharacterList) {
            if (!character.saveFileExists()) continue;
            AbstractDungeon.player = character;
            return character;
        }
        logger.info("No character save file was found!");
        return null;
    }

    public ArrayList<CharStat> getAllCharacterStats() {
        ArrayList<CharStat> allCharStats = new ArrayList<CharStat>();
        for (AbstractPlayer c : masterCharacterList) {
            allCharStats.add(c.getCharStat());
        }
        return allCharStats;
    }

    public void refreshAllCharStats() {
        for (AbstractPlayer c : masterCharacterList) {
            c.refreshCharStat();
        }
    }

    public ArrayList<Prefs> getAllPrefs() {
        ArrayList<Prefs> allPrefs = new ArrayList<Prefs>();
        for (AbstractPlayer c : masterCharacterList) {
            allPrefs.add(c.getPrefs());
        }
        return allPrefs;
    }

    public AbstractPlayer getRandomCharacter(Random rng) {
        int index = rng.random(masterCharacterList.size() - 1);
        return masterCharacterList.get(index);
    }

    public AbstractPlayer recreateCharacter(AbstractPlayer.PlayerClass p) {
        for (AbstractPlayer old : masterCharacterList) {
            if (old.chosenClass != p) continue;
            AbstractPlayer newChar = old.newInstance();
            masterCharacterList.set(masterCharacterList.indexOf(old), newChar);
            old.dispose();
            logger.info("Successfully recreated " + newChar.chosenClass.name());
            return newChar;
        }
        return null;
    }

    public AbstractPlayer getCharacter(AbstractPlayer.PlayerClass c) {
        for (AbstractPlayer character : masterCharacterList) {
            if (character.chosenClass != c) continue;
            return character;
        }
        logger.error("The character " + c.name() + " does not exist in the CharacterManager's master character list");
        return null;
    }

    public ArrayList<AbstractPlayer> getAllCharacters() {
        return masterCharacterList;
    }
}

