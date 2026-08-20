/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction;
import com.megacrit.cardcrawl.actions.animations.FastShakeAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
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
import com.megacrit.cardcrawl.powers.ConfusionPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.vfx.combat.BiteEffect;
import com.megacrit.cardcrawl.vfx.combat.IntimidateEffect;

public class Snecko
extends AbstractMonster {
    public static final String ID = "Snecko";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Snecko");
    public static final String NAME = Snecko.monsterStrings.NAME;
    public static final String[] MOVES = Snecko.monsterStrings.MOVES;
    public static final String[] DIALOG = Snecko.monsterStrings.DIALOG;
    private static final byte GLARE = 1;
    private static final byte BITE = 2;
    private static final byte TAIL = 3;
    private static final int BITE_DAMAGE = 15;
    private static final int TAIL_DAMAGE = 8;
    private static final int A_2_BITE_DAMAGE = 18;
    private static final int A_2_TAIL_DAMAGE = 10;
    private int biteDmg;
    private int tailDmg;
    private static final int VULNERABLE_AMT = 2;
    private static final int HP_MIN = 114;
    private static final int HP_MAX = 120;
    private static final int A_2_HP_MIN = 120;
    private static final int A_2_HP_MAX = 125;
    private boolean firstTurn = true;

    public Snecko() {
        this(0.0f, 0.0f);
    }

    public Snecko(float x, float y) {
        super(NAME, ID, 120, -30.0f, -20.0f, 310.0f, 305.0f, null, x, y);
        this.loadAnimation("images/monsters/theCity/reptile/skeleton.atlas", "images/monsters/theCity/reptile/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.8f);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(120, 125);
        } else {
            this.setHp(114, 120);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.biteDmg = 18;
            this.tailDmg = 10;
        } else {
            this.biteDmg = 15;
            this.tailDmg = 8;
        }
        this.damage.add(new DamageInfo(this, this.biteDmg));
        this.damage.add(new DamageInfo(this, this.tailDmg));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_SNECKO_GLARE"));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new IntimidateEffect(this.hb.cX, this.hb.cY), 0.5f));
                AbstractDungeon.actionManager.addToBottom(new FastShakeAction(AbstractDungeon.player, 1.0f, 1.0f));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new ConfusionPower(AbstractDungeon.player)));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK_2"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BiteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-50.0f, 50.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-50.0f, 50.0f) * Settings.scale, Color.CHARTREUSE.cpy()), 0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.NONE));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                if (AbstractDungeon.ascensionLevel >= 17) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, 2, true), 2));
                }
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new VulnerablePower(AbstractDungeon.player, 2, true), 2));
                break;
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
            case "ATTACK_2": {
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
    protected void getMove(int num) {
        if (this.firstTurn) {
            this.firstTurn = false;
            this.setMove(MOVES[0], (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
            return;
        }
        if (num < 40) {
            this.setMove(MOVES[1], (byte)3, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
            return;
        }
        if (this.lastTwoMoves((byte)2)) {
            this.setMove(MOVES[1], (byte)3, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
        } else {
            this.setMove(MOVES[2], (byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
        }
    }

    @Override
    public void die() {
        super.die();
        CardCrawlGame.sound.play("SNECKO_DEATH");
    }
}

