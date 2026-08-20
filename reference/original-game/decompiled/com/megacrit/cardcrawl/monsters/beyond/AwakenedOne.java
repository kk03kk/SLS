/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Bone;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.ClearCardQueueAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction;
import com.megacrit.cardcrawl.actions.animations.ShoutAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.EscapeAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.unique.CanLoseAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.VoidCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.CuriosityPower;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.UnawakenedPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.AwakenedEyeParticle;
import com.megacrit.cardcrawl.vfx.AwakenedWingParticle;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import com.megacrit.cardcrawl.vfx.combat.IntenseZoomEffect;
import com.megacrit.cardcrawl.vfx.combat.ShockWaveEffect;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AwakenedOne
extends AbstractMonster {
    private static final Logger logger = LogManager.getLogger(AwakenedOne.class.getName());
    public static final String ID = "AwakenedOne";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("AwakenedOne");
    public static final String NAME = AwakenedOne.monsterStrings.NAME;
    public static final String[] MOVES = AwakenedOne.monsterStrings.MOVES;
    public static final String[] DIALOG = AwakenedOne.monsterStrings.DIALOG;
    private boolean form1 = true;
    private boolean firstTurn = true;
    private boolean saidPower = false;
    public static final int STAGE_1_HP = 300;
    public static final int STAGE_2_HP = 300;
    public static final int A_9_STAGE_1_HP = 320;
    public static final int A_9_STAGE_2_HP = 320;
    private static final int A_4_STR = 2;
    private static final byte SLASH = 1;
    private static final byte SOUL_STRIKE = 2;
    private static final byte REBIRTH = 3;
    private static final String SS_NAME = MOVES[0];
    private static final int SLASH_DMG = 20;
    private static final int SS_DMG = 6;
    private static final int SS_AMT = 4;
    private static final int REGEN_AMT = 10;
    private static final int STR_AMT = 1;
    private static final byte DARK_ECHO = 5;
    private static final byte SLUDGE = 6;
    private static final byte TACKLE = 8;
    private static final String DARK_ECHO_NAME = MOVES[1];
    private static final String SLUDGE_NAME = MOVES[3];
    private static final int ECHO_DMG = 40;
    private static final int SLUDGE_DMG = 18;
    private static final int TACKLE_DMG = 10;
    private static final int TACKLE_AMT = 3;
    private float fireTimer = 0.0f;
    private static final float FIRE_TIME = 0.1f;
    private Bone eye;
    private Bone back;
    private boolean animateParticles = false;
    private ArrayList<AwakenedWingParticle> wParticles = new ArrayList();

    public AwakenedOne(float x, float y) {
        super(NAME, ID, 300, 40.0f, -30.0f, 460.0f, 250.0f, null, x, y);
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(320);
        } else {
            this.setHp(300);
        }
        this.loadAnimation("images/monsters/theForest/awakenedOne/skeleton.atlas", "images/monsters/theForest/awakenedOne/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle_1", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle_1", 0.3f);
        this.stateData.setMix("Hit", "Idle_2", 0.2f);
        this.stateData.setMix("Attack_1", "Idle_1", 0.2f);
        this.stateData.setMix("Attack_2", "Idle_2", 0.2f);
        this.state.getData().setMix("Idle_1", "Idle_2", 1.0f);
        this.eye = this.skeleton.findBone("Eye");
        for (Bone b : this.skeleton.getBones()) {
            logger.info(b.getData().getName());
        }
        this.back = this.skeleton.findBone("Hips");
        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = -200.0f * Settings.scale;
        this.dialogY = 10.0f * Settings.scale;
        this.damage.add(new DamageInfo(this, 20));
        this.damage.add(new DamageInfo(this, 6));
        this.damage.add(new DamageInfo(this, 40));
        this.damage.add(new DamageInfo(this, 18));
        this.damage.add(new DamageInfo(this, 10));
    }

    @Override
    public void usePreBattleAction() {
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BEYOND");
        AbstractDungeon.getCurrRoom().cannotLose = true;
        if (AbstractDungeon.ascensionLevel >= 19) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new RegenerateMonsterPower(this, 15)));
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new CuriosityPower(this, 2)));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new RegenerateMonsterPower(this, 10)));
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new CuriosityPower(this, 1)));
        }
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new UnawakenedPower(this)));
        if (AbstractDungeon.ascensionLevel >= 4) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, 2), 2));
        }
        UnlockTracker.markBossAsSeen("CROW");
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_AWAKENED_POUNCE"));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK_1"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                break;
            }
            case 2: {
                for (int i = 0; i < 4; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.FIRE));
                }
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_AWAKENEDONE_1"));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new IntenseZoomEffect(this.hb.cX, this.hb.cY, true), 0.05f, true));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "REBIRTH"));
                break;
            }
            case 5: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK_2"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.1f));
                this.firstTurn = false;
                AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_AWAKENEDONE_3"));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new ShockWaveEffect(this.hb.cX, this.hb.cY, new Color(0.1f, 0.0f, 0.2f, 1.0f), ShockWaveEffect.ShockWaveType.CHAOTIC), 0.3f));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new ShockWaveEffect(this.hb.cX, this.hb.cY, new Color(0.3f, 0.2f, 0.4f, 1.0f), ShockWaveEffect.ShockWaveType.CHAOTIC), 1.0f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(2), AbstractGameAction.AttackEffect.SMASH));
                break;
            }
            case 6: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK_2"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(3), AbstractGameAction.AttackEffect.POISON));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(new VoidCard(), 1, true, true));
                break;
            }
            case 8: {
                AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_AWAKENED_ATTACK"));
                for (int i = 0; i < 3; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
                    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.06f));
                    AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player, (DamageInfo)this.damage.get(4), AbstractGameAction.AttackEffect.FIRE, true));
                }
                break;
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "REBIRTH": {
                this.maxHealth = AbstractDungeon.ascensionLevel >= 9 ? 320 : 300;
                if (Settings.isEndless && AbstractDungeon.player.hasBlight("ToughEnemies")) {
                    float mod = AbstractDungeon.player.getBlight("ToughEnemies").effectFloat();
                    this.maxHealth = (int)((float)this.maxHealth * mod);
                }
                if (ModHelper.isModEnabled("MonsterHunter")) {
                    this.currentHealth = (int)((float)this.currentHealth * 1.5f);
                }
                this.state.setAnimation(0, "Idle_2", true);
                this.halfDead = false;
                this.animateParticles = true;
                AbstractDungeon.actionManager.addToBottom(new HealAction(this, this, this.maxHealth));
                AbstractDungeon.actionManager.addToBottom(new CanLoseAction());
                break;
            }
            case "ATTACK_1": {
                this.state.setAnimation(0, "Attack_1", false);
                this.state.addAnimation(0, "Idle_1", true, 0.0f);
                break;
            }
            case "ATTACK_2": {
                this.state.setAnimation(0, "Attack_2", false);
                this.state.addAnimation(0, "Idle_2", true, 0.0f);
                break;
            }
        }
    }

    @Override
    protected void getMove(int num) {
        if (this.form1) {
            if (this.firstTurn) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, 20);
                this.firstTurn = false;
                return;
            }
            if (num < 25) {
                if (!this.lastMove((byte)2)) {
                    this.setMove(SS_NAME, (byte)2, AbstractMonster.Intent.ATTACK, 6, 4, true);
                } else {
                    this.setMove((byte)1, AbstractMonster.Intent.ATTACK, 20);
                }
            } else if (!this.lastTwoMoves((byte)1)) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, 20);
            } else {
                this.setMove(SS_NAME, (byte)2, AbstractMonster.Intent.ATTACK, 6, 4, true);
            }
        } else {
            if (this.firstTurn) {
                this.setMove(DARK_ECHO_NAME, (byte)5, AbstractMonster.Intent.ATTACK, 40);
                return;
            }
            if (num < 50) {
                if (!this.lastTwoMoves((byte)6)) {
                    this.setMove(SLUDGE_NAME, (byte)6, AbstractMonster.Intent.ATTACK_DEBUFF, 18);
                } else {
                    this.setMove((byte)8, AbstractMonster.Intent.ATTACK, 10, 3, true);
                }
            } else if (!this.lastTwoMoves((byte)8)) {
                this.setMove((byte)8, AbstractMonster.Intent.ATTACK, 10, 3, true);
            } else {
                this.setMove(SLUDGE_NAME, (byte)6, AbstractMonster.Intent.ATTACK_DEBUFF, 18);
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            if (this.form1) {
                this.state.addAnimation(0, "Idle_1", true, 0.0f);
            } else {
                this.state.addAnimation(0, "Idle_2", true, 0.0f);
            }
        }
        if (this.currentHealth <= 0 && !this.halfDead) {
            if (AbstractDungeon.getCurrRoom().cannotLose) {
                this.halfDead = true;
            }
            for (AbstractPower p : this.powers) {
                p.onDeath();
            }
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onMonsterDeath(this);
            }
            this.addToTop(new ClearCardQueueAction());
            Iterator s = this.powers.iterator();
            while (s.hasNext()) {
                AbstractPower p;
                p = (AbstractPower)s.next();
                if (p.type != AbstractPower.PowerType.DEBUFF && !p.ID.equals("Curiosity") && !p.ID.equals("Unawakened") && !p.ID.equals("Shackled")) continue;
                s.remove();
            }
            this.setMove((byte)3, AbstractMonster.Intent.UNKNOWN);
            this.createIntent();
            AbstractDungeon.actionManager.addToBottom(new ShoutAction(this, DIALOG[0]));
            AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 3, AbstractMonster.Intent.UNKNOWN));
            this.applyPowers();
            this.firstTurn = true;
            this.form1 = false;
            if (GameActionManager.turn <= 1) {
                UnlockTracker.unlockAchievement("YOU_ARE_NOTHING");
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (!this.isDying && this.animateParticles) {
            this.fireTimer -= Gdx.graphics.getDeltaTime();
            if (this.fireTimer < 0.0f) {
                this.fireTimer = 0.1f;
                AbstractDungeon.effectList.add(new AwakenedEyeParticle(this.skeleton.getX() + this.eye.getWorldX(), this.skeleton.getY() + this.eye.getWorldY()));
                this.wParticles.add(new AwakenedWingParticle());
            }
        }
        Iterator<AwakenedWingParticle> p = this.wParticles.iterator();
        while (p.hasNext()) {
            AwakenedWingParticle e = p.next();
            e.update();
            if (!e.isDone) continue;
            p.remove();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        for (AwakenedWingParticle p : this.wParticles) {
            if (!p.renderBehind) continue;
            p.render(sb, this.skeleton.getX() + this.back.getWorldX(), this.skeleton.getY() + this.back.getWorldY());
        }
        super.render(sb);
        for (AwakenedWingParticle p : this.wParticles) {
            if (p.renderBehind) continue;
            p.render(sb, this.skeleton.getX() + this.back.getWorldX(), this.skeleton.getY() + this.back.getWorldY());
        }
    }

    @Override
    public void die() {
        if (!AbstractDungeon.getCurrRoom().cannotLose) {
            super.die();
            this.useFastShakeAnimation(5.0f);
            CardCrawlGame.screenShake.rumble(4.0f);
            if (this.saidPower) {
                CardCrawlGame.sound.play("VO_AWAKENEDONE_2");
                AbstractDungeon.effectList.add(new SpeechBubble(this.hb.cX + this.dialogX, this.hb.cY + this.dialogY, 2.5f, DIALOG[1], false));
                this.saidPower = true;
            }
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (m.isDying || !(m instanceof Cultist)) continue;
                AbstractDungeon.actionManager.addToBottom(new EscapeAction(m));
            }
            this.onBossVictoryLogic();
            UnlockTracker.hardUnlockOverride("CROW");
            UnlockTracker.unlockAchievement("CROW");
            this.onFinalBossVictoryLogic();
        }
    }
}

