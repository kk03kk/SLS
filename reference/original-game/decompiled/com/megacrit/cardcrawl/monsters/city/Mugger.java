/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.EscapeAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ThieveryPower;
import com.megacrit.cardcrawl.vfx.combat.SmokeBombEffect;

public class Mugger
extends AbstractMonster {
    public static final String ID = "Mugger";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Mugger");
    public static final String NAME = Mugger.monsterStrings.NAME;
    public static final String[] MOVES = Mugger.monsterStrings.MOVES;
    public static final String[] DIALOG = Mugger.monsterStrings.DIALOG;
    private static final int HP_MIN = 48;
    private static final int HP_MAX = 52;
    private static final int A_2_HP_MIN = 50;
    private static final int A_2_HP_MAX = 54;
    public static final String ENCOUNTER_NAME = "City Looters";
    private int swipeDmg;
    private int bigSwipeDmg;
    private int goldAmt;
    private int escapeDef = 11;
    private static final byte MUG = 1;
    private static final byte SMOKE_BOMB = 2;
    private static final byte ESCAPE = 3;
    private static final byte BIGSWIPE = 4;
    private static final String SLASH_MSG1 = DIALOG[0];
    private static final String RUN_MSG = DIALOG[1];
    private int slashCount = 0;
    private int stolenGold = 0;

    public Mugger(float x, float y) {
        super(NAME, ID, 52, 0.0f, 0.0f, 200.0f, 220.0f, null, x, y);
        this.dialogX = -30.0f * Settings.scale;
        this.dialogY = 50.0f * Settings.scale;
        this.goldAmt = AbstractDungeon.ascensionLevel >= 17 ? 20 : 15;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(50, 54);
        } else {
            this.setHp(48, 52);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.swipeDmg = 11;
            this.bigSwipeDmg = 18;
        } else {
            this.swipeDmg = 10;
            this.bigSwipeDmg = 16;
        }
        this.damage.add(new DamageInfo(this, this.swipeDmg));
        this.damage.add(new DamageInfo(this, this.bigSwipeDmg));
        this.loadAnimation("images/monsters/theCity/looterAlt/skeleton.atlas", "images/monsters/theCity/looterAlt/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ThieveryPower(this, this.goldAmt)));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                this.playSfx();
                if (this.slashCount == 1 && AbstractDungeon.aiRng.randomBoolean(0.6f)) {
                    AbstractDungeon.actionManager.addToBottom(new TalkAction(this, SLASH_MSG1));
                }
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new AbstractGameAction(){

                    @Override
                    public void update() {
                        Mugger.this.stolenGold = Mugger.this.stolenGold + Math.min(Mugger.this.goldAmt, AbstractDungeon.player.gold);
                        this.isDone = true;
                    }
                });
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), this.goldAmt));
                ++this.slashCount;
                if (this.slashCount == 2) {
                    if (AbstractDungeon.aiRng.randomBoolean(0.5f)) {
                        this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
                        break;
                    }
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[0], 4, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[1], 1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                break;
            }
            case 4: {
                this.playSfx();
                ++this.slashCount;
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new AbstractGameAction(){

                    @Override
                    public void update() {
                        Mugger.this.stolenGold = Mugger.this.stolenGold + Math.min(Mugger.this.goldAmt, AbstractDungeon.player.gold);
                        this.isDone = true;
                    }
                });
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), this.goldAmt));
                this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
                break;
            }
            case 2: {
                if (AbstractDungeon.ascensionLevel >= 17) {
                    AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, this.escapeDef + 6));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, this.escapeDef));
                }
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 3, AbstractMonster.Intent.ESCAPE));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new TalkAction(this, RUN_MSG));
                AbstractDungeon.getCurrRoom().mugged = true;
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new SmokeBombEffect(this.hb.cX, this.hb.cY)));
                AbstractDungeon.actionManager.addToBottom(new EscapeAction(this));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 3, AbstractMonster.Intent.ESCAPE));
                break;
            }
        }
    }

    private void playSfx() {
        int roll = AbstractDungeon.aiRng.random(2);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_MUGGER_1A"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_MUGGER_1B"));
        }
    }

    private void playDeathSfx() {
        int roll = AbstractDungeon.aiRng.random(2);
        if (roll == 0) {
            CardCrawlGame.sound.play("VO_MUGGER_2A");
        } else {
            CardCrawlGame.sound.play("VO_MUGGER_2B");
        }
    }

    @Override
    public void die() {
        this.playDeathSfx();
        this.state.setTimeScale(0.1f);
        this.useShakeAnimation(5.0f);
        if (this.stolenGold > 0) {
            AbstractDungeon.getCurrRoom().addStolenGoldToRewards(this.stolenGold);
        }
        super.die();
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
    }
}

