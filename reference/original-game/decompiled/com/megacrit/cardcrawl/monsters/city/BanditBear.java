/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;

public class BanditBear
extends AbstractMonster {
    public static final String ID = "BanditBear";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("BanditBear");
    public static final String NAME = BanditBear.monsterStrings.NAME;
    public static final String[] MOVES = BanditBear.monsterStrings.MOVES;
    public static final String[] DIALOG = BanditBear.monsterStrings.DIALOG;
    public static final int HP_MIN = 38;
    public static final int HP_MAX = 42;
    public static final int A_2_HP_MIN = 40;
    public static final int A_2_HP_MAX = 44;
    private static final int MAUL_DMG = 18;
    private static final int A_2_MAUL_DMG = 20;
    private static final int LUNGE_DMG = 9;
    private static final int A_2_LUNGE_DMG = 10;
    private static final int LUNGE_DEFENSE = 9;
    private static final int CON_AMT = -2;
    private static final int A_17_CON_AMT = -4;
    private int maulDmg;
    private int lungeDmg;
    private int con_reduction;
    private static final byte MAUL = 1;
    private static final byte BEAR_HUG = 2;
    private static final byte LUNGE = 3;

    public BanditBear(float x, float y) {
        super(NAME, ID, 42, -5.0f, -4.0f, 180.0f, 280.0f, null, x, y);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(40, 44);
        } else {
            this.setHp(38, 42);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.maulDmg = 20;
            this.lungeDmg = 10;
        } else {
            this.maulDmg = 18;
            this.lungeDmg = 9;
        }
        this.con_reduction = AbstractDungeon.ascensionLevel >= 17 ? -4 : -2;
        this.damage.add(new DamageInfo(this, this.maulDmg));
        this.damage.add(new DamageInfo(this, this.lungeDmg));
        this.loadAnimation("images/monsters/theCity/bear/skeleton.atlas", "images/monsters/theCity/bear/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.2f);
        this.state.setTimeScale(1.0f);
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new DexterityPower(AbstractDungeon.player, this.con_reduction), this.con_reduction));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 3, AbstractMonster.Intent.ATTACK_DEFEND, ((DamageInfo)this.damage.get((int)1)).base));
                break;
            }
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "MAUL"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 3, AbstractMonster.Intent.ATTACK_DEFEND, ((DamageInfo)this.damage.get((int)1)).base));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, 9));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                break;
            }
        }
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "MAUL": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.setTimeScale(1.0f);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    public void die() {
        super.die();
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (m.isDead || m.isDying) continue;
            m.deathReact();
        }
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
    }
}

