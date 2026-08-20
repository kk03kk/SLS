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
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.FocusPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.SurroundedPower;

public class SpireShield
extends AbstractMonster {
    public static final String ID = "SpireShield";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SpireShield");
    public static final String NAME = SpireShield.monsterStrings.NAME;
    public static final String[] MOVES = SpireShield.monsterStrings.MOVES;
    public static final String[] DIALOG = SpireShield.monsterStrings.DIALOG;
    private int moveCount = 0;
    private static final byte BASH = 1;
    private static final byte FORTIFY = 2;
    private static final byte SMASH = 3;
    private static final int BASH_DEBUFF = -1;
    private static final int FORTIFY_BLOCK = 30;

    public SpireShield() {
        super(NAME, ID, 110, 0.0f, -20.0f, 380.0f, 290.0f, null, -1000.0f, 15.0f);
        this.type = AbstractMonster.EnemyType.ELITE;
        this.loadAnimation("images/monsters/theEnding/shield/skeleton.atlas", "images/monsters/theEnding/shield/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.1f);
        if (AbstractDungeon.ascensionLevel >= 8) {
            this.setHp(125);
        } else {
            this.setHp(110);
        }
        if (AbstractDungeon.ascensionLevel >= 3) {
            this.damage.add(new DamageInfo(this, 14));
            this.damage.add(new DamageInfo(this, 38));
        } else {
            this.damage.add(new DamageInfo(this, 12));
            this.damage.add(new DamageInfo(this, 34));
        }
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new SurroundedPower(AbstractDungeon.player)));
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
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.35f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                if (!AbstractDungeon.player.orbs.isEmpty() && AbstractDungeon.aiRng.randomBoolean()) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FocusPower(AbstractDungeon.player, -1), -1));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new StrengthPower(AbstractDungeon.player, -1), -1));
                break;
            }
            case 2: {
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)m, this, 30));
                }
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "OLD_ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                if (AbstractDungeon.ascensionLevel >= 18) {
                    AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, 99));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, ((DamageInfo)this.damage.get((int)1)).output));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        switch (this.moveCount % 3) {
            case 0: {
                if (AbstractDungeon.aiRng.randomBoolean()) {
                    this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
                    break;
                }
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base);
                break;
            }
            case 1: {
                if (!this.lastMove((byte)1)) {
                    this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)0)).base);
                    break;
                }
                this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
                break;
            }
            default: {
                this.setMove((byte)3, AbstractMonster.Intent.ATTACK_DEFEND, ((DamageInfo)this.damage.get((int)1)).base);
            }
        }
        ++this.moveCount;
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "OLD_ATTACK": {
                this.state.setAnimation(0, "old_attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
            case "ATTACK": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
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

