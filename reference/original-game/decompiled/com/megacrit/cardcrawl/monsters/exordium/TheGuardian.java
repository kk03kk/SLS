/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.utility.LoseBlockAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ModeShiftPower;
import com.megacrit.cardcrawl.powers.SharpHidePower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.combat.CleaveEffect;
import com.megacrit.cardcrawl.vfx.combat.IntenseZoomEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheGuardian
extends AbstractMonster {
    private static final Logger logger = LogManager.getLogger(TheGuardian.class.getName());
    public static final String ID = "TheGuardian";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("TheGuardian");
    public static final String NAME = TheGuardian.monsterStrings.NAME;
    public static final String[] MOVES = TheGuardian.monsterStrings.MOVES;
    public static final String[] DIALOG = TheGuardian.monsterStrings.DIALOG;
    private static final String DEFENSIVE_MODE = "Defensive Mode";
    private static final String OFFENSIVE_MODE = "Offensive Mode";
    private static final String RESET_THRESH = "Reset Threshold";
    public static final int HP = 240;
    public static final int A_2_HP = 250;
    private static final int DMG_THRESHOLD = 30;
    private static final int A_2_DMG_THRESHOLD = 35;
    private static final int A_19_DMG_THRESHOLD = 40;
    private int dmgThreshold;
    private int dmgThresholdIncrease = 10;
    private int dmgTaken;
    private static final int FIERCE_BASH_DMG = 32;
    private static final int A_2_FIERCE_BASH_DMG = 36;
    private static final int ROLL_DMG = 9;
    private static final int A_2_ROLL_DMG = 10;
    private int fierceBashDamage;
    private int whirlwindDamage = 5;
    private int twinSlamDamage = 8;
    private int rollDamage;
    private int whirlwindCount = 4;
    private int DEFENSIVE_BLOCK = 20;
    private int blockAmount = 9;
    private int thornsDamage = 3;
    private int VENT_DEBUFF = 2;
    private boolean isOpen = true;
    private boolean closeUpTriggered = false;
    private static final byte CLOSE_UP = 1;
    private static final byte FIERCE_BASH = 2;
    private static final byte ROLL_ATTACK = 3;
    private static final byte TWIN_SLAM = 4;
    private static final byte WHIRLWIND = 5;
    private static final byte CHARGE_UP = 6;
    private static final byte VENT_STEAM = 7;
    private static final String CLOSEUP_NAME = MOVES[0];
    private static final String FIERCEBASH_NAME = MOVES[1];
    private static final String TWINSLAM_NAME = MOVES[3];
    private static final String WHIRLWIND_NAME = MOVES[4];
    private static final String CHARGEUP_NAME = MOVES[5];
    private static final String VENTSTEAM_NAME = MOVES[6];

    public TheGuardian() {
        super(NAME, ID, 240, 0.0f, 95.0f, 440.0f, 350.0f, null, -50.0f, -100.0f);
        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = -100.0f * Settings.scale;
        this.dialogY = 50.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 19) {
            this.setHp(250);
            this.dmgThreshold = 40;
        } else if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(250);
            this.dmgThreshold = 35;
        } else {
            this.setHp(240);
            this.dmgThreshold = 30;
        }
        if (AbstractDungeon.ascensionLevel >= 4) {
            this.fierceBashDamage = 36;
            this.rollDamage = 10;
        } else {
            this.fierceBashDamage = 32;
            this.rollDamage = 9;
        }
        this.damage.add(new DamageInfo(this, this.fierceBashDamage));
        this.damage.add(new DamageInfo(this, this.rollDamage));
        this.damage.add(new DamageInfo(this, this.whirlwindDamage));
        this.damage.add(new DamageInfo(this, this.twinSlamDamage));
        this.loadAnimation("images/monsters/theBottom/boss/guardian/skeleton.atlas", "images/monsters/theBottom/boss/guardian/skeleton.json", 2.0f);
        this.state.setAnimation(0, "idle", true);
    }

    @Override
    public void usePreBattleAction() {
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
            CardCrawlGame.music.unsilenceBGM();
            AbstractDungeon.scene.fadeOutAmbiance();
            AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BOTTOM");
        }
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ModeShiftPower(this, this.dmgThreshold)));
        AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, RESET_THRESH));
        UnlockTracker.markBossAsSeen("GUARDIAN");
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                this.useCloseUp();
                break;
            }
            case 2: {
                this.useFierceBash();
                break;
            }
            case 7: {
                this.useVentSteam();
                break;
            }
            case 3: {
                this.useRollAttack();
                break;
            }
            case 4: {
                this.useTwinSmash();
                break;
            }
            case 5: {
                this.useWhirlwind();
                break;
            }
            case 6: {
                this.useChargeUp();
                break;
            }
            default: {
                logger.info("ERROR");
            }
        }
    }

    private void useFierceBash() {
        AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
        AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        this.setMove(VENTSTEAM_NAME, (byte)7, AbstractMonster.Intent.STRONG_DEBUFF);
    }

    private void useVentSteam() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, this.VENT_DEBUFF, true), this.VENT_DEBUFF));
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new VulnerablePower(AbstractDungeon.player, this.VENT_DEBUFF, true), this.VENT_DEBUFF));
        this.setMove(WHIRLWIND_NAME, (byte)5, AbstractMonster.Intent.ATTACK, this.whirlwindDamage, this.whirlwindCount, true);
    }

    private void useCloseUp() {
        AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction((AbstractCreature)this, DIALOG[1]));
        if (AbstractDungeon.ascensionLevel >= 19) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new SharpHidePower(this, this.thornsDamage + 1)));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new SharpHidePower(this, this.thornsDamage)));
        }
        this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
    }

    private void useTwinSmash() {
        AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, OFFENSIVE_MODE));
        AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(3), AbstractGameAction.AttackEffect.SLASH_HEAVY));
        AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(3), AbstractGameAction.AttackEffect.SLASH_HEAVY));
        AbstractDungeon.actionManager.addToBottom(new RemoveSpecificPowerAction((AbstractCreature)this, (AbstractCreature)this, "Sharp Hide"));
        this.setMove(WHIRLWIND_NAME, (byte)5, AbstractMonster.Intent.ATTACK, this.whirlwindDamage, this.whirlwindCount, true);
    }

    private void useRollAttack() {
        AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
        AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        this.setMove(TWINSLAM_NAME, (byte)4, AbstractMonster.Intent.ATTACK_BUFF, this.twinSlamDamage, 2, true);
    }

    private void useWhirlwind() {
        AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
        AbstractDungeon.actionManager.addToBottom(new SFXAction("ATTACK_WHIRLWIND"));
        for (int i = 0; i < this.whirlwindCount; ++i) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("ATTACK_HEAVY"));
            AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new CleaveEffect(true), 0.15f));
            AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player, (DamageInfo)this.damage.get(2), AbstractGameAction.AttackEffect.NONE, true));
        }
        this.setMove(CHARGEUP_NAME, (byte)6, AbstractMonster.Intent.DEFEND);
    }

    private void useChargeUp() {
        AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, this.blockAmount));
        AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_GUARDIAN_DESTROY"));
        AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[2], 1.0f, 2.5f));
        this.setMove(FIERCEBASH_NAME, (byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
    }

    @Override
    protected void getMove(int num) {
        if (this.isOpen) {
            this.setMove(CHARGEUP_NAME, (byte)6, AbstractMonster.Intent.DEFEND);
        } else {
            this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
        }
    }

    @Override
    public void changeState(String stateName) {
        switch (stateName) {
            case "Defensive Mode": {
                AbstractDungeon.actionManager.addToBottom(new RemoveSpecificPowerAction((AbstractCreature)this, (AbstractCreature)this, "Mode Shift"));
                CardCrawlGame.sound.play("GUARDIAN_ROLL_UP");
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, this.DEFENSIVE_BLOCK));
                this.stateData.setMix("idle", "transition", 0.1f);
                this.state.setTimeScale(2.0f);
                this.state.setAnimation(0, "transition", false);
                this.state.addAnimation(0, "defensive", true, 0.0f);
                this.dmgThreshold += this.dmgThresholdIncrease;
                this.setMove(CLOSEUP_NAME, (byte)1, AbstractMonster.Intent.BUFF);
                this.createIntent();
                this.isOpen = false;
                this.updateHitbox(0.0f, 95.0f, 440.0f, 250.0f);
                this.healthBarUpdatedEvent();
                break;
            }
            case "Offensive Mode": {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ModeShiftPower(this, this.dmgThreshold)));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, RESET_THRESH));
                if (this.currentBlock != 0) {
                    AbstractDungeon.actionManager.addToBottom(new LoseBlockAction(this, this, this.currentBlock));
                }
                this.stateData.setMix("defensive", "idle", 0.2f);
                this.state.setTimeScale(1.0f);
                this.state.setAnimation(0, "idle", true);
                this.isOpen = true;
                this.closeUpTriggered = false;
                this.updateHitbox(0.0f, 95.0f, 440.0f, 350.0f);
                this.healthBarUpdatedEvent();
                break;
            }
            case "Reset Threshold": {
                this.dmgTaken = 0;
                break;
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        int tmpHealth = this.currentHealth;
        super.damage(info);
        if (this.isOpen && !this.closeUpTriggered && tmpHealth > this.currentHealth && !this.isDying) {
            this.dmgTaken += tmpHealth - this.currentHealth;
            if (this.getPower("Mode Shift") != null) {
                this.getPower((String)"Mode Shift").amount -= tmpHealth - this.currentHealth;
                this.getPower("Mode Shift").updateDescription();
            }
            if (this.dmgTaken >= this.dmgThreshold) {
                this.dmgTaken = 0;
                AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new IntenseZoomEffect(this.hb.cX, this.hb.cY, false), 0.05f, true));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, DEFENSIVE_MODE));
                this.closeUpTriggered = true;
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);
    }

    @Override
    public void die() {
        this.useFastShakeAnimation(5.0f);
        CardCrawlGame.screenShake.rumble(4.0f);
        super.die();
        this.onBossVictoryLogic();
        UnlockTracker.hardUnlockOverride("GUARDIAN");
        UnlockTracker.unlockAchievement("GUARDIAN");
    }
}

