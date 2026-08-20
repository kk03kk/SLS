/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Repulsor
extends AbstractMonster {
    public static final String ID = "Repulsor";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Repulsor");
    public static final String NAME = Repulsor.monsterStrings.NAME;
    public static final String[] MOVES = Repulsor.monsterStrings.MOVES;
    public static final String[] DIALOG = Repulsor.monsterStrings.DIALOG;
    public static final String ENCOUNTER_NAME_W = "Ancient Shapes Weak";
    public static final String ENCOUNTER_NAME = "Ancient Shapes";
    private static final float HB_X = -8.0f;
    private static final float HB_Y = -10.0f;
    private static final float HB_W = 150.0f;
    private static final float HB_H = 150.0f;
    private static final byte DAZE = 1;
    private static final byte ATTACK = 2;
    private int attackDmg;
    private int dazeAmt;

    public Repulsor(float x, float y) {
        super(NAME, ID, 35, -8.0f, -10.0f, 150.0f, 150.0f, null, x, y + 10.0f);
        this.loadAnimation("images/monsters/theForest/repulser/skeleton.atlas", "images/monsters/theForest/repulser/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.dazeAmt = 2;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(31, 38);
        } else {
            this.setHp(29, 35);
        }
        this.attackDmg = AbstractDungeon.ascensionLevel >= 2 ? 13 : 11;
        this.damage.add(new DamageInfo(this, this.attackDmg));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
                break;
            }
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new Dazed(), this.dazeAmt, true, true));
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (num < 20 && !this.lastMove((byte)2)) {
            this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            return;
        }
        this.setMove((byte)1, AbstractMonster.Intent.DEBUFF);
    }
}

