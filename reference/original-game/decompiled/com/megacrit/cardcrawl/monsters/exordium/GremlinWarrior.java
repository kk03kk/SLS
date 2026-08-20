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
import com.megacrit.cardcrawl.actions.common.EscapeAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AngryPower;
import com.megacrit.cardcrawl.vfx.SpeechBubble;

public class GremlinWarrior
extends AbstractMonster {
    public static final String ID = "GremlinWarrior";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("GremlinWarrior");
    public static final String NAME = GremlinWarrior.monsterStrings.NAME;
    public static final String[] MOVES = GremlinWarrior.monsterStrings.MOVES;
    public static final String[] DIALOG = GremlinWarrior.monsterStrings.DIALOG;
    private static final int SCRATCH_DAMAGE = 4;
    private static final int A_2_SCRATCH_DAMAGE = 5;
    private static final byte SCRATCH = 1;
    private static final int HP_MIN = 20;
    private static final int HP_MAX = 24;
    private static final int A_2_HP_MIN = 21;
    private static final int A_2_HP_MAX = 25;

    public GremlinWarrior(float x, float y) {
        super(NAME, ID, 24, -4.0f, 12.0f, 130.0f, 194.0f, null, x, y);
        this.dialogY = 30.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(21, 25);
        } else {
            this.setHp(20, 24);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.damage.add(new DamageInfo(this, 5));
        } else {
            this.damage.add(new DamageInfo(this, 4));
        }
        this.loadAnimation("images/monsters/theBottom/angryGremlin/skeleton.atlas", "images/monsters/theBottom/angryGremlin/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void usePreBattleAction() {
        if (AbstractDungeon.ascensionLevel >= 17) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new AngryPower(this, 2)));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new AngryPower(this, 1)));
        }
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                if (this.escapeNext) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                break;
            }
            case 99: {
                this.playSfx();
                AbstractDungeon.effectList.add(new SpeechBubble(this.hb.cX + this.dialogX, this.hb.cY + this.dialogY, 2.5f, DIALOG[1], false));
                AbstractDungeon.actionManager.addToBottom(new EscapeAction(this));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                break;
            }
        }
    }

    private void playSfx() {
        int roll = MathUtils.random(2);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINANGRY_1A"));
        } else if (roll == 1) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINANGRY_1B"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINANGRY_1C"));
        }
    }

    private void playDeathSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            CardCrawlGame.sound.play("VO_GREMLINANGRY_2A");
        } else {
            CardCrawlGame.sound.play("VO_GREMLINANGRY_2B");
        }
    }

    @Override
    public void die() {
        super.die();
        this.playDeathSfx();
    }

    @Override
    public void escapeNext() {
        if (!this.cannotEscape && !this.escapeNext) {
            this.escapeNext = true;
            AbstractDungeon.effectList.add(new SpeechBubble(this.dialogX, this.dialogY, 3.0f, DIALOG[2], false));
        }
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
    }

    @Override
    public void deathReact() {
        if (this.intent != AbstractMonster.Intent.ESCAPE && !this.isDying) {
            AbstractDungeon.effectList.add(new SpeechBubble(this.dialogX, this.dialogY, 3.0f, DIALOG[2], false));
            this.setMove((byte)99, AbstractMonster.Intent.ESCAPE);
            this.createIntent();
        }
    }
}

