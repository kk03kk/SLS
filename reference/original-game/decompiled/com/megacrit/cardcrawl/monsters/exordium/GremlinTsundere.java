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
import com.megacrit.cardcrawl.actions.unique.GainBlockRandomMonsterAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.SpeechBubble;

public class GremlinTsundere
extends AbstractMonster {
    public static final String ID = "GremlinTsundere";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("GremlinTsundere");
    public static final String NAME = GremlinTsundere.monsterStrings.NAME;
    public static final String[] MOVES = GremlinTsundere.monsterStrings.MOVES;
    public static final String[] DIALOG = GremlinTsundere.monsterStrings.DIALOG;
    private static final int HP_MIN = 12;
    private static final int HP_MAX = 15;
    private static final int A_2_HP_MIN = 13;
    private static final int A_2_HP_MAX = 17;
    private static final int BLOCK_AMOUNT = 7;
    private static final int A_2_BLOCK_AMOUNT = 8;
    private static final int A_17_BLOCK_AMOUNT = 11;
    private static final int BASH_DAMAGE = 6;
    private static final int A_2_BASH_DAMAGE = 8;
    private int blockAmt;
    private int bashDmg;
    private static final byte PROTECT = 1;
    private static final byte BASH = 2;

    public GremlinTsundere(float x, float y) {
        super(NAME, ID, 15, 0.0f, 0.0f, 120.0f, 200.0f, null, x, y);
        this.dialogY = 60.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 17) {
            this.setHp(13, 17);
            this.blockAmt = 11;
        } else if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(13, 17);
            this.blockAmt = 8;
        } else {
            this.setHp(12, 15);
            this.blockAmt = 7;
        }
        this.bashDmg = AbstractDungeon.ascensionLevel >= 2 ? 8 : 6;
        this.damage.add(new DamageInfo(this, this.bashDmg));
        this.loadAnimation("images/monsters/theBottom/femaleGremlin/skeleton.atlas", "images/monsters/theBottom/femaleGremlin/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new GainBlockRandomMonsterAction(this, this.blockAmt));
                int aliveCount = 0;
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (m.isDying || m.isEscaping) continue;
                    ++aliveCount;
                }
                if (this.escapeNext) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                    break;
                }
                if (aliveCount > 1) {
                    this.setMove(MOVES[0], (byte)1, AbstractMonster.Intent.DEFEND);
                    break;
                }
                this.setMove(MOVES[1], (byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
                if (this.escapeNext) {
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[1], 2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                break;
            }
            case 99: {
                AbstractDungeon.effectList.add(new SpeechBubble(this.hb.cX + this.dialogX, this.hb.cY + this.dialogY, 2.5f, DIALOG[1], false));
                AbstractDungeon.actionManager.addToBottom(new EscapeAction(this));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 99, AbstractMonster.Intent.ESCAPE));
                break;
            }
        }
    }

    @Override
    public void die() {
        super.die();
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
        this.setMove(MOVES[0], (byte)1, AbstractMonster.Intent.DEFEND);
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

