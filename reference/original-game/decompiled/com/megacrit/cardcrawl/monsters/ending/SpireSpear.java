/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.ending;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class SpireSpear
extends AbstractMonster {
    public static final String ID = "SpireSpear";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SpireSpear");
    public static final String NAME = SpireSpear.monsterStrings.NAME;
    public static final String[] MOVES = SpireSpear.monsterStrings.MOVES;
    public static final String[] DIALOG = SpireSpear.monsterStrings.DIALOG;
    private int moveCount = 0;
    private static final byte BURN_STRIKE = 1;
    private static final byte PIERCER = 2;
    private static final byte SKEWER = 3;
    private static final int BURN_STRIKE_COUNT = 2;
    private int skewerCount;

    public SpireSpear() {
        super(NAME, ID, 160, 0.0f, -15.0f, 380.0f, 290.0f, null, 70.0f, 10.0f);
        this.type = AbstractMonster.EnemyType.ELITE;
        this.loadAnimation("images/monsters/theEnding/spear/skeleton.atlas", "images/monsters/theEnding/spear/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.7f);
        if (AbstractDungeon.ascensionLevel >= 8) {
            this.setHp(180);
        } else {
            this.setHp(160);
        }
        if (AbstractDungeon.ascensionLevel >= 3) {
            this.skewerCount = 4;
            this.damage.add(new DamageInfo(this, 6));
            this.damage.add(new DamageInfo(this, 10));
        } else {
            this.skewerCount = 3;
            this.damage.add(new DamageInfo(this, 5));
            this.damage.add(new DamageInfo(this, 10));
        }
    }

    @Override
    public void usePreBattleAction() {
        if (AbstractDungeon.ascensionLevel >= 18) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ArtifactPower(this, 2)));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ArtifactPower(this, 1)));
        }
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                for (int i = 0; i < 2; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.15f));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
                }
                if (AbstractDungeon.ascensionLevel >= 18) {
                    AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new Burn(), 2, false, true));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Burn(), 2));
                break;
            }
            case 2: {
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, this, new StrengthPower(m, 2), 2));
                }
                break;
            }
            case 3: {
                for (int i = 0; i < this.skewerCount; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.05f));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_DIAGONAL, true));
                }
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        switch (this.moveCount % 3) {
            case 0: {
                if (!this.lastMove((byte)1)) {
                    this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
                    break;
                }
                this.setMove((byte)2, AbstractMonster.Intent.BUFF);
                break;
            }
            case 1: {
                this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base, this.skewerCount, true);
                break;
            }
            default: {
                if (AbstractDungeon.aiRng.randomBoolean()) {
                    this.setMove((byte)2, AbstractMonster.Intent.BUFF);
                    break;
                }
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
            }
        }
        ++this.moveCount;
    }

    @Override
    public void changeState(String key) {
        AnimationState.TrackEntry e = null;
        switch (key) {
            case "SLOW_ATTACK": {
                this.state.setAnimation(0, "Attack_1", false);
                e = this.state.addAnimation(0, "Idle", true, 0.0f);
                e.setTimeScale(0.5f);
                break;
            }
            case "ATTACK": {
                this.state.setAnimation(0, "Attack_2", false);
                e = this.state.addAnimation(0, "Idle", true, 0.0f);
                e.setTimeScale(0.7f);
                break;
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            AnimationState.TrackEntry e = this.state.addAnimation(0, "Idle", true, 0.0f);
            e.setTimeScale(0.7f);
        }
    }

    @Override
    public void die() {
        super.die();
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (m.isDead || m.isDying) continue;
            if (AbstractDungeon.player.hasPower("Surrounded")) {
                AbstractDungeon.player.flipHorizontal = m.drawX < AbstractDungeon.player.drawX;
                AbstractDungeon.actionManager.addToBottom(new RemoveSpecificPowerAction((AbstractCreature)AbstractDungeon.player, (AbstractCreature)AbstractDungeon.player, "Surrounded"));
            }
            if (!m.hasPower("BackAttack")) continue;
            AbstractDungeon.actionManager.addToBottom(new RemoveSpecificPowerAction((AbstractCreature)m, (AbstractCreature)m, "BackAttack"));
        }
    }
}

