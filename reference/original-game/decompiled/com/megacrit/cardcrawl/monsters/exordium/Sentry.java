/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.FastShakeAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.ShockWaveEffect;
import com.megacrit.cardcrawl.vfx.combat.SmallLaserEffect;

public class Sentry
extends AbstractMonster {
    public static final String ID = "Sentry";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Sentry");
    public static final String NAME = Sentry.monsterStrings.NAME;
    public static final String[] MOVES = Sentry.monsterStrings.MOVES;
    public static final String[] DIALOG = Sentry.monsterStrings.DIALOG;
    public static final String ENC_NAME = "Sentries";
    private static final int HP_MIN = 38;
    private static final int HP_MAX = 42;
    private static final int A_2_HP_MIN = 39;
    private static final int A_2_HP_MAX = 45;
    private static final byte BOLT = 3;
    private static final byte BEAM = 4;
    private int beamDmg;
    private int dazedAmt;
    private static final int DAZED_AMT = 2;
    private static final int A_18_DAZED_AMT = 3;
    private boolean firstMove = true;

    public Sentry(float x, float y) {
        super(NAME, ID, 42, 0.0f, -5.0f, 180.0f, 310.0f, null, x, y);
        this.type = AbstractMonster.EnemyType.ELITE;
        if (AbstractDungeon.ascensionLevel >= 8) {
            this.setHp(39, 45);
        } else {
            this.setHp(38, 42);
        }
        this.beamDmg = AbstractDungeon.ascensionLevel >= 3 ? 10 : 9;
        this.dazedAmt = AbstractDungeon.ascensionLevel >= 18 ? 3 : 2;
        this.damage.add(new DamageInfo(this, this.beamDmg));
        this.loadAnimation("images/monsters/theBottom/sentry/skeleton.atlas", "images/monsters/theBottom/sentry/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTimeScale(2.0f);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("idle", "attack", 0.1f);
        this.stateData.setMix("idle", "spaz1", 0.1f);
        this.stateData.setMix("idle", "hit", 0.1f);
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ArtifactPower(this, 1)));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new SFXAction("THUNDERCLAP"));
                if (!Settings.FAST_MODE) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new ShockWaveEffect(this.hb.cX, this.hb.cY, Color.ROYAL, ShockWaveEffect.ShockWaveType.ADDITIVE), 0.5f));
                    AbstractDungeon.actionManager.addToBottom(new FastShakeAction(AbstractDungeon.player, 0.6f, 0.2f));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new ShockWaveEffect(this.hb.cX, this.hb.cY, Color.ROYAL, ShockWaveEffect.ShockWaveType.ADDITIVE), 0.1f));
                    AbstractDungeon.actionManager.addToBottom(new FastShakeAction(AbstractDungeon.player, 0.6f, 0.15f));
                }
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Dazed(), this.dazedAmt));
                break;
            }
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new SFXAction("ATTACK_MAGIC_BEAM_SHORT", 0.5f));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BorderFlashEffect(Color.SKY)));
                if (Settings.FAST_MODE) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new SmallLaserEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, this.hb.cX, this.hb.cY), 0.1f));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new SmallLaserEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, this.hb.cX, this.hb.cY), 0.3f));
                }
                AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.NONE, Settings.FAST_MODE));
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "hit", false);
            this.state.addAnimation(0, "idle", true, 0.0f);
        }
    }

    @Override
    public void changeState(String stateName) {
        switch (stateName) {
            case "ATTACK": {
                this.state.setAnimation(0, "attack", false);
                this.state.addAnimation(0, "idle", true, 0.0f);
            }
        }
    }

    @Override
    protected void getMove(int num) {
        if (this.firstMove) {
            if (AbstractDungeon.getMonsters().monsters.lastIndexOf(this) % 2 == 0) {
                this.setMove((byte)3, AbstractMonster.Intent.DEBUFF);
            } else {
                this.setMove((byte)4, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            }
            this.firstMove = false;
            return;
        }
        if (this.lastMove((byte)4)) {
            this.setMove((byte)3, AbstractMonster.Intent.DEBUFF);
        } else {
            this.setMove((byte)4, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
        }
    }
}

