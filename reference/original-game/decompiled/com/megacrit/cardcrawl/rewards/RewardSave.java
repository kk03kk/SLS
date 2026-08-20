/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rewards;

public class RewardSave {
    public String type;
    public String id;
    public int amount;
    public int bonusGold;

    public RewardSave(String type, String id, int amount, int bonusGold) {
        this.type = type;
        this.id = id;
        this.amount = amount;
    }

    public RewardSave(String type, String id) {
        this(type, id, 0, 0);
    }
}

