/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class Taskmaster
extends AbstractMonster {
    public static final String ID = "SlaverBoss";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SlaverBoss");
    public static final String NAME = Taskmaster.monsterStrings.NAME;
    public static final String[] MOVES = Taskmaster.monsterStrings.MOVES;
    public static final String[] DIALOG = Taskmaster.monsterStrings.DIALOG;
    private static final int HP_MIN = 54;
    private static final int HP_MAX = 60;
    private static final int A_2_HP_MIN = 57;
    private static final int A_2_HP_MAX = 64;
    private static final int WHIP_DMG = 4;
    private static final int SCOURING_WHIP_DMG = 7;
    private static final int WOUNDS = 1;
    private static final int A_2_WOUNDS = 2;
    private int woundCount;
    private static final byte SCOURING_WHIP = 2;

    public Taskmaster(float x, float y) {
        super(NAME, ID, AbstractDungeon.monsterHpRng.random(54, 60), -10.0f, -8.0f, 200.0f, 280.0f, null, x, y);
        this.type = AbstractMonster.EnemyType.ELITE;
        if (AbstractDungeon.ascensionLevel >= 8) {
            this.setHp(57, 64);
        } else {
            this.setHp(54, 60);
        }
        this.woundCount = AbstractDungeon.ascensionLevel >= 18 ? 3 : (AbstractDungeon.ascensionLevel >= 3 ? 2 : 1);
        this.damage.add(new DamageInfo(this, 4));
        this.damage.add(new DamageInfo(this, 7));
        this.loadAnimation("images/monsters/theCity/slaverMaster/skeleton.atlas", "images/monsters/theCity/slaverMaster/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 2: {
                this.playSfx();
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Wound(), this.woundCount));
                if (AbstractDungeon.ascensionLevel < 18) break;
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, 1), 1));
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)2, AbstractMonster.Intent.ATTACK_DEBUFF, 7);
    }

    private void playSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_SLAVERLEADER_1A"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_SLAVERLEADER_1B"));
        }
    }

    private void playDeathSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            CardCrawlGame.sound.play("VO_SLAVERLEADER_2A");
        } else {
            CardCrawlGame.sound.play("VO_SLAVERLEADER_2B");
        }
    }

    @Override
    public void die() {
        super.die();
        this.playDeathSfx();
    }
}

