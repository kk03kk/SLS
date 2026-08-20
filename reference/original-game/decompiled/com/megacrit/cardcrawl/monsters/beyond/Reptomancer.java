/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import com.megacrit.cardcrawl.actions.common.SuicideAction;
import com.megacrit.cardcrawl.actions.utility.HideHealthBarAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.SnakeDagger;
import com.megacrit.cardcrawl.powers.MinionPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.vfx.combat.BiteEffect;

public class Reptomancer
extends AbstractMonster {
    public static final String ID = "Reptomancer";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Reptomancer");
    public static final String NAME = Reptomancer.monsterStrings.NAME;
    private static final int HP_MIN = 180;
    private static final int HP_MAX = 190;
    private static final int A_2_HP_MIN = 190;
    private static final int A_2_HP_MAX = 200;
    private static final int BITE_DMG = 30;
    private static final int A_2_BITE_DMG = 34;
    private static final int SNAKE_STRIKE_DMG = 13;
    private static final int A_2_SNAKE_STRIKE_DMG = 16;
    private static final int DAGGERS_PER_SPAWN = 1;
    private static final int ASC_2_DAGGERS_PER_SPAWN = 2;
    private static final byte SNAKE_STRIKE = 1;
    private static final byte SPAWN_DAGGER = 2;
    private static final byte BIG_BITE = 3;
    public static final float[] POSX = new float[]{210.0f, -220.0f, 180.0f, -250.0f};
    public static final float[] POSY = new float[]{75.0f, 115.0f, 345.0f, 335.0f};
    private int daggersPerSpawn;
    private AbstractMonster[] daggers = new AbstractMonster[4];
    private boolean firstMove = true;

    public Reptomancer() {
        super(NAME, ID, AbstractDungeon.monsterHpRng.random(180, 190), 0.0f, -30.0f, 220.0f, 320.0f, null, -20.0f, 10.0f);
        this.type = AbstractMonster.EnemyType.ELITE;
        this.loadAnimation("images/monsters/theForest/mage/skeleton.atlas", "images/monsters/theForest/mage/skeleton.json", 1.0f);
        this.daggersPerSpawn = AbstractDungeon.ascensionLevel >= 18 ? 2 : 1;
        if (AbstractDungeon.ascensionLevel >= 8) {
            this.setHp(190, 200);
        } else {
            this.setHp(180, 190);
        }
        if (AbstractDungeon.ascensionLevel >= 3) {
            this.damage.add(new DamageInfo(this, 16));
            this.damage.add(new DamageInfo(this, 34));
        } else {
            this.damage.add(new DamageInfo(this, 13));
            this.damage.add(new DamageInfo(this, 30));
        }
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        this.stateData.setMix("Idle", "Sumon", 0.1f);
        this.stateData.setMix("Sumon", "Idle", 0.1f);
        this.stateData.setMix("Hurt", "Idle", 0.1f);
        this.stateData.setMix("Idle", "Hurt", 0.1f);
        this.stateData.setMix("Attack", "Idle", 0.1f);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void usePreBattleAction() {
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (!m.id.equals(this.id)) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new MinionPower(this)));
            }
            if (!(m instanceof SnakeDagger)) continue;
            if (AbstractDungeon.getMonsters().monsters.indexOf(m) > AbstractDungeon.getMonsters().monsters.indexOf(this)) {
                this.daggers[0] = m;
                continue;
            }
            this.daggers[1] = m;
        }
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BiteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-50.0f, 50.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-50.0f, 50.0f) * Settings.scale, Color.ORANGE.cpy()), 0.1f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.NONE));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BiteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-50.0f, 50.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-50.0f, 50.0f) * Settings.scale, Color.ORANGE.cpy()), 0.1f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.NONE));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, 1, true), 1));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "SUMMON"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                int daggersSpawned = 0;
                for (int i = 0; daggersSpawned < this.daggersPerSpawn && i < this.daggers.length; ++i) {
                    if (this.daggers[i] != null && !this.daggers[i].isDeadOrEscaped()) continue;
                    SnakeDagger daggerToSpawn = new SnakeDagger(POSX[i], POSY[i]);
                    this.daggers[i] = daggerToSpawn;
                    AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(daggerToSpawn, true));
                    ++daggersSpawned;
                }
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BiteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-50.0f, 50.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-50.0f, 50.0f) * Settings.scale, Color.CHARTREUSE.cpy()), 0.1f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.NONE));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    private boolean canSpawn() {
        int aliveCount = 0;
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m == this || m.isDying) continue;
            ++aliveCount;
        }
        return aliveCount <= 3;
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hurt", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    public void die() {
        super.die();
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (m.isDead || m.isDying) continue;
            AbstractDungeon.actionManager.addToTop(new HideHealthBarAction(m));
            AbstractDungeon.actionManager.addToTop(new SuicideAction(m));
        }
    }

    @Override
    protected void getMove(int num) {
        if (this.firstMove) {
            this.firstMove = false;
            this.setMove((byte)2, AbstractMonster.Intent.UNKNOWN);
            return;
        }
        if (num < 33) {
            if (!this.lastMove((byte)1)) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
            } else {
                this.getMove(AbstractDungeon.aiRng.random(33, 99));
            }
        } else if (num < 66) {
            if (!this.lastTwoMoves((byte)2)) {
                if (this.canSpawn()) {
                    this.setMove((byte)2, AbstractMonster.Intent.UNKNOWN);
                } else {
                    this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
                }
            } else {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
            }
        } else if (!this.lastMove((byte)3)) {
            this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
        } else {
            this.getMove(AbstractDungeon.aiRng.random(65));
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
            case "SUMMON": {
                this.state.setAnimation(0, "Sumon", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
        }
    }
}

