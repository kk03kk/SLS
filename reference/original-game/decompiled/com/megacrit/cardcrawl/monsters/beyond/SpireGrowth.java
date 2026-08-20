/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ConstrictedPower;

public class SpireGrowth
extends AbstractMonster {
    public static final String ID = "Serpent";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Serpent");
    public static final String NAME = SpireGrowth.monsterStrings.NAME;
    public static final String[] MOVES = SpireGrowth.monsterStrings.MOVES;
    public static final String[] DIALOG = SpireGrowth.monsterStrings.DIALOG;
    private static final int START_HP = 170;
    private static final int A_2_START_HP = 190;
    private int tackleDmg = 16;
    private int smashDmg = 22;
    private int constrictDmg = 10;
    private int A_2_tackleDmg = 18;
    private int A_2_smashDmg = 25;
    private int tackleDmgActual;
    private int smashDmgActual;
    private static final byte QUICK_TACKLE = 1;
    private static final byte CONSTRICT = 2;
    private static final byte SMASH = 3;

    public SpireGrowth() {
        super(NAME, ID, 170, -10.0f, -35.0f, 480.0f, 430.0f, null, 0.0f, 10.0f);
        this.loadAnimation("images/monsters/theForest/spireGrowth/skeleton.atlas", "images/monsters/theForest/spireGrowth/skeleton.json", 1.0f);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(190);
        } else {
            this.setHp(170);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.tackleDmgActual = this.A_2_tackleDmg;
            this.smashDmgActual = this.A_2_smashDmg;
        } else {
            this.tackleDmgActual = this.tackleDmg;
            this.smashDmgActual = this.smashDmg;
        }
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        e.setTimeScale(1.3f);
        this.stateData.setMix("Hurt", "Idle", 0.2f);
        this.stateData.setMix("Idle", "Hurt", 0.2f);
        this.damage.add(new DamageInfo(this, this.tackleDmgActual));
        this.damage.add(new DamageInfo(this, this.smashDmgActual));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                if (AbstractDungeon.ascensionLevel >= 17) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new ConstrictedPower(AbstractDungeon.player, this, this.constrictDmg + 2)));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new ConstrictedPower(AbstractDungeon.player, this, this.constrictDmg)));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (AbstractDungeon.ascensionLevel >= 17 && !AbstractDungeon.player.hasPower("Constricted") && !this.lastMove((byte)2)) {
            this.setMove((byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
            return;
        }
        if (num < 50 && !this.lastTwoMoves((byte)1)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            return;
        }
        if (!AbstractDungeon.player.hasPower("Constricted") && !this.lastMove((byte)2)) {
            this.setMove((byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
            return;
        }
        if (!this.lastTwoMoves((byte)3)) {
            this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
            return;
        }
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hurt", false);
            this.state.setTimeScale(1.3f);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "ATTACK": {
                this.state.setAnimation(0, "Attack", false);
                this.state.setTimeScale(1.3f);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
        }
    }
}

