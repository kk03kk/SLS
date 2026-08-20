/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.animations.ShoutAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.vfx.combat.BiteEffect;

public class Maw
extends AbstractMonster {
    public static final String ID = "Maw";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Maw");
    public static final String NAME = Maw.monsterStrings.NAME;
    public static final String[] MOVES = Maw.monsterStrings.MOVES;
    public static final String[] DIALOG = Maw.monsterStrings.DIALOG;
    private static final int HP = 300;
    private static final float HB_X = 0.0f;
    private static final float HB_Y = -40.0f;
    private static final float HB_W = 430.0f;
    private static final float HB_H = 360.0f;
    private static final int SLAM_DMG = 25;
    private static final int NOM_DMG = 5;
    private static final int A_2_SLAM_DMG = 30;
    private int slamDmg;
    private int nomDmg;
    private static final byte ROAR = 2;
    private static final byte SLAM = 3;
    private static final byte DROOL = 4;
    private static final byte NOMNOMNOM = 5;
    private boolean roared = false;
    private int turnCount = 1;
    private int strUp;
    private int terrifyDur;

    public Maw(float x, float y) {
        super(NAME, ID, 300, 0.0f, -40.0f, 430.0f, 360.0f, null, x, y);
        this.loadAnimation("images/monsters/theForest/maw/skeleton.atlas", "images/monsters/theForest/maw/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.dialogX = -160.0f * Settings.scale;
        this.dialogY = 40.0f * Settings.scale;
        this.strUp = 3;
        this.terrifyDur = 3;
        if (AbstractDungeon.ascensionLevel >= 17) {
            this.strUp += 2;
            this.terrifyDur += 2;
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.slamDmg = 30;
            this.nomDmg = 5;
        } else {
            this.slamDmg = 25;
            this.nomDmg = 5;
        }
        this.damage.add(new DamageInfo(this, this.slamDmg));
        this.damage.add(new DamageInfo(this, this.nomDmg));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new SFXAction("MAW_DEATH", 0.1f));
                AbstractDungeon.actionManager.addToBottom(new ShoutAction(this, DIALOG[0], 1.0f, 2.0f));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, this.terrifyDur, true), this.terrifyDur));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, this.terrifyDur, true), this.terrifyDur));
                this.roared = true;
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                break;
            }
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, this.strUp), this.strUp));
                break;
            }
            case 5: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                for (int i = 0; i < this.turnCount / 2; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new BiteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-50.0f, 50.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-50.0f, 50.0f) * Settings.scale, Color.SKY.cpy())));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.NONE));
                }
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        ++this.turnCount;
        if (!this.roared) {
            this.setMove((byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
            return;
        }
        if (num < 50 && !this.lastMove((byte)5)) {
            if (this.turnCount / 2 <= 1) {
                this.setMove((byte)5, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
            } else {
                this.setMove((byte)5, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base, this.turnCount / 2, true);
            }
            return;
        }
        if (this.lastMove((byte)3) || this.lastMove((byte)5)) {
            this.setMove((byte)4, AbstractMonster.Intent.BUFF);
            return;
        }
        this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
    }

    @Override
    public void die() {
        super.die();
        CardCrawlGame.sound.play("MAW_DEATH");
    }
}

