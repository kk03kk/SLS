/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class Donu
extends AbstractMonster {
    public static final String ID = "Donu";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Donu");
    public static final String NAME = Donu.monsterStrings.NAME;
    public static final String[] MOVES = Donu.monsterStrings.MOVES;
    public static final String[] DIALOG = Donu.monsterStrings.DIALOG;
    public static final int HP = 250;
    public static final int A_2_HP = 265;
    private static final byte BEAM = 0;
    private static final byte CIRCLE_OF_PROTECTION = 2;
    private static final int ARTIFACT_AMT = 2;
    private static final int BEAM_DMG = 10;
    private static final int BEAM_AMT = 2;
    private static final int A_2_BEAM_DMG = 12;
    private int beamDmg;
    private static final String CIRCLE_NAME = MOVES[0];
    private static final int CIRCLE_STR_AMT = 3;
    private boolean isAttacking;

    public Donu() {
        super(NAME, ID, 250, 0.0f, -20.0f, 390.0f, 390.0f, null, 100.0f, 20.0f);
        this.loadAnimation("images/monsters/theForest/donu/skeleton.atlas", "images/monsters/theForest/donu/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.1f);
        this.stateData.setMix("Attack_2", "Idle", 0.1f);
        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = -200.0f * Settings.scale;
        this.dialogY = 10.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(265);
        } else {
            this.setHp(250);
        }
        this.beamDmg = AbstractDungeon.ascensionLevel >= 4 ? 12 : 10;
        this.damage.add(new DamageInfo(this, this.beamDmg));
        this.isAttacking = false;
    }

    @Override
    public void changeState(String stateName) {
        switch (stateName) {
            case "ATTACK": {
                this.state.setAnimation(0, "Attack_2", false);
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
    public void usePreBattleAction() {
        if (AbstractDungeon.ascensionLevel >= 19) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ArtifactPower(this, 3)));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ArtifactPower(this, 2)));
        }
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 0: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                for (int i = 0; i < 2; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
                }
                this.isAttacking = false;
                break;
            }
            case 2: {
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_DONU_DEFENSE"));
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, this, new StrengthPower(m, 3), 3));
                }
                this.isAttacking = true;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (this.isAttacking) {
            this.setMove((byte)0, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
        } else {
            this.setMove(CIRCLE_NAME, (byte)2, AbstractMonster.Intent.BUFF);
        }
    }

    @Override
    public void die() {
        super.die();
        if (AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.useFastShakeAnimation(5.0f);
            CardCrawlGame.screenShake.rumble(4.0f);
            this.onBossVictoryLogic();
            UnlockTracker.hardUnlockOverride("DONUT");
            UnlockTracker.unlockAchievement("SHAPES");
            this.onFinalBossVictoryLogic();
        }
    }
}

