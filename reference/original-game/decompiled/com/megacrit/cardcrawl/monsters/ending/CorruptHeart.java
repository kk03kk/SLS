/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.ending;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.cards.status.VoidCard;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.HeartAnimListener;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.BeatOfDeathPower;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.InvinciblePower;
import com.megacrit.cardcrawl.powers.PainfulStabsPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.BloodShotEffect;
import com.megacrit.cardcrawl.vfx.combat.HeartBuffEffect;
import com.megacrit.cardcrawl.vfx.combat.HeartMegaDebuffEffect;
import com.megacrit.cardcrawl.vfx.combat.ViceCrushEffect;

public class CorruptHeart
extends AbstractMonster {
    public static final String ID = "CorruptHeart";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("CorruptHeart");
    public static final String NAME = CorruptHeart.monsterStrings.NAME;
    public static final String[] MOVES = CorruptHeart.monsterStrings.MOVES;
    public static final String[] DIALOG = CorruptHeart.monsterStrings.DIALOG;
    private HeartAnimListener animListener = new HeartAnimListener();
    private static final byte BLOOD_SHOTS = 1;
    private static final byte ECHO_ATTACK = 2;
    private static final byte DEBILITATE = 3;
    private static final byte GAIN_ONE_STRENGTH = 4;
    public static final int DEBUFF_AMT = -1;
    private int bloodHitCount;
    private boolean isFirstMove = true;
    private int moveCount = 0;
    private int buffCount = 0;

    public CorruptHeart() {
        super(NAME, ID, 750, 30.0f, -30.0f, 476.0f, 410.0f, null, -50.0f, 30.0f);
        this.loadAnimation("images/npcs/heart/skeleton.atlas", "images/npcs/heart/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTimeScale(1.5f);
        this.state.addListener(this.animListener);
        this.type = AbstractMonster.EnemyType.BOSS;
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(800);
        } else {
            this.setHp(750);
        }
        if (AbstractDungeon.ascensionLevel >= 4) {
            this.damage.add(new DamageInfo(this, 45));
            this.damage.add(new DamageInfo(this, 2));
            this.bloodHitCount = 15;
        } else {
            this.damage.add(new DamageInfo(this, 40));
            this.damage.add(new DamageInfo(this, 2));
            this.bloodHitCount = 12;
        }
    }

    @Override
    public void usePreBattleAction() {
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_ENDING");
        int invincibleAmt = 300;
        if (AbstractDungeon.ascensionLevel >= 19) {
            invincibleAmt -= 100;
        }
        int beatAmount = 1;
        if (AbstractDungeon.ascensionLevel >= 19) {
            ++beatAmount;
        }
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new InvinciblePower(this, invincibleAmt), invincibleAmt));
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new BeatOfDeathPower(this, beatAmount), beatAmount));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new HeartMegaDebuffEffect()));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new VulnerablePower(AbstractDungeon.player, 2, true), 2));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, 2, true), 2));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new FrailPower(AbstractDungeon.player, 2, true), 2));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new Dazed(), 1, true, false, false, (float)Settings.WIDTH * 0.2f, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new Slimed(), 1, true, false, false, (float)Settings.WIDTH * 0.35f, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new Wound(), 1, true, false, false, (float)Settings.WIDTH * 0.5f, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new Burn(), 1, true, false, false, (float)Settings.WIDTH * 0.65f, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new VoidCard(), 1, true, false, false, (float)Settings.WIDTH * 0.8f, (float)Settings.HEIGHT / 2.0f));
                break;
            }
            case 4: {
                int additionalAmount = 0;
                if (this.hasPower("Strength") && this.getPower((String)"Strength").amount < 0) {
                    additionalAmount = -this.getPower((String)"Strength").amount;
                }
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BorderFlashEffect(new Color(0.8f, 0.5f, 1.0f, 1.0f))));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new HeartBuffEffect(this.hb.cX, this.hb.cY)));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, additionalAmount + 2), additionalAmount + 2));
                switch (this.buffCount) {
                    case 0: {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ArtifactPower(this, 2), 2));
                        break;
                    }
                    case 1: {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new BeatOfDeathPower(this, 1), 1));
                        break;
                    }
                    case 2: {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new PainfulStabsPower(this)));
                        break;
                    }
                    case 3: {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, 10), 10));
                        break;
                    }
                    default: {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, 50), 50));
                    }
                }
                ++this.buffCount;
                break;
            }
            case 1: {
                if (Settings.FAST_MODE) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new BloodShotEffect(this.hb.cX, this.hb.cY, AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, this.bloodHitCount), 0.25f));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(new BloodShotEffect(this.hb.cX, this.hb.cY, AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, this.bloodHitCount), 0.6f));
                }
                for (int i = 0; i < this.bloodHitCount; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.BLUNT_HEAVY, true));
                }
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new ViceCrushEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY), 0.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (this.isFirstMove) {
            this.setMove((byte)3, AbstractMonster.Intent.STRONG_DEBUFF);
            this.isFirstMove = false;
            return;
        }
        switch (this.moveCount % 3) {
            case 0: {
                if (AbstractDungeon.aiRng.randomBoolean()) {
                    this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base, this.bloodHitCount, true);
                    break;
                }
                this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
                break;
            }
            case 1: {
                if (!this.lastMove((byte)2)) {
                    this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
                    break;
                }
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base, this.bloodHitCount, true);
                break;
            }
            default: {
                this.setMove((byte)4, AbstractMonster.Intent.BUFF);
            }
        }
        ++this.moveCount;
    }

    @Override
    public void die() {
        if (!AbstractDungeon.getCurrRoom().cannotLose) {
            super.die();
            this.state.removeListener(this.animListener);
            this.onBossVictoryLogic();
            this.onFinalBossVictoryLogic();
            CardCrawlGame.stopClock = true;
        }
    }
}

