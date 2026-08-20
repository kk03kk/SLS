/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters;

import com.megacrit.cardcrawl.core.Settings;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonsterInfo
implements Comparable<MonsterInfo> {
    private static final Logger logger = LogManager.getLogger(MonsterInfo.class.getName());
    public String name;
    public float weight;

    public MonsterInfo(String name, float weight) {
        this.name = name;
        this.weight = weight;
    }

    public static void normalizeWeights(ArrayList<MonsterInfo> list) {
        Collections.sort(list);
        float total = 0.0f;
        for (MonsterInfo i : list) {
            total += i.weight;
        }
        for (MonsterInfo i : list) {
            i.weight /= total;
            if (!Settings.isInfo) continue;
            logger.info(i.name + ": " + i.weight + "%");
        }
    }

    public static String roll(ArrayList<MonsterInfo> list, float roll) {
        float currentWeight = 0.0f;
        for (MonsterInfo i : list) {
            if (!(roll < (currentWeight += i.weight))) continue;
            return i.name;
        }
        return "ERROR";
    }

    @Override
    public int compareTo(MonsterInfo other) {
        return Float.compare(this.weight, other.weight);
    }
}

