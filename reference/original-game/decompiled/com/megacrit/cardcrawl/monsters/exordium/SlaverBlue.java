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
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.WeakPower;

public class SlaverBlue
extends AbstractMonster {
    public static final String ID = "SlaverBlue";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SlaverBlue");
    public static final String NAME = SlaverBlue.monsterStrings.NAME;
    public static final String[] MOVES = SlaverBlue.monsterStrings.MOVES;
    public static final String[] DIALOG = SlaverBlue.monsterStrings.DIALOG;
    private static final int HP_MIN = 46;
    private static final int HP_MAX = 50;
    private static final int A_2_HP_MIN = 48;
    private static final int A_2_HP_MAX = 52;
    private static final int STAB_DMG = 12;
    private static final int A_2_STAB_DMG = 13;
    private static final int RAKE_DMG = 7;
    private static final int A_2_RAKE_DMG = 8;
    private int stabDmg = 12;
    private int rakeDmg = 7;
    private int weakAmt = 1;
    private static final byte STAB = 1;
    private static final byte RAKE = 4;

    public SlaverBlue(float x, float y) {
        super(NAME, ID, 50, 0.0f, 0.0f, 170.0f, 230.0f, null, x, y);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(48, 52);
        } else {
            this.setHp(46, 50);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.stabDmg = 13;
            this.rakeDmg = 8;
        } else {
            this.stabDmg = 12;
            this.rakeDmg = 7;
        }
        this.damage.add(new DamageInfo(this, this.stabDmg));
        this.damage.add(new DamageInfo(this, this.rakeDmg));
        this.loadAnimation("images/monsters/theBottom/blueSlaver/skeleton.atlas", "images/monsters/theBottom/blueSlaver/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                this.playSfx();
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
                break;
            }
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                if (AbstractDungeon.ascensionLevel >= 17) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, this.weakAmt + 1, true), this.weakAmt + 1));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, this.weakAmt, true), this.weakAmt));
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    private void playSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_SLAVERBLUE_1A"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_SLAVERBLUE_1B"));
        }
    }

    private void playDeathSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            CardCrawlGame.sound.play("VO_SLAVERBLUE_2A");
        } else {
            CardCrawlGame.sound.play("VO_SLAVERBLUE_2B");
        }
    }

    @Override
    protected void getMove(int num) {
        if (num >= 40 && !this.lastTwoMoves((byte)1)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            return;
        }
        if (AbstractDungeon.ascensionLevel >= 17) {
            if (!this.lastMove((byte)4)) {
                this.setMove(MOVES[0], (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
                return;
            }
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            return;
        }
        if (!this.lastTwoMoves((byte)4)) {
            this.setMove(MOVES[0], (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
            return;
        }
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
    }

    @Override
    public void die() {
        super.die();
        this.playDeathSfx();
    }
}

