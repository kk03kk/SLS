/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.EscapeAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.SpeechBubble;

public class GremlinWizard
extends AbstractMonster {
    public static final String ID = "GremlinWizard";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("GremlinWizard");
    public static final String NAME = GremlinWizard.monsterStrings.NAME;
    public static final String[] MOVES = GremlinWizard.monsterStrings.MOVES;
    public static final String[] DIALOG = GremlinWizard.monsterStrings.DIALOG;
    private static final int HP_MIN = 21;
    private static final int HP_MAX = 25;
    private static final int A_2_HP_MIN = 22;
    private static final int A_2_HP_MAX = 26;
    private static final int MAGIC_DAMAGE = 25;
    private static final int A_2_MAGIC_DAMAGE = 30;
    private static final int CHARGE_LIMIT = 3;
    private int currentCharge = 1;
    private static final byte DOPE_MAGIC = 1;
    private static final byte CHARGE = 2;

    public GremlinWizard(float x, float y) {
        super(NAME, ID, 25, 40.0f, -5.0f, 130.0f, 180.0f, null, x - 35.0f, y);
        this.dialogX = 0.0f * Settings.scale;
        this.dialogY = 50.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(22, 26);
        } else {
            this.setHp(21, 25);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.damage.add(new DamageInfo(this, 30));
        } else {
            this.damage.add(new DamageInfo(this, 25));
        }
        this.loadAnimation("images/monsters/theBottom/wizardGremlin/skeleton.atlas", "images/monsters/theBottom/wizardGremlin/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "animation", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 2: {
                ++this.currentCharge;
                AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction((AbstractCreature)this, DIALOG[1]));
                if (this.escapeNext) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                    break;
                }
                if (this.currentCharge == 3) {
                    this.playSfx();
                    AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[2], 1.5f, 3.0f));
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[1], 1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                    break;
                }
                this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.UNKNOWN);
                break;
            }
            case 1: {
                this.currentCharge = 0;
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
                if (this.escapeNext) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                    break;
                }
                if (AbstractDungeon.ascensionLevel >= 17) {
                    this.setMove(MOVES[1], (byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
                    break;
                }
                this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.UNKNOWN);
                break;
            }
            case 99: {
                AbstractDungeon.effectList.add(new SpeechBubble(this.hb.cX + this.dialogX, this.hb.cY + this.dialogY, 2.5f, DIALOG[3], false));
                AbstractDungeon.actionManager.addToBottom(new EscapeAction(this));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                break;
            }
        }
    }

    private void playSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINDOPEY_1A"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_GREMLINDOPEY_1B"));
        }
    }

    private void playDeathSfx() {
        int roll = MathUtils.random(2);
        if (roll == 0) {
            CardCrawlGame.sound.play("VO_GREMLINDOPEY_2A");
        } else if (roll == 1) {
            CardCrawlGame.sound.play("VO_GREMLINDOPEY_2B");
        } else {
            CardCrawlGame.sound.play("VO_GREMLINDOPEY_2C");
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
            AbstractDungeon.effectList.add(new SpeechBubble(this.hb.cX + this.dialogX, this.hb.cY + this.dialogY, 3.0f, DIALOG[4], false));
        }
    }

    @Override
    protected void getMove(int num) {
        this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.UNKNOWN);
    }

    @Override
    public void deathReact() {
        if (this.intent != AbstractMonster.Intent.ESCAPE && !this.isDying) {
            AbstractDungeon.effectList.add(new SpeechBubble(this.hb.cX + this.dialogX, this.hb.cY + this.dialogY, 3.0f, DIALOG[4], false));
            this.setMove((byte)99, AbstractMonster.Intent.ESCAPE);
            this.createIntent();
        }
    }
}

