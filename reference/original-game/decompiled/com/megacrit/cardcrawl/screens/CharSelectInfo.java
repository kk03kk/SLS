/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CharSelectInfo {
    public String name;
    public String flavorText;
    public String hp;
    public int gold;
    public int currentHp;
    public int maxHp;
    public int maxOrbs;
    public int cardDraw;
    public int floorNum;
    public String levelName;
    public long saveDate;
    public AbstractPlayer player;
    public String deckString;
    public ArrayList<String> relics;
    public ArrayList<String> deck;
    public boolean resumeGame;
    public boolean isHardMode;

    public CharSelectInfo(String name, String flavorText, int currentHp, int maxHp, int maxOrbs, int gold, int cardDraw, AbstractPlayer player, ArrayList<String> relics, ArrayList<String> deck, boolean resumeGame) {
        this.name = name;
        this.flavorText = flavorText;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.maxOrbs = maxOrbs;
        this.hp = Integer.toString(currentHp) + "/" + Integer.toString(maxHp);
        this.gold = gold;
        this.cardDraw = cardDraw;
        this.relics = relics;
        this.deck = deck;
        this.player = player;
        this.resumeGame = resumeGame;
        if (!resumeGame) {
            this.setDeck();
        }
    }

    public CharSelectInfo(String name, String fText, int currentHp, int maxHp, int maxOrbs, int gold, int cardDraw, AbstractPlayer player, ArrayList<String> relics, ArrayList<String> deck, long saveDate, int floorNum, String levelName, boolean isHardMode) {
        this(name, fText, currentHp, maxHp, maxOrbs, gold, cardDraw, player, relics, deck, true);
        this.isHardMode = isHardMode;
        this.saveDate = saveDate;
        this.floorNum = floorNum;
        this.levelName = levelName;
    }

    private void setDeck() {
        this.deckString = CharSelectInfo.createDeckInfoString(this.player.getStartingDeck());
    }

    public static String createDeckInfoString(ArrayList<String> deck) {
        HashMap<String, Integer> cards = new HashMap<String, Integer>();
        for (String string : deck) {
            if (!cards.containsKey(string)) {
                cards.put(string, 1);
                continue;
            }
            cards.put(string, (Integer)cards.get(string) + 1);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : cards.entrySet()) {
            sb.append("#b").append(entry.getValue()).append(" ").append((String)entry.getKey());
            if ((Integer)entry.getValue() > 1) {
                sb.append("s");
            }
            sb.append(", ");
        }
        String string = sb.toString();
        if (string.length() > 80) {
            return "Click the deck icon to view starting cards.";
        }
        return string.substring(0, string.length() - 2);
    }
}

