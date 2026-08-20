/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class SpikeSlime_M
extends AbstractMonster {
    public static final String ID = "SpikeSlime_M";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SpikeSlime_M");
    public static final String NAME = SpikeSlime_M.monsterStrings.NAME;
    public static final String[] MOVES = SpikeSlime_M.monsterStrings.MOVES;
    public static final String[] DIALOG = SpikeSlime_M.monsterStrings.DIALOG;
    public static final int HP_MIN = 28;
    public static final int HP_MAX = 32;
    public static final int A_2_HP_MIN = 29;
    public static final int A_2_HP_MAX = 34;
    public static final int TACKLE_DAMAGE = 8;
    public static final int WOUND_COUNT = 1;
    public static final int A_2_TACKLE_DAMAGE = 10;
    public static final int FRAIL_TURNS = 1;
    private static final byte FLAME_TACKLE = 1;
    private static final byte FRAIL_LICK = 4;
    private static final String FRAIL_NAME = MOVES[0];

    public SpikeSlime_M(float x, float y) {
        this(x, y, 0, 32);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(29, 34);
        } else {
            this.setHp(28, 32);
        }
    }

    public SpikeSlime_M(float x, float y, int poisonAmount, int newHealth) {
        super(NAME, ID, newHealth, 0.0f, -25.0f, 170.0f, 130.0f, null, x, y, true);
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.damage.add(new DamageInfo(this, 10));
        } else {
            this.damage.add(new DamageInfo(this, 8));
        }
        if (poisonAmount >= 1) {
            this.powers.add(new PoisonPower(this, this, poisonAmount));
        }
        this.loadAnimation("images/monsters/theBottom/slimeAltM/skeleton.atlas", "images/monsters/theBottom/slimeAltM/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, 1, true), 1));
                break;
            }
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Slimed(), 1));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (AbstractDungeon.ascensionLevel >= 17) {
            if (num < 30) {
                if (this.lastTwoMoves((byte)1)) {
                    this.setMove(FRAIL_NAME, (byte)4, AbstractMonster.Intent.DEBUFF);
                } else {
                    this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base);
                }
            } else if (this.lastMove((byte)4)) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base);
            } else {
                this.setMove(FRAIL_NAME, (byte)4, AbstractMonster.Intent.DEBUFF);
            }
        } else if (num < 30) {
            if (this.lastTwoMoves((byte)1)) {
                this.setMove(FRAIL_NAME, (byte)4, AbstractMonster.Intent.DEBUFF);
            } else {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base);
            }
        } else if (this.lastTwoMoves((byte)4)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base);
        } else {
            this.setMove(FRAIL_NAME, (byte)4, AbstractMonster.Intent.DEBUFF);
        }
    }

    @Override
    public void die() {
        super.die();
        if (AbstractDungeon.getMonsters().areMonstersBasicallyDead() && AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
            this.onBossVictoryLogic();
            UnlockTracker.hardUnlockOverride("SLIME");
            UnlockTracker.unlockAchievement("SLIME_BOSS");
        }
    }
}

