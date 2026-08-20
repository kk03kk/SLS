/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;
import com.megacrit.cardcrawl.vfx.combat.OrbFlareEffect;

public class LightningOrbPassiveAction
extends AbstractGameAction {
    private DamageInfo info;
    private AbstractOrb orb;
    private boolean hitAll;

    public LightningOrbPassiveAction(DamageInfo info, AbstractOrb orb, boolean hitAll) {
        this.info = info;
        this.orb = orb;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = AbstractGameAction.AttackEffect.NONE;
        this.hitAll = hitAll;
    }

    @Override
    public void update() {
        if (!this.hitAll) {
            AbstractMonster m = AbstractDungeon.getRandomMonster();
            if (m != null) {
                float speedTime = 0.2f / (float)AbstractDungeon.player.orbs.size();
                if (Settings.FAST_MODE) {
                    speedTime = 0.0f;
                }
                this.info.output = AbstractOrb.applyLockOn(m, this.info.base);
                this.addToTop(new DamageAction(m, this.info, AbstractGameAction.AttackEffect.NONE, true));
                this.addToTop(new VFXAction(new LightningEffect(m.drawX, m.drawY), speedTime));
                this.addToTop(new SFXAction("ORB_LIGHTNING_EVOKE"));
                if (this.orb != null) {
                    this.addToTop(new VFXAction(new OrbFlareEffect(this.orb, OrbFlareEffect.OrbFlareColor.LIGHTNING), speedTime));
                }
            }
        } else {
            float speedTime = 0.2f / (float)AbstractDungeon.player.orbs.size();
            if (Settings.FAST_MODE) {
                speedTime = 0.0f;
            }
            this.addToTop(new DamageAllEnemiesAction((AbstractCreature)AbstractDungeon.player, DamageInfo.createDamageMatrix(this.info.base, true, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.NONE));
            for (AbstractMonster m3 : AbstractDungeon.getMonsters().monsters) {
                if (m3.isDeadOrEscaped() || m3.halfDead) continue;
                this.addToTop(new VFXAction(new LightningEffect(m3.drawX, m3.drawY), speedTime));
            }
            this.addToTop(new SFXAction("ORB_LIGHTNING_EVOKE"));
            if (this.orb != null) {
                this.addToTop(new VFXAction(new OrbFlareEffect(this.orb, OrbFlareEffect.OrbFlareColor.LIGHTNING), speedTime));
            }
        }
        this.isDone = true;
    }
}

