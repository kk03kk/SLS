/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.BanditBear;
import com.megacrit.cardcrawl.powers.WeakPower;

public class BanditLeader
extends AbstractMonster {
    public static final String ID = "BanditLeader";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("BanditLeader");
    public static final String NAME = BanditLeader.monsterStrings.NAME;
    public static final String[] MOVES = BanditLeader.monsterStrings.MOVES;
    public static final String[] DIALOG = BanditLeader.monsterStrings.DIALOG;
    public static final int HP_MIN = 35;
    public static final int HP_MAX = 39;
    public static final int A_2_HP_MIN = 37;
    public static final int A_2_HP_MAX = 41;
    private static final int SLASH_DAMAGE = 15;
    private static final int AGONIZE_DAMAGE = 10;
    private static final int A_2_SLASH_DAMAGE = 17;
    private static final int A_2_AGONIZE_DAMAGE = 12;
    private static final int WEAK_AMT = 2;
    private static final int A_17_WEAK = 3;
    private int slashDmg;
    private int agonizeDmg;
    private int weakAmount;
    private static final byte CROSS_SLASH = 1;
    private static final byte MOCK = 2;
    private static final byte AGONIZING_SLASH = 3;

    public BanditLeader(float x, float y) {
        super(NAME, ID, 39, -10.0f, -7.0f, 180.0f, 285.0f, null, x, y);
        this.dialogX = 0.0f * Settings.scale;
        this.dialogY = 50.0f * Settings.scale;
        this.weakAmount = AbstractDungeon.ascensionLevel >= 17 ? 3 : 2;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(37, 41);
        } else {
            this.setHp(35, 39);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.slashDmg = 17;
            this.agonizeDmg = 12;
        } else {
            this.slashDmg = 15;
            this.agonizeDmg = 10;
        }
        this.damage.add(new DamageInfo(this, this.slashDmg));
        this.damage.add(new DamageInfo(this, this.agonizeDmg));
        this.loadAnimation("images/monsters/theCity/romeo/skeleton.atlas", "images/monsters/theCity/romeo/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.2f);
        this.state.setTimeScale(0.8f);
    }

    @Override
    public void deathReact() {
        if (!this.isDeadOrEscaped()) {
            AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[2]));
        }
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 2: {
                boolean bearLives = true;
                for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    if (!(m instanceof BanditBear) || !m.isDying) continue;
                    bearLives = false;
                    break;
                }
                if (bearLives) {
                    AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[0]));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[1]));
                }
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 3, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "STAB"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, this.weakAmount, true), this.weakAmount));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                break;
            }
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "STAB"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                if (AbstractDungeon.ascensionLevel >= 17 && !this.lastTwoMoves((byte)1)) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 3, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base));
                break;
            }
        }
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "STAB": {
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
            this.state.setTimeScale(0.8f);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)2, AbstractMonster.Intent.UNKNOWN);
    }
}

