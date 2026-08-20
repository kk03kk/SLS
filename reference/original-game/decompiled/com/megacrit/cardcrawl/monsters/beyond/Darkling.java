/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.RegrowPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Darkling
extends AbstractMonster {
    private static final Logger logger = LogManager.getLogger(Darkling.class.getName());
    public static final String ID = "Darkling";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Darkling");
    public static final String NAME = Darkling.monsterStrings.NAME;
    public static final String[] MOVES = Darkling.monsterStrings.MOVES;
    public static final String[] DIALOG = Darkling.monsterStrings.DIALOG;
    public static final int HP_MIN = 48;
    public static final int HP_MAX = 56;
    public static final int A_2_HP_MIN = 50;
    public static final int A_2_HP_MAX = 59;
    private static final float HB_X = 0.0f;
    private static final float HB_Y = -20.0f;
    private static final float HB_W = 260.0f;
    private static final float HB_H = 200.0f;
    private static final int BITE_DMG = 8;
    private static final int A_2_BITE_DMG = 9;
    private int chompDmg;
    private int nipDmg;
    private static final int BLOCK_AMT = 12;
    private static final int CHOMP_AMT = 2;
    private static final byte CHOMP = 1;
    private static final byte HARDEN = 2;
    private static final byte NIP = 3;
    private static final byte COUNT = 4;
    private static final byte REINCARNATE = 5;
    private boolean firstMove = true;

    public Darkling(float x, float y) {
        super(NAME, ID, 56, 0.0f, -20.0f, 260.0f, 200.0f, null, x, y + 20.0f);
        this.loadAnimation("images/monsters/theForest/darkling/skeleton.atlas", "images/monsters/theForest/darkling/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        e.setTimeScale(MathUtils.random(0.75f, 1.0f));
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(50, 59);
        } else {
            this.setHp(48, 56);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.chompDmg = 9;
            this.nipDmg = AbstractDungeon.monsterHpRng.random(9, 13);
        } else {
            this.chompDmg = 8;
            this.nipDmg = AbstractDungeon.monsterHpRng.random(7, 11);
        }
        this.dialogX = -50.0f * Settings.scale;
        this.damage.add(new DamageInfo(this, this.chompDmg));
        this.damage.add(new DamageInfo(this, this.nipDmg));
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.getCurrRoom().cannotLose = true;
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new RegrowPower(this)));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, 12));
                if (AbstractDungeon.ascensionLevel < 17) break;
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, 2), 2));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
                break;
            }
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction((AbstractCreature)this, DIALOG[0]));
                break;
            }
            case 5: {
                if (MathUtils.randomBoolean()) {
                    AbstractDungeon.actionManager.addToBottom(new SFXAction("DARKLING_REGROW_2", MathUtils.random(-0.1f, 0.1f)));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new SFXAction("DARKLING_REGROW_1", MathUtils.random(-0.1f, 0.1f)));
                }
                AbstractDungeon.actionManager.addToBottom(new HealAction(this, this, this.maxHealth / 2));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "REVIVE"));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new RegrowPower(this), 1));
                for (AbstractRelic r : AbstractDungeon.player.relics) {
                    r.onSpawnMonster(this);
                }
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (this.halfDead) {
            this.setMove((byte)5, AbstractMonster.Intent.BUFF);
            return;
        }
        if (this.firstMove) {
            if (num < 50) {
                if (AbstractDungeon.ascensionLevel >= 17) {
                    this.setMove((byte)2, AbstractMonster.Intent.DEFEND_BUFF);
                } else {
                    this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
                }
            } else {
                this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
            }
            this.firstMove = false;
            return;
        }
        if (num < 40) {
            if (!this.lastMove((byte)1) && AbstractDungeon.getMonsters().monsters.lastIndexOf(this) % 2 == 0) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
            } else {
                this.getMove(AbstractDungeon.aiRng.random(40, 99));
            }
        } else if (num < 70) {
            if (!this.lastMove((byte)2)) {
                if (AbstractDungeon.ascensionLevel >= 17) {
                    this.setMove((byte)2, AbstractMonster.Intent.DEFEND_BUFF);
                } else {
                    this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
                }
            } else {
                this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
            }
        } else if (!this.lastTwoMoves((byte)3)) {
            this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
        } else {
            this.getMove(AbstractDungeon.aiRng.random(0, 99));
        }
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "ATTACK": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
            case "REVIVE": {
                this.halfDead = false;
                break;
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (this.currentHealth <= 0 && !this.halfDead) {
            this.halfDead = true;
            for (AbstractPower p : this.powers) {
                p.onDeath();
            }
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onMonsterDeath(this);
            }
            this.powers.clear();
            logger.info("This monster is now half dead.");
            boolean allDead = true;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (!m.id.equals(ID) || m.halfDead) continue;
                allDead = false;
            }
            logger.info("All dead: " + allDead);
            if (!allDead) {
                if (this.nextMove != 4) {
                    this.setMove((byte)4, AbstractMonster.Intent.UNKNOWN);
                    this.createIntent();
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 4, AbstractMonster.Intent.UNKNOWN));
                }
            } else {
                AbstractDungeon.getCurrRoom().cannotLose = false;
                this.halfDead = false;
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    m.die();
                }
            }
        } else if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    public void die() {
        if (!AbstractDungeon.getCurrRoom().cannotLose) {
            super.die();
        }
    }
}

