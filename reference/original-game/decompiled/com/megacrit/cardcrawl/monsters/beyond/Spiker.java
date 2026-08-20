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
import com.megacrit.cardcrawl.powers.ThornsPower;

public class Spiker
extends AbstractMonster {
    public static final String ID = "Spiker";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Spiker");
    public static final String NAME = Spiker.monsterStrings.NAME;
    public static final String[] MOVES = Spiker.monsterStrings.MOVES;
    public static final String[] DIALOG = Spiker.monsterStrings.DIALOG;
    public static final String ENCOUNTER_NAME = "Ancient Shapes";
    private static final int HP_MIN = 42;
    private static final int HP_MAX = 56;
    private static final int A_2_HP_MIN = 44;
    private static final int A_2_HP_MAX = 60;
    private static final float HB_X = -8.0f;
    private static final float HB_Y = -10.0f;
    private static final float HB_W = 150.0f;
    private static final float HB_H = 150.0f;
    private static final int STARTING_THORNS = 3;
    private static final int A_2_STARTING_THORNS = 4;
    private int startingThorns;
    private static final byte ATTACK = 1;
    private static final int ATTACK_DMG = 7;
    private static final int A_2_ATTACK_DMG = 9;
    private int attackDmg;
    private static final byte BUFF_THORNS = 2;
    private static final int BUFF_AMT = 2;
    private int thornsCount = 0;

    public Spiker(float x, float y) {
        super(NAME, ID, 56, -8.0f, -10.0f, 150.0f, 150.0f, null, x, y + 10.0f);
        this.loadAnimation("images/monsters/theForest/spiker/skeleton.atlas", "images/monsters/theForest/spiker/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(44, 60);
        } else {
            this.setHp(42, 56);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.startingThorns = 4;
            this.attackDmg = 9;
        } else {
            this.startingThorns = 3;
            this.attackDmg = 7;
        }
        this.damage.add(new DamageInfo(this, this.attackDmg));
    }

    @Override
    public void usePreBattleAction() {
        if (AbstractDungeon.ascensionLevel >= 17) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ThornsPower(this, this.startingThorns + 3)));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ThornsPower(this, this.startingThorns)));
        }
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
                break;
            }
            case 2: {
                ++this.thornsCount;
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ThornsPower(this, 2), 2));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (this.thornsCount > 5) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            return;
        }
        if (num < 50 && !this.lastMove((byte)1)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            return;
        }
        this.setMove((byte)2, AbstractMonster.Intent.BUFF);
    }
}

