/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateJumpAction;
import com.megacrit.cardcrawl.actions.animations.AnimateShakeAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.animations.ShoutAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import com.megacrit.cardcrawl.actions.common.SuicideAction;
import com.megacrit.cardcrawl.actions.unique.CanLoseAction;
import com.megacrit.cardcrawl.actions.unique.CannotLoseAction;
import com.megacrit.cardcrawl.actions.utility.HideHealthBarAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.ShakeScreenAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_L;
import com.megacrit.cardcrawl.powers.SplitPower;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.combat.WeightyImpactEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlimeBoss
extends AbstractMonster {
    private static final Logger logger = LogManager.getLogger(SlimeBoss.class.getName());
    public static final String ID = "SlimeBoss";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SlimeBoss");
    public static final String NAME = SlimeBoss.monsterStrings.NAME;
    public static final String[] MOVES = SlimeBoss.monsterStrings.MOVES;
    public static final String[] DIALOG = SlimeBoss.monsterStrings.DIALOG;
    public static final int HP = 140;
    public static final int A_2_HP = 150;
    public static final int TACKLE_DAMAGE = 9;
    public static final int SLAM_DAMAGE = 35;
    public static final int A_2_TACKLE_DAMAGE = 10;
    public static final int A_2_SLAM_DAMAGE = 38;
    private int tackleDmg;
    private int slamDmg;
    public static final int STICKY_TURNS = 3;
    private static final byte SLAM = 1;
    private static final byte PREP_SLAM = 2;
    private static final byte SPLIT = 3;
    private static final byte STICKY = 4;
    private static final String SLAM_NAME = MOVES[0];
    private static final String PREP_NAME = MOVES[1];
    private static final String SPLIT_NAME = MOVES[2];
    private static final String STICKY_NAME = MOVES[3];
    private boolean firstTurn = true;

    public SlimeBoss() {
        super(NAME, ID, 140, 0.0f, -30.0f, 400.0f, 350.0f, null, 0.0f, 28.0f);
        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = -150.0f * Settings.scale;
        this.dialogY = -70.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(150);
        } else {
            this.setHp(140);
        }
        if (AbstractDungeon.ascensionLevel >= 4) {
            this.tackleDmg = 10;
            this.slamDmg = 38;
        } else {
            this.tackleDmg = 9;
            this.slamDmg = 35;
        }
        this.damage.add(new DamageInfo(this, this.tackleDmg));
        this.damage.add(new DamageInfo(this, this.slamDmg));
        this.powers.add(new SplitPower(this));
        this.loadAnimation("images/monsters/theBottom/boss/slime/skeleton.atlas", "images/monsters/theBottom/boss/slime/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void usePreBattleAction() {
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
            CardCrawlGame.music.unsilenceBGM();
            AbstractDungeon.scene.fadeOutAmbiance();
            AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BOTTOM");
        }
        UnlockTracker.markBossAsSeen("SLIME");
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_SLIME_ATTACK"));
                if (AbstractDungeon.ascensionLevel >= 19) {
                    AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Slimed(), 5));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Slimed(), 3));
                }
                this.setMove(PREP_NAME, (byte)2, AbstractMonster.Intent.UNKNOWN);
                break;
            }
            case 2: {
                this.playSfx();
                AbstractDungeon.actionManager.addToBottom(new ShoutAction(this, DIALOG[0], 1.0f, 2.0f));
                AbstractDungeon.actionManager.addToBottom(new ShakeScreenAction(0.3f, ScreenShake.ShakeDur.LONG, ScreenShake.ShakeIntensity.LOW));
                this.setMove(SLAM_NAME, (byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
                break;
            }
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateJumpAction(this));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new WeightyImpactEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, new Color(0.1f, 1.0f, 0.1f, 0.0f))));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.8f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.POISON));
                this.setMove(STICKY_NAME, (byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new CannotLoseAction());
                AbstractDungeon.actionManager.addToBottom(new AnimateShakeAction(this, 1.0f, 0.1f));
                AbstractDungeon.actionManager.addToBottom(new HideHealthBarAction(this));
                AbstractDungeon.actionManager.addToBottom(new SuicideAction(this, false));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(1.0f));
                AbstractDungeon.actionManager.addToBottom(new SFXAction("SLIME_SPLIT"));
                AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(new SpikeSlime_L(-385.0f, 20.0f, 0, this.currentHealth), false));
                AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(new AcidSlime_L(120.0f, -8.0f, 0, this.currentHealth), false));
                AbstractDungeon.actionManager.addToBottom(new CanLoseAction());
                this.setMove(SPLIT_NAME, (byte)3, AbstractMonster.Intent.UNKNOWN);
            }
        }
    }

    private void playSfx() {
        int roll = MathUtils.random(1);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_SLIMEBOSS_1A"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_SLIMEBOSS_1B"));
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (!this.isDying && (float)this.currentHealth <= (float)this.maxHealth / 2.0f && this.nextMove != 3) {
            logger.info("SPLIT");
            this.setMove(SPLIT_NAME, (byte)3, AbstractMonster.Intent.UNKNOWN);
            this.createIntent();
            AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction((AbstractCreature)this, TextAboveCreatureAction.TextType.INTERRUPTED));
            AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, SPLIT_NAME, 3, AbstractMonster.Intent.UNKNOWN));
        }
    }

    @Override
    protected void getMove(int num) {
        if (this.firstTurn) {
            this.firstTurn = false;
            this.setMove(STICKY_NAME, (byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
            return;
        }
    }

    @Override
    public void die() {
        super.die();
        CardCrawlGame.sound.play("VO_SLIMEBOSS_2A");
        for (AbstractGameAction a : AbstractDungeon.actionManager.actions) {
            if (!(a instanceof SpawnMonsterAction)) continue;
            return;
        }
        if (this.currentHealth <= 0) {
            this.useFastShakeAnimation(5.0f);
            CardCrawlGame.screenShake.rumble(4.0f);
            this.onBossVictoryLogic();
            UnlockTracker.hardUnlockOverride("SLIME");
            UnlockTracker.unlockAchievement("SLIME_BOSS");
        }
    }
}

