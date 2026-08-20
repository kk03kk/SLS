/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import com.megacrit.cardcrawl.vfx.combat.PowerBuffEffect;
import com.megacrit.cardcrawl.vfx.combat.PowerDebuffEffect;
import java.util.Collections;

@Deprecated
public class ApplyPoisonOnRandomMonsterAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ApplyPowerAction");
    public static final String[] TEXT = ApplyPoisonOnRandomMonsterAction.uiStrings.TEXT;
    private float startingDuration;
    private AbstractPower powerToApply;

    public ApplyPoisonOnRandomMonsterAction(AbstractCreature source, int stackAmount, boolean isFast, AbstractGameAction.AttackEffect effect) {
        this.startingDuration = Settings.FAST_MODE ? 0.1f : (isFast ? Settings.ACTION_DUR_FASTER : Settings.ACTION_DUR_FAST);
        this.duration = this.startingDuration;
        this.target = null;
        this.source = source;
        this.amount = stackAmount;
        this.actionType = AbstractGameAction.ActionType.POWER;
        this.attackEffect = effect;
        if (AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.duration = 0.0f;
            this.startingDuration = 0.0f;
            this.isDone = true;
        }
    }

    /*
     * WARNING - void declaration
     */
    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            this.target = AbstractDungeon.getRandomMonster();
            if (this.target == null) {
                this.isDone = true;
                return;
            }
            this.powerToApply = new PoisonPower(this.target, this.source, this.amount);
            if (this.source != null) {
                for (AbstractPower abstractPower : this.source.powers) {
                    abstractPower.onApplyPower(this.powerToApply, this.target, this.source);
                }
            }
            if (this.target.hasPower("Artifact")) {
                this.addToTop(new TextAboveCreatureAction(this.target, TEXT[0]));
                this.duration -= Gdx.graphics.getDeltaTime();
                CardCrawlGame.sound.play("NULLIFY_SFX");
                this.target.getPower("Artifact").flashWithoutSound();
                this.target.getPower("Artifact").onSpecificTrigger();
                return;
            }
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, this.attackEffect));
            boolean hasBuffAlready = false;
            for (AbstractPower p : this.target.powers) {
                if (!p.ID.equals(this.powerToApply.ID) || p.ID.equals("Night Terror")) continue;
                p.stackPower(this.amount);
                p.flash();
                if ((p instanceof StrengthPower || p instanceof DexterityPower) && this.amount <= 0) {
                    AbstractDungeon.effectList.add(new PowerDebuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, this.powerToApply.name + TEXT[3]));
                } else if (this.amount > 0) {
                    if (p.type == AbstractPower.PowerType.BUFF || p instanceof StrengthPower || p instanceof DexterityPower) {
                        AbstractDungeon.effectList.add(new PowerBuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, "+" + Integer.toString(this.amount) + " " + this.powerToApply.name));
                    } else {
                        AbstractDungeon.effectList.add(new PowerDebuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, "+" + Integer.toString(this.amount) + " " + this.powerToApply.name));
                    }
                } else if (p.type == AbstractPower.PowerType.BUFF) {
                    AbstractDungeon.effectList.add(new PowerBuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, this.powerToApply.name + TEXT[3]));
                } else {
                    AbstractDungeon.effectList.add(new PowerDebuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, this.powerToApply.name + TEXT[3]));
                }
                p.updateDescription();
                hasBuffAlready = true;
                AbstractDungeon.onModifyPower();
            }
            if (this.powerToApply.type == AbstractPower.PowerType.DEBUFF) {
                this.target.useFastShakeAnimation(0.5f);
            }
            if (!hasBuffAlready) {
                this.target.powers.add(this.powerToApply);
                Collections.sort(this.target.powers);
                this.powerToApply.onInitialApplication();
                this.powerToApply.flash();
                if (this.amount < 0 && (this.powerToApply.ID.equals("Strength") || this.powerToApply.ID.equals("Dexterity") || this.powerToApply.ID.equals("Focus"))) {
                    AbstractDungeon.effectList.add(new PowerDebuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, this.powerToApply.name + TEXT[3]));
                } else if (this.powerToApply.type == AbstractPower.PowerType.BUFF) {
                    AbstractDungeon.effectList.add(new PowerBuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, this.powerToApply.name));
                } else {
                    AbstractDungeon.effectList.add(new PowerDebuffEffect(this.target.hb.cX - this.target.animX, this.target.hb.cY + this.target.hb.height / 2.0f, this.powerToApply.name));
                }
                AbstractDungeon.onModifyPower();
                if (this.target.isPlayer) {
                    void var2_6;
                    boolean bl = false;
                    for (AbstractPower p : this.target.powers) {
                        if (p.type != AbstractPower.PowerType.BUFF) continue;
                        ++var2_6;
                    }
                    if (var2_6 >= 10) {
                        UnlockTracker.unlockAchievement("POWERFUL");
                    }
                }
            }
        }
        this.tickDuration();
    }
}

