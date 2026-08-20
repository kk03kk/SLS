/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.unique.BurnIncreaseAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.HexaghostBody;
import com.megacrit.cardcrawl.monsters.exordium.HexaghostOrb;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.FireballEffect;
import com.megacrit.cardcrawl.vfx.combat.GhostIgniteEffect;
import com.megacrit.cardcrawl.vfx.combat.InflameEffect;
import com.megacrit.cardcrawl.vfx.combat.ScreenOnFireEffect;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Hexaghost
extends AbstractMonster {
    private static final Logger logger = LogManager.getLogger(Hexaghost.class.getName());
    public static final String ID = "Hexaghost";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Hexaghost");
    public static final String NAME = Hexaghost.monsterStrings.NAME;
    public static final String[] MOVES = Hexaghost.monsterStrings.MOVES;
    public static final String[] DIALOG = Hexaghost.monsterStrings.DIALOG;
    public static final String IMAGE = "images/monsters/theBottom/boss/ghost/core.png";
    private static final int HP = 250;
    private static final int A_2_HP = 264;
    private ArrayList<HexaghostOrb> orbs = new ArrayList();
    private static final int SEAR_DMG = 6;
    private static final int INFERNO_DMG = 2;
    private static final int FIRE_TACKLE_DMG = 5;
    private static final int BURN_COUNT = 1;
    private static final int STR_AMT = 2;
    private static final int A_4_INFERNO_DMG = 3;
    private static final int A_4_FIRE_TACKLE_DMG = 6;
    private static final int A_19_BURN_COUNT = 2;
    private static final int A_19_STR_AMT = 3;
    private int searDmg;
    private int strengthenBlockAmt = 12;
    private int strAmount;
    private int searBurnCount;
    private int fireTackleDmg;
    private int fireTackleCount = 2;
    private int infernoDmg;
    private int infernoHits = 6;
    private static final byte DIVIDER = 1;
    private static final byte TACKLE = 2;
    private static final byte INFLAME = 3;
    private static final byte SEAR = 4;
    private static final byte ACTIVATE = 5;
    private static final byte INFERNO = 6;
    private static final String STRENGTHEN_NAME = MOVES[0];
    private static final String SEAR_NAME = MOVES[1];
    private static final String BURN_NAME = MOVES[2];
    private static final String ACTIVATE_STATE = "Activate";
    private static final String ACTIVATE_ORB = "Activate Orb";
    private static final String DEACTIVATE_ALL_ORBS = "Deactivate";
    private boolean activated = false;
    private boolean burnUpgraded = false;
    private int orbActiveCount = 0;
    private HexaghostBody body;

    public Hexaghost() {
        super(NAME, ID, 250, 20.0f, 0.0f, 450.0f, 450.0f, IMAGE);
        this.type = AbstractMonster.EnemyType.BOSS;
        this.body = new HexaghostBody(this);
        this.disposables.add(this.body);
        this.createOrbs();
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(264);
        } else {
            this.setHp(250);
        }
        if (AbstractDungeon.ascensionLevel >= 19) {
            this.strAmount = 3;
            this.searBurnCount = 2;
            this.fireTackleDmg = 6;
            this.infernoDmg = 3;
        } else if (AbstractDungeon.ascensionLevel >= 4) {
            this.strAmount = 2;
            this.searBurnCount = 1;
            this.fireTackleDmg = 6;
            this.infernoDmg = 3;
        } else {
            this.strAmount = 2;
            this.searBurnCount = 1;
            this.fireTackleDmg = 5;
            this.infernoDmg = 2;
        }
        this.searDmg = 6;
        this.damage.add(new DamageInfo(this, this.fireTackleDmg));
        this.damage.add(new DamageInfo(this, this.searDmg));
        this.damage.add(new DamageInfo(this, -1));
        this.damage.add(new DamageInfo(this, this.infernoDmg));
    }

    @Override
    public void usePreBattleAction() {
        UnlockTracker.markBossAsSeen("GHOST");
        CardCrawlGame.music.precacheTempBgm("BOSS_BOTTOM");
    }

    private void createOrbs() {
        this.orbs.add(new HexaghostOrb(-90.0f, 380.0f, this.orbs.size()));
        this.orbs.add(new HexaghostOrb(90.0f, 380.0f, this.orbs.size()));
        this.orbs.add(new HexaghostOrb(160.0f, 250.0f, this.orbs.size()));
        this.orbs.add(new HexaghostOrb(90.0f, 120.0f, this.orbs.size()));
        this.orbs.add(new HexaghostOrb(-90.0f, 120.0f, this.orbs.size()));
        this.orbs.add(new HexaghostOrb(-160.0f, 250.0f, this.orbs.size()));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 5: {
                int d;
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, ACTIVATE_STATE));
                ((DamageInfo)this.damage.get((int)2)).base = d = AbstractDungeon.player.currentHealth / 12 + 1;
                this.applyPowers();
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)2)).base, 6, true);
                break;
            }
            case 1: {
                for (int i = 0; i < 6; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new GhostIgniteEffect(AbstractDungeon.player.hb.cX + MathUtils.random(-120.0f, 120.0f) * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-120.0f, 120.0f) * Settings.scale), 0.05f));
                    if (MathUtils.randomBoolean()) {
                        AbstractDungeon.actionManager.addToBottom(new SFXAction("GHOST_ORB_IGNITE_1", 0.3f));
                    } else {
                        AbstractDungeon.actionManager.addToBottom(new SFXAction("GHOST_ORB_IGNITE_2", 0.3f));
                    }
                    AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player, (DamageInfo)this.damage.get(2), AbstractGameAction.AttackEffect.BLUNT_HEAVY, true));
                }
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, DEACTIVATE_ALL_ORBS));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BorderFlashEffect(Color.CHARTREUSE)));
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, ACTIVATE_ORB));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new FireballEffect(this.hb.cX, this.hb.cY, AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY), 0.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.FIRE));
                Burn c = new Burn();
                if (this.burnUpgraded) {
                    c.upgrade();
                }
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)c, this.searBurnCount));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, ACTIVATE_ORB));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new InflameEffect(this), 0.5f));
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, this.strengthenBlockAmt));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, this.strAmount), this.strAmount));
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, ACTIVATE_ORB));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            case 6: {
                AbstractDungeon.actionManager.addToBottom(new VFXAction(this, new ScreenOnFireEffect(), 1.0f));
                for (int i = 0; i < this.infernoHits; ++i) {
                    AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(3), AbstractGameAction.AttackEffect.FIRE));
                }
                AbstractDungeon.actionManager.addToBottom(new BurnIncreaseAction());
                if (!this.burnUpgraded) {
                    this.burnUpgraded = true;
                }
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, DEACTIVATE_ALL_ORBS));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            default: {
                logger.info("ERROR: Default Take Turn was called on " + this.name);
            }
        }
    }

    @Override
    protected void getMove(int num) {
        if (!this.activated) {
            this.activated = true;
            this.setMove((byte)5, AbstractMonster.Intent.UNKNOWN);
        } else {
            switch (this.orbActiveCount) {
                case 0: {
                    this.setMove(SEAR_NAME, (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
                    break;
                }
                case 1: {
                    this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, this.fireTackleCount, true);
                    break;
                }
                case 2: {
                    this.setMove(SEAR_NAME, (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
                    break;
                }
                case 3: {
                    this.setMove(STRENGTHEN_NAME, (byte)3, AbstractMonster.Intent.DEFEND_BUFF);
                    break;
                }
                case 4: {
                    this.setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, this.fireTackleCount, true);
                    break;
                }
                case 5: {
                    this.setMove(SEAR_NAME, (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)1)).base);
                    break;
                }
                case 6: {
                    this.setMove(BURN_NAME, (byte)6, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)3)).base, this.infernoHits, true);
                }
            }
        }
    }

    @Override
    public void changeState(String stateName) {
        switch (stateName) {
            case "Activate": {
                if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
                    CardCrawlGame.music.unsilenceBGM();
                    AbstractDungeon.scene.fadeOutAmbiance();
                    CardCrawlGame.music.playPrecachedTempBgm();
                }
                for (HexaghostOrb orb : this.orbs) {
                    orb.activate(this.drawX + this.animX, this.drawY + this.animY);
                }
                this.orbActiveCount = 6;
                this.body.targetRotationSpeed = 120.0f;
                break;
            }
            case "Activate Orb": {
                for (HexaghostOrb orb : this.orbs) {
                    if (orb.activated) continue;
                    orb.activate(this.drawX + this.animX, this.drawY + this.animY);
                    break;
                }
                ++this.orbActiveCount;
                if (this.orbActiveCount != 6) break;
                this.setMove(BURN_NAME, (byte)6, AbstractMonster.Intent.ATTACK_DEBUFF, ((DamageInfo)this.damage.get((int)3)).base, this.infernoHits, true);
                break;
            }
            case "Deactivate": {
                for (HexaghostOrb orb : this.orbs) {
                    orb.deactivate();
                }
                CardCrawlGame.sound.play("CARD_EXHAUST", 0.2f);
                CardCrawlGame.sound.play("CARD_EXHAUST", 0.2f);
                this.orbActiveCount = 0;
            }
        }
    }

    @Override
    public void die() {
        this.useFastShakeAnimation(5.0f);
        CardCrawlGame.screenShake.rumble(4.0f);
        super.die();
        for (HexaghostOrb orb : this.orbs) {
            orb.hide();
        }
        this.onBossVictoryLogic();
        UnlockTracker.hardUnlockOverride("GHOST");
        UnlockTracker.unlockAchievement("GHOST_GUARDIAN");
    }

    @Override
    public void update() {
        super.update();
        this.body.update();
        for (HexaghostOrb orb : this.orbs) {
            orb.update(this.drawX + this.animX, this.drawY + this.animY);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        this.body.render(sb);
        super.render(sb);
    }
}

