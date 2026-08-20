/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.unique.GainBlockRandomMonsterAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Centurion
extends AbstractMonster {
    public static final String ID = "Centurion";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Centurion");
    public static final String NAME = Centurion.monsterStrings.NAME;
    public static final String[] MOVES = Centurion.monsterStrings.MOVES;
    public static final String[] DIALOG = Centurion.monsterStrings.DIALOG;
    private static final float IDLE_TIMESCALE = 0.8f;
    private static final int HP_MIN = 76;
    private static final int HP_MAX = 80;
    private static final int A_2_HP_MIN = 78;
    private static final int A_2_HP_MAX = 83;
    private static final int SLASH_DMG = 12;
    private static final int FURY_DMG = 6;
    private static final int FURY_HITS = 3;
    private static final int A_2_SLASH_DMG = 14;
    private static final int A_2_FURY_DMG = 7;
    private int slashDmg;
    private int furyDmg;
    private int furyHits;
    private int blockAmount;
    private int BLOCK_AMOUNT = 15;
    private int A_17_BLOCK_AMOUNT = 20;
    private static final byte SLASH = 1;
    private static final byte PROTECT = 2;
    private static final byte FURY = 3;

    public Centurion(float x, float y) {
        super(NAME, ID, 80, -14.0f, -20.0f, 250.0f, 330.0f, null, x, y);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(78, 83);
        } else {
            this.setHp(76, 80);
        }
        this.blockAmount = AbstractDungeon.ascensionLevel >= 17 ? this.A_17_BLOCK_AMOUNT : this.BLOCK_AMOUNT;
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.slashDmg = 14;
            this.furyDmg = 7;
            this.furyHits = 3;
        } else {
            this.slashDmg = 12;
            this.furyDmg = 6;
            this.furyHits = 3;
        }
        this.damage.add(new DamageInfo(this, this.slashDmg));
        this.damage.add(new DamageInfo(this, this.furyDmg));
        this.loadAnimation("images/monsters/theCity/tank/skeleton.atlas", "images/monsters/theCity/tank/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.2f);
        this.state.setTimeScale(0.8f);
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                this.playSfx();
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "MACE_HIT"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.25f));
                AbstractDungeon.actionManager.addToBottom(new GainBlockRandomMonsterAction(this, this.blockAmount));
                break;
            }
            case 3: {
                for (int i = 0; i < this.furyHits; ++i) {
                    this.playSfx();
                    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "MACE_HIT"));
                    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                }
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    private void playSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_TANK_1A"));
        } else if (roll == 1) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_TANK_1B"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_TANK_1C"));
        }
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "MACE_HIT": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
        }
    }

    @Override
    protected void getMove(int num) {
        if (num >= 65 && !this.lastTwoMoves((byte)2) && !this.lastTwoMoves((byte)3)) {
            int aliveCount = 0;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m.isDying || m.isEscaping) continue;
                ++aliveCount;
            }
            if (aliveCount > 1) {
                this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
                return;
            }
            this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base, this.furyHits, true);
            return;
        }
        if (!this.lastTwoMoves((byte)1)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            return;
        }
        int aliveCount = 0;
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m.isDying || m.isEscaping) continue;
            ++aliveCount;
        }
        if (aliveCount > 1) {
            this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
            return;
        }
        this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base, this.furyHits, true);
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.setTimeScale(0.8f);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    public void die() {
        this.state.setTimeScale(0.1f);
        this.useShakeAnimation(5.0f);
        super.die();
    }
}

