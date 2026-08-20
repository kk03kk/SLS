/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;

public class EffectHelper {
    public static void gainGold(AbstractCreature target, float srcX, float srcY, int amount) {
        for (int i = 0; i < amount; ++i) {
            AbstractDungeon.effectList.add(new GainPennyEffect(target, srcX, srcY, target.hb.cX, target.hb.cY, true));
        }
    }
}

