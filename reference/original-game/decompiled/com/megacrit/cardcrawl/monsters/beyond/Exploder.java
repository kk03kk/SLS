/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ExplosivePower;

public class Exploder
extends AbstractMonster {
    public static final String ID = "Exploder";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Exploder");
    public static final String NAME = Exploder.monsterStrings.NAME;
    public static final String[] MOVES = Exploder.monsterStrings.MOVES;
    public static final String[] DIALOG = Exploder.monsterStrings.DIALOG;
    public static final String ENCOUNTER_NAME = "Ancient Shapes";
    private static final int HP_MIN = 30;
    private static final int HP_MAX = 30;
    private static final int A_2_HP_MIN = 30;
    private static final int A_2_HP_MAX = 35;
    private int turnCount = 0;
    private static final float HB_X = -8.0f;
    private static final float HB_Y = -10.0f;
    private static final float HB_W = 150.0f;
    private static final float HB_H = 150.0f;
    private static final byte ATTACK = 1;
    private static final int ATTACK_DMG = 9;
    private static final int A_2_ATTACK_DMG = 11;
    private int attackDmg;
    private static final byte BLOCK = 2;
    private static final int EXPLODE_BASE = 3;

    public Exploder(float x, float y) {
        super(NAME, ID, 30, -8.0f, -10.0f, 150.0f, 150.0f, null, x, y + 10.0f);
        this.loadAnimation("images/monsters/theForest/exploder/skeleton.atlas", "images/monsters/theForest/exploder/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(30, 35);
        } else {
            this.setHp(30, 30);
        }
        this.attackDmg = AbstractDungeon.ascensionLevel >= 2 ? 11 : 9;
        this.damage.add(new DamageInfo(this, this.attackDmg));
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ExplosivePower(this, 3)));
    }

    @Override
    public void takeTurn() {
        ++this.turnCount;
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
                break;
            }
            case 2: {
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (this.turnCount < 2) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
        } else {
            this.setMove((byte)2, AbstractMonster.Intent.UNKNOWN);
        }
    }
}

