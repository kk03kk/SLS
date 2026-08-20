/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
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
import com.megacrit.cardcrawl.vfx.SpeechBubble;

public class GremlinThief
extends AbstractMonster {
    public static final String ID = "GremlinThief";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("GremlinThief");
    public static final String NAME = GremlinThief.monsterStrings.NAME;
    public static final String[] MOVES = GremlinThief.monsterStrings.MOVES;
    public static final String[] DIALOG = GremlinThief.monsterStrings.DIALOG;
    private static final int THIEF_DAMAGE = 9;
    private static final int A_2_THIEF_DAMAGE = 10;
    private static final byte PUNCTURE = 1;
    private static final int HP_MIN = 10;
    private static final int HP_MAX = 14;
    private static final int A_2_HP_MIN = 11;
    private static final int A_2_HP_MAX = 15;
    private int thiefDamage;

    public GremlinThief(float x, float y) {
        super(NAME, ID, 14, 0.0f, 0.0f, 120.0f, 160.0f, null, x, y);
        this.dialogY = 50.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(11, 15);
        } else {
            this.setHp(10, 14);
        }
        this.thiefDamage = AbstractDungeon.ascensionLevel >= 2 ? 10 : 9;
        this.damage.add(new DamageInfo(this, this.thiefDamage));
        this.loadAnimation("images/monsters/theBottom/thiefGremlin/skeleton.atlas", "images/monsters/theBottom/thiefGremlin/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "animation", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
                if (!this.escapeNext) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 1, AbstractMonster.Intent.ATTACK, this.thiefDamage));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
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
        int roll = MathUtils.random(1);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINSPAZZY_1A"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINSPAZZY_1B"));
        }
    }

    private void playDeathSfx() {
        int roll = MathUtils.random(2);
        if (roll == 0) {
            CardCrawlGame.sound.play("VO_GREMLINSPAZZY_2A");
        } else if (roll == 1) {
            CardCrawlGame.sound.play("VO_GREMLINSPAZZY_2B");
        } else {
            CardCrawlGame.sound.play("VO_GREMLINSPAZZY_2C");
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

