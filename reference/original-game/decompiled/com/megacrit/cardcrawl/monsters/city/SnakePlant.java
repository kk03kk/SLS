/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.MalleablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.vfx.combat.BiteEffect;

public class SnakePlant
extends AbstractMonster {
    public static final String ID = "SnakePlant";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SnakePlant");
    public static final String NAME = SnakePlant.monsterStrings.NAME;
    public static final String[] MOVES = SnakePlant.monsterStrings.MOVES;
    public static final String[] DIALOG = SnakePlant.monsterStrings.DIALOG;
    private static final int HP_MIN = 75;
    private static final int HP_MAX = 79;
    private static final int A_2_HP_MIN = 78;
    private static final int A_2_HP_MAX = 82;
    private static final byte CHOMPY_CHOMPS = 1;
    private static final byte SPORES = 2;
    private static final int CHOMPY_AMT = 3;
    private static final int CHOMPY_DMG = 7;
    private static final int A_2_CHOMPY_DMG = 8;
    private int rainBlowsDmg;

    public SnakePlant(float x, float y) {
        super(NAME, ID, 79, 0.0f, -44.0f, 350.0f, 360.0f, null, x, y + 50.0f);
        this.loadAnimation("images/monsters/theCity/snakePlant/skeleton.atlas", "images/monsters/theCity/snakePlant/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.8f);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(78, 82);
        } else {
            this.setHp(75, 79);
        }
        this.rainBlowsDmg = AbstractDungeon.ascensionLevel >= 2 ? 8 : 7;
        this.damage.add(new DamageInfo(this, this.rainBlowsDmg));
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new MalleablePower(this)));
    }

    @Override
    public void changeState(String stateName) {
        switch (stateName) {
            case "ATTACK": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    public void takeTurn() {
        AbstractPlayer p = AbstractDungeon.player;
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                int numBlows = 3;
                for (int i = 0; i < numBlows; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new BiteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-50.0f, 50.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-50.0f, 50.0f) * Settings.scale, Color.CHARTREUSE.cpy()), 0.2f));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction(p, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.NONE, true));
                }
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, 2, true), 2));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, 2, true), 2));
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (AbstractDungeon.ascensionLevel >= 17) {
            if (num < 65) {
                if (this.lastTwoMoves((byte)1)) {
                    this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
                } else {
                    this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 3, true);
                }
            } else if (this.lastMove((byte)2) || this.lastMoveBefore((byte)2)) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 3, true);
            } else {
                this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
            }
        } else if (num < 65) {
            if (this.lastTwoMoves((byte)1)) {
                this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
            } else {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 3, true);
            }
        } else if (this.lastMove((byte)2)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 3, true);
        } else {
            this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
        }
    }
}

