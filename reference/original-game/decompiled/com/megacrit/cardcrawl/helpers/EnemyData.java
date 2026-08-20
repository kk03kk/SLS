/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.helpers.GameDataStringBuilder;

public class EnemyData {
    public String name;
    public int level;
    public MonsterType type;

    public EnemyData(String key, int level, MonsterType type) {
        this.name = key;
        this.level = level;
        this.type = type;
    }

    public static String gameDataUploadHeader() {
        GameDataStringBuilder builder = new GameDataStringBuilder();
        builder.addFieldData("name");
        builder.addFieldData("level");
        builder.addFieldData("type");
        return builder.toString();
    }

    public String gameDataUploadData() {
        GameDataStringBuilder builder = new GameDataStringBuilder();
        builder.addFieldData(this.name);
        builder.addFieldData(this.level);
        builder.addFieldData(this.type.name());
        return builder.toString();
    }

    public static enum MonsterType {
        WEAK,
        STRONG,
        ELITE,
        BOSS,
        EVENT;

    }
}

