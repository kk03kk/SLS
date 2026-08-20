/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateHopAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.unique.VampireDamageAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.vfx.combat.BiteEffect;

public class ShelledParasite
extends AbstractMonster {
    public static final String ID = "Shelled Parasite";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Shelled Parasite");
    public static final String NAME = ShelledParasite.monsterStrings.NAME;
    public static final String[] MOVES = ShelledParasite.monsterStrings.MOVES;
    public static final String[] DIALOG = ShelledParasite.monsterStrings.DIALOG;
    private static final int HP_MIN = 68;
    private static final int HP_MAX = 72;
    private static final int A_2_HP_MIN = 70;
    private static final int A_2_HP_MAX = 75;
    private static final float HB_X_F = 20.0f;
    private static final float HB_Y_F = -6.0f;
    private static final float HB_W = 350.0f;
    private static final float HB_H = 260.0f;
    private static final int PLATED_ARMOR_AMT = 14;
    private static final int FELL_DMG = 18;
    private static final int DOUBLE_STRIKE_DMG = 6;
    private static final int SUCK_DMG = 10;
    private static final int A_2_FELL_DMG = 21;
    private static final int A_2_DOUBLE_STRIKE_DMG = 7;
    private static final int A_2_SUCK_DMG = 12;
    private int fellDmg;
    private int doubleStrikeDmg;
    private int suckDmg;
    private static final int DOUBLE_STRIKE_COUNT = 2;
    private static final int FELL_FRAIL_AMT = 2;
    private static final byte FELL = 1;
    private static final byte DOUBLE_STRIKE = 2;
    private static final byte LIFE_SUCK = 3;
    private static final byte STUNNED = 4;
    private boolean firstMove = true;
    public static final String ARMOR_BREAK = "ARMOR_BREAK";

    public ShelledParasite(float x, float y) {
        super(NAME, ID, 72, 20.0f, -6.0f, 350.0f, 260.0f, null, x, y);
        this.loadAnimation("images/monsters/theCity/shellMonster/skeleton.atlas", "images/monsters/theCity/shellMonster/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.2f);
        e.setTimeScale(0.8f);
        this.dialogX = -50.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(70, 75);
        } else {
            this.setHp(68, 72);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.doubleStrikeDmg = 7;
            this.fellDmg = 21;
            this.suckDmg = 12;
        } else {
            this.doubleStrikeDmg = 6;
            this.fellDmg = 18;
            this.suckDmg = 10;
        }
        this.damage.add(new DamageInfo(this, this.doubleStrikeDmg));
        this.damage.add(new DamageInfo(this, this.fellDmg));
        this.damage.add(new DamageInfo(this, this.suckDmg));
    }

    public ShelledParasite() {
        this(-20.0f, 10.0f);
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new PlatedArmorPower(this, 14)));
        AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, 14));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, 2, true), 2));
                break;
            }
            case 2: {
                for (int i = 0; i < 2; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new AnimateHopAction(this));
                    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.2f));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
                }
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4f));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BiteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-25.0f, 25.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-25.0f, 25.0f) * Settings.scale, Color.GOLD.cpy()), 0.0f));
                AbstractDungeon.actionManager.addToBottom(new VampireDamageAction(AbstractDungeon.player, (DamageInfo)this.damage.get(2), AbstractGameAction.AttackEffect.NONE));
                break;
            }
            case 4: {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
                AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction((AbstractCreature)this, TextAboveCreatureAction.TextType.STUNNED));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    public void changeState(String stateName) {
        switch (stateName) {
            case "ATTACK": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
            case "ARMOR_BREAK": {
                AbstractDungeon.actionManager.addToBottom(new AnimateHopAction(this));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new AnimateHopAction(this));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new AnimateHopAction(this));
                this.setMove((byte)4, AbstractMonster.Intent.STUN);
                this.createIntent();
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    protected void getMove(int num) {
        if (this.firstMove) {
            this.firstMove = false;
            if (AbstractDungeon.ascensionLevel >= 17) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
                return;
            }
            if (AbstractDungeon.aiRng.randomBoolean()) {
                this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
            } else {
                this.setMove((byte)3, AbstractMonster.Intent.ATTACK_BUFF, ((DamageInfo)this.damage.get((int)2)).base);
            }
            return;
        }
        if (num < 20) {
            if (!this.lastMove((byte)1)) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
            } else {
                this.getMove(AbstractDungeon.aiRng.random(20, 99));
            }
        } else if (num < 60) {
            if (!this.lastTwoMoves((byte)2)) {
                this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
            } else {
                this.setMove((byte)3, AbstractMonster.Intent.ATTACK_BUFF, ((DamageInfo)this.damage.get((int)2)).base);
            }
        } else if (!this.lastTwoMoves((byte)3)) {
            this.setMove((byte)3, AbstractMonster.Intent.ATTACK_BUFF, ((DamageInfo)this.damage.get((int)2)).base);
        } else {
            this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
        }
    }
}

