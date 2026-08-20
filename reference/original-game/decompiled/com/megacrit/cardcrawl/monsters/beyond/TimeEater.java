/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.ShoutAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.unique.RemoveDebuffsAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DrawReductionPower;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.TimeWarpPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.combat.ShockWaveEffect;

public class TimeEater
extends AbstractMonster {
    public static final String ID = "TimeEater";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("TimeEater");
    public static final String NAME = TimeEater.monsterStrings.NAME;
    public static final String[] MOVES = TimeEater.monsterStrings.MOVES;
    public static final String[] DIALOG = TimeEater.monsterStrings.DIALOG;
    public static final int HP = 456;
    public static final int A_2_HP = 480;
    private static final byte REVERBERATE = 2;
    private static final byte RIPPLE = 3;
    private static final byte HEAD_SLAM = 4;
    private static final byte HASTE = 5;
    private static final int REVERB_DMG = 7;
    private static final int REVERB_AMT = 3;
    private static final int A_2_REVERB_DMG = 8;
    private static final int RIPPLE_BLOCK = 20;
    private static final int HEAD_SLAM_DMG = 26;
    private static final int A_2_HEAD_SLAM_DMG = 32;
    private int reverbDmg;
    private int headSlamDmg;
    private static final int HEAD_SLAM_STICKY = 1;
    private static final int RIPPLE_DEBUFF_TURNS = 1;
    private boolean usedHaste = false;
    private boolean firstTurn = true;

    public TimeEater() {
        super(NAME, ID, 456, -10.0f, -30.0f, 476.0f, 410.0f, null, -50.0f, 30.0f);
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(480);
        } else {
            this.setHp(456);
        }
        this.loadAnimation("images/monsters/theForest/timeEater/skeleton.atlas", "images/monsters/theForest/timeEater/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.8f);
        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = -200.0f * Settings.scale;
        this.dialogY = 10.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 4) {
            this.reverbDmg = 8;
            this.headSlamDmg = 32;
        } else {
            this.reverbDmg = 7;
            this.headSlamDmg = 26;
        }
        this.damage.add(new DamageInfo(this, this.reverbDmg, DamageInfo.DamageType.NORMAL));
        this.damage.add(new DamageInfo(this, this.headSlamDmg, DamageInfo.DamageType.NORMAL));
    }

    @Override
    public void usePreBattleAction() {
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BEYOND");
        UnlockTracker.markBossAsSeen("WIZARD");
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new TimeWarpPower(this)));
    }

    @Override
    public void takeTurn() {
        if (this.firstTurn) {
            if (AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.WATCHER) {
                AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[2], 0.5f, 2.0f));
            } else {
                AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[0], 0.5f, 2.0f));
            }
            this.firstTurn = false;
        }
        switch (this.nextMove) {
            case 2: {
                for (int i = 0; i < 3; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new ShockWaveEffect(this.hb.cX, this.hb.cY, Settings.BLUE_TEXT_COLOR, ShockWaveEffect.ShockWaveType.CHAOTIC), 0.75f));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
                }
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, 20));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new VulnerablePower(AbstractDungeon.player, 1, true), 1));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, 1, true), 1));
                if (AbstractDungeon.ascensionLevel < 19) break;
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, 1, true), 1));
                break;
            }
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.POISON));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new DrawReductionPower(AbstractDungeon.player, 1)));
                if (AbstractDungeon.ascensionLevel < 19) break;
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Slimed(), 2));
                break;
            }
            case 5: {
                AbstractDungeon.actionManager.addToBottom(new ShoutAction(this, DIALOG[1], 0.5f, 2.0f));
                AbstractDungeon.actionManager.addToBottom(new RemoveDebuffsAction(this));
                AbstractDungeon.actionManager.addToBottom(new RemoveSpecificPowerAction((AbstractCreature)this, (AbstractCreature)this, "Shackled"));
                AbstractDungeon.actionManager.addToBottom(new HealAction(this, this, this.maxHealth / 2 - this.currentHealth));
                if (AbstractDungeon.ascensionLevel < 19) break;
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, this.headSlamDmg));
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
        if (this.currentHealth < this.maxHealth / 2 && !this.usedHaste) {
            this.usedHaste = true;
            this.setMove((byte)5, AbstractMonster.Intent.BUFF);
            return;
        }
        if (num < 45) {
            if (!this.lastTwoMoves((byte)2)) {
                this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 3, true);
                return;
            }
            this.getMove(AbstractDungeon.aiRng.random(50, 99));
            return;
        }
        if (num < 80) {
            if (!this.lastMove((byte)4)) {
                this.setMove((byte)4, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
                return;
            }
            if (AbstractDungeon.aiRng.randomBoolean(0.66f)) {
                this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 3, true);
                return;
            }
            this.setMove((byte)3, AbstractMonster.Intent.DEFEND_DEBUFF);
            return;
        }
        if (!this.lastMove((byte)3)) {
            this.setMove((byte)3, AbstractMonster.Intent.DEFEND_DEBUFF);
            return;
        }
        this.getMove(AbstractDungeon.aiRng.random(74));
    }

    @Override
    public void die() {
        if (!AbstractDungeon.getCurrRoom().cannotLose) {
            this.useFastShakeAnimation(5.0f);
            CardCrawlGame.screenShake.rumble(4.0f);
            super.die();
            this.onBossVictoryLogic();
            UnlockTracker.hardUnlockOverride("WIZARD");
            UnlockTracker.unlockAchievement("TIME_EATER");
            this.onFinalBossVictoryLogic();
        }
    }
}

