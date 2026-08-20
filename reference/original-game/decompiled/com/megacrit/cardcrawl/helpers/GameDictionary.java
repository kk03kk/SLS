/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.Keyword;
import com.megacrit.cardcrawl.localization.KeywordStrings;
import java.util.TreeMap;

public class GameDictionary {
    private static final KeywordStrings keywordStrings = CardCrawlGame.languagePack.getKeywordString("Game Dictionary");
    public static final String[] TEXT = GameDictionary.keywordStrings.TEXT;
    public static final Keyword ARTIFACT = GameDictionary.keywordStrings.ARTIFACT;
    public static final Keyword BLOCK = GameDictionary.keywordStrings.BLOCK;
    public static final Keyword EVOKE = GameDictionary.keywordStrings.EVOKE;
    public static final Keyword CONFUSED = GameDictionary.keywordStrings.CONFUSED;
    public static final Keyword CHANNEL = GameDictionary.keywordStrings.CHANNEL;
    public static final Keyword CURSE = GameDictionary.keywordStrings.CURSE;
    public static final Keyword DARK = GameDictionary.keywordStrings.DARK;
    public static final Keyword DEXTERITY = GameDictionary.keywordStrings.DEXTERITY;
    public static final Keyword ETHEREAL = GameDictionary.keywordStrings.ETHEREAL;
    public static final Keyword EXHAUST = GameDictionary.keywordStrings.EXHAUST;
    public static final Keyword FRAIL = GameDictionary.keywordStrings.FRAIL;
    public static final Keyword FROST = GameDictionary.keywordStrings.FROST;
    public static final Keyword INNATE = GameDictionary.keywordStrings.INNATE;
    public static final Keyword INTANGIBLE = GameDictionary.keywordStrings.INTANGIBLE;
    public static final Keyword FOCUS = GameDictionary.keywordStrings.FOCUS;
    public static final Keyword LIGHTNING = GameDictionary.keywordStrings.LIGHTNING;
    public static final Keyword LOCKED = GameDictionary.keywordStrings.LOCKED;
    public static final Keyword LOCK_ON = GameDictionary.keywordStrings.LOCK_ON;
    public static final Keyword OPENER = GameDictionary.keywordStrings.OPENER;
    public static final Keyword PLASMA = GameDictionary.keywordStrings.PLASMA;
    public static final Keyword POISON = GameDictionary.keywordStrings.POISON;
    public static final Keyword RETAIN = GameDictionary.keywordStrings.RETAIN;
    public static final Keyword SHIV = GameDictionary.keywordStrings.SHIV;
    public static final Keyword STATUS = GameDictionary.keywordStrings.STATUS;
    public static final Keyword STRENGTH = GameDictionary.keywordStrings.STRENGTH;
    public static final Keyword STRIKE = GameDictionary.keywordStrings.STRIKE;
    public static final Keyword TRANSFORM = GameDictionary.keywordStrings.TRANSFORM;
    public static final Keyword UNKNOWN = GameDictionary.keywordStrings.UNKNOWN;
    public static final Keyword UNPLAYABLE = GameDictionary.keywordStrings.UNPLAYABLE;
    public static final Keyword UPGRADE = GameDictionary.keywordStrings.UPGRADE;
    public static final Keyword VIGOR = GameDictionary.keywordStrings.VIGOR;
    public static final Keyword VOID = GameDictionary.keywordStrings.VOID;
    public static final Keyword VULNERABLE = GameDictionary.keywordStrings.VULNERABLE;
    public static final Keyword WEAK = GameDictionary.keywordStrings.WEAK;
    public static final Keyword WOUND = GameDictionary.keywordStrings.WOUND;
    public static final Keyword DAZED = GameDictionary.keywordStrings.DAZED;
    public static final Keyword BURN = GameDictionary.keywordStrings.BURN;
    public static final Keyword THORNS = GameDictionary.keywordStrings.THORNS;
    public static final Keyword STANCE = GameDictionary.keywordStrings.STANCE;
    public static final Keyword WRATH = GameDictionary.keywordStrings.WRATH;
    public static final Keyword CALM = GameDictionary.keywordStrings.CALM;
    public static final Keyword ENLIGHTENMENT = GameDictionary.keywordStrings.DIVINITY;
    public static final Keyword SCRY = GameDictionary.keywordStrings.SCRY;
    public static final Keyword PRAYER = GameDictionary.keywordStrings.PRAYER;
    public static final Keyword REGEN = GameDictionary.keywordStrings.REGEN;
    public static final Keyword RITUAL = GameDictionary.keywordStrings.RITUAL;
    public static final Keyword FATAL = GameDictionary.keywordStrings.FATAL;
    public static final TreeMap<String, String> keywords = new TreeMap();
    public static final TreeMap<String, String> parentWord = new TreeMap();

    public static void initialize() {
        keywords.put("[R]", TEXT[0]);
        keywords.put("[G]", TEXT[0]);
        keywords.put("[B]", TEXT[0]);
        keywords.put("[W]", TEXT[0]);
        keywords.put("[E]", TEXT[0]);
        GameDictionary.createDictionaryEntry(GameDictionary.ARTIFACT.NAMES, GameDictionary.ARTIFACT.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.BLOCK.NAMES, GameDictionary.BLOCK.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.BURN.NAMES, GameDictionary.BURN.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.CALM.NAMES, GameDictionary.CALM.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.CHANNEL.NAMES, GameDictionary.CHANNEL.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.CONFUSED.NAMES, GameDictionary.CONFUSED.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.CURSE.NAMES, GameDictionary.CURSE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.DARK.NAMES, GameDictionary.DARK.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.DAZED.NAMES, GameDictionary.DAZED.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.DEXTERITY.NAMES, GameDictionary.DEXTERITY.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.ENLIGHTENMENT.NAMES, GameDictionary.ENLIGHTENMENT.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.ETHEREAL.NAMES, GameDictionary.ETHEREAL.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.EVOKE.NAMES, GameDictionary.EVOKE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.EXHAUST.NAMES, GameDictionary.EXHAUST.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.FOCUS.NAMES, GameDictionary.FOCUS.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.FRAIL.NAMES, GameDictionary.FRAIL.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.FROST.NAMES, GameDictionary.FROST.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.INNATE.NAMES, GameDictionary.INNATE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.INTANGIBLE.NAMES, GameDictionary.INTANGIBLE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.LIGHTNING.NAMES, GameDictionary.LIGHTNING.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.LOCK_ON.NAMES, GameDictionary.LOCK_ON.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.LOCKED.NAMES, GameDictionary.LOCKED.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.OPENER.NAMES, GameDictionary.OPENER.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.PLASMA.NAMES, GameDictionary.PLASMA.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.POISON.NAMES, GameDictionary.POISON.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.PRAYER.NAMES, GameDictionary.PRAYER.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.RETAIN.NAMES, GameDictionary.RETAIN.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.SCRY.NAMES, GameDictionary.SCRY.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.SHIV.NAMES, GameDictionary.SHIV.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.STANCE.NAMES, GameDictionary.STANCE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.STATUS.NAMES, GameDictionary.STATUS.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.STRENGTH.NAMES, GameDictionary.STRENGTH.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.STRIKE.NAMES, GameDictionary.STRIKE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.THORNS.NAMES, GameDictionary.THORNS.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.TRANSFORM.NAMES, GameDictionary.TRANSFORM.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.UNKNOWN.NAMES, GameDictionary.UNKNOWN.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.UNPLAYABLE.NAMES, GameDictionary.UNPLAYABLE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.UPGRADE.NAMES, GameDictionary.UPGRADE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.VIGOR.NAMES, GameDictionary.VIGOR.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.VOID.NAMES, GameDictionary.VOID.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.VULNERABLE.NAMES, GameDictionary.VULNERABLE.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.WEAK.NAMES, GameDictionary.WEAK.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.WOUND.NAMES, GameDictionary.WOUND.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.WRATH.NAMES, GameDictionary.WRATH.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.REGEN.NAMES, GameDictionary.REGEN.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.RITUAL.NAMES, GameDictionary.RITUAL.DESCRIPTION);
        GameDictionary.createDictionaryEntry(GameDictionary.FATAL.NAMES, GameDictionary.FATAL.DESCRIPTION);
    }

    private static void createDictionaryEntry(String[] names, String desc) {
        for (String n : names) {
            keywords.put(n, desc);
            parentWord.put(n, names[0]);
        }
    }
}

