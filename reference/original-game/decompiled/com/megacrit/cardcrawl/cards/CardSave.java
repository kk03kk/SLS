/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards;

public class CardSave {
    public int upgrades;
    public int misc;
    public String id;

    public CardSave(String cardID, int timesUpgraded, int misc) {
        this.id = cardID;
        this.upgrades = timesUpgraded;
        this.misc = misc;
    }
}

