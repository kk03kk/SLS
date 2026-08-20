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
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.vfx.SpeechBubble;

public class GremlinFat
extends AbstractMonster {
    public static final String ID = "GremlinFat";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("GremlinFat");
    public static final String NAME = GremlinFat.monsterStrings.NAME;
    public static final String[] MOVES = GremlinFat.monsterStrings.MOVES;
    public static final String[] DIALOG = GremlinFat.monsterStrings.DIALOG;
    private static final int HP_MIN = 13;
    private static final int HP_MAX = 17;
    private static final int A_2_HP_MIN = 14;
    private static final int A_2_HP_MAX = 18;
    private static final int BLUNT_DAMAGE = 4;
    private static final int A_2_BLUNT_DAMAGE = 5;
    private static final int WEAK_AMT = 1;
    private static final byte BLUNT = 2;

    public GremlinFat(float x, float y) {
        super(NAME, ID, 17, 0.0f, 0.0f, 110.0f, 220.0f, null, x, y);
        this.dialogY = 30.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(14, 18);
        } else {
            this.setHp(13, 17);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.damage.add(new DamageInfo(this, 5));
        } else {
            this.damage.add(new DamageInfo(this, 4));
        }
        this.loadAnimation("images/monsters/theBottom/fatGremlin/skeleton.atlas", "images/monsters/theBottom/fatGremlin/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "animation", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, 1, true), 1));
                if (AbstractDungeon.ascensionLevel >= 17) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, 1, true), 1));
                }
                if (this.escapeNext) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
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
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINFAT_1A"));
        } else if (roll == 1) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINFAT_1B"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINFAT_1C"));
        }
    }

    private void playDeathSfx() {
        int roll = MathUtils.random(2);
        if (roll == 0) {
            CardCrawlGame.sound.play("VO_GREMLINFAT_2A");
        } else if (roll == 1) {
            CardCrawlGame.sound.play("VO_GREMLINFAT_2B");
        } else {
            CardCrawlGame.sound.play("VO_GREMLINFAT_2C");
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
        this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base);
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

