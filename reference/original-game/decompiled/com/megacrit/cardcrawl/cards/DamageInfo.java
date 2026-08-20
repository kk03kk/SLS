/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DamageInfo {
    public AbstractCreature owner;
    public String name;
    public DamageType type;
    public int base;
    public int output;
    public boolean isModified = false;

    public DamageInfo(AbstractCreature damageSource, int base, DamageType type) {
        this.owner = damageSource;
        this.type = type;
        this.base = base;
        this.output = base;
    }

    public DamageInfo(AbstractCreature owner, int base) {
        this(owner, base, DamageType.NORMAL);
    }

    public void applyPowers(AbstractCreature owner, AbstractCreature target) {
        this.output = this.base;
        this.isModified = false;
        float tmp = this.output;
        if (!owner.isPlayer) {
            float mod;
            if (Settings.isEndless && AbstractDungeon.player.hasBlight("DeadlyEnemies") && this.base != (int)(tmp *= (mod = AbstractDungeon.player.getBlight("DeadlyEnemies").effectFloat()))) {
                this.isModified = true;
            }
            for (AbstractPower p : owner.powers) {
                tmp = p.atDamageGive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            for (AbstractPower p : target.powers) {
                tmp = p.atDamageReceive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            if (this.base != (int)(tmp = AbstractDungeon.player.stance.atDamageReceive(tmp, this.type))) {
                this.isModified = true;
            }
            for (AbstractPower p : owner.powers) {
                tmp = p.atDamageFinalGive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            for (AbstractPower p : target.powers) {
                tmp = p.atDamageFinalReceive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            this.output = MathUtils.floor(tmp);
            if (this.output < 0) {
                this.output = 0;
            }
        } else {
            for (AbstractPower p : owner.powers) {
                tmp = p.atDamageGive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            if (this.base != (int)(tmp = AbstractDungeon.player.stance.atDamageGive(tmp, this.type))) {
                this.isModified = true;
            }
            for (AbstractPower p : target.powers) {
                tmp = p.atDamageReceive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            for (AbstractPower p : owner.powers) {
                tmp = p.atDamageFinalGive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            for (AbstractPower p : target.powers) {
                tmp = p.atDamageFinalReceive(tmp, this.type);
                if (this.base == (int)tmp) continue;
                this.isModified = true;
            }
            this.output = MathUtils.floor(tmp);
            if (this.output < 0) {
                this.output = 0;
            }
        }
    }

    public void applyEnemyPowersOnly(AbstractCreature target) {
        this.output = this.base;
        this.isModified = false;
        float tmp = this.output;
        for (AbstractPower p : target.powers) {
            tmp = p.atDamageReceive(this.output, this.type);
            if (this.base == this.output) continue;
            this.isModified = true;
        }
        for (AbstractPower p : target.powers) {
            tmp = p.atDamageFinalReceive(this.output, this.type);
            if (this.base == this.output) continue;
            this.isModified = true;
        }
        if (tmp < 0.0f) {
            tmp = 0.0f;
        }
        this.output = MathUtils.floor(tmp);
    }

    public static int[] createDamageMatrix(int baseDamage) {
        return DamageInfo.createDamageMatrix(baseDamage, false);
    }

    public static int[] createDamageMatrix(int baseDamage, boolean isPureDamage) {
        int[] retVal = new int[AbstractDungeon.getMonsters().monsters.size()];
        for (int i = 0; i < retVal.length; ++i) {
            DamageInfo info = new DamageInfo(AbstractDungeon.player, baseDamage);
            if (!isPureDamage) {
                info.applyPowers(AbstractDungeon.player, AbstractDungeon.getMonsters().monsters.get(i));
            }
            retVal[i] = info.output;
        }
        return retVal;
    }

    public static int[] createDamageMatrix(int baseDamage, boolean isPureDamage, boolean isOrbDamage) {
        int[] retVal = new int[AbstractDungeon.getMonsters().monsters.size()];
        for (int i = 0; i < retVal.length; ++i) {
            DamageInfo info = new DamageInfo(AbstractDungeon.player, baseDamage);
            if (isOrbDamage && AbstractDungeon.getMonsters().monsters.get(i).hasPower("Lockon")) {
                info.output = (int)((float)info.base * 1.5f);
            }
            retVal[i] = info.output;
        }
        return retVal;
    }

    public static enum DamageType {
        NORMAL,
        THORNS,
        HP_LOSS;

    }
}

