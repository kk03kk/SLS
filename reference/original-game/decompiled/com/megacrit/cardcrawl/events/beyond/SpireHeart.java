/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.beyond;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AnimatedNpc;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.HeartAnimListener;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.GameOverScreen;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.DamageHeartEffect;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpireHeart
extends AbstractEvent {
    private static final Logger logger = LogManager.getLogger(SpireHeart.class.getName());
    public static final String ID = "Spire Heart";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Spire Heart");
    public static final String NAME = SpireHeart.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = SpireHeart.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = SpireHeart.eventStrings.OPTIONS;
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;
    private AnimatedNpc npc = null;
    private float x = 1300.0f * Settings.xScale;
    private float y = (float)Settings.HEIGHT / 2.0f + 40.0f * Settings.scale;
    private boolean startHeartAnimation = false;
    private float eventFadeTimer = 3.0f;
    private Color fadeColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private long globalDamageDealt;
    private long totalDamageDealt;
    private int damageDealt;
    private int winstreak;
    private static final String HEART_DMG_KEY = "test_stat";

    public SpireHeart() {
        boolean skipUpload;
        this.npc = new AnimatedNpc(1300.0f * Settings.xScale, AbstractDungeon.floorY - 80.0f * Settings.scale, "images/npcs/heart/skeleton.atlas", "images/npcs/heart/skeleton.json", "idle");
        this.npc.setTimeScale(1.5f);
        this.npc.addListener(new HeartAnimListener());
        this.body = DESCRIPTIONS[0];
        this.roomEventText.clear();
        this.roomEventText.addDialogOption(OPTIONS[0]);
        this.hasDialog = true;
        this.hasFocus = true;
        this.globalDamageDealt = CardCrawlGame.publisherIntegration.getGlobalStat(HEART_DMG_KEY);
        GameOverScreen.resetScoreChecks();
        this.damageDealt = GameOverScreen.calcScore(true);
        CardCrawlGame.publisherIntegration.incrementStat(HEART_DMG_KEY, this.damageDealt);
        String winStreakStatId = Settings.isBeta ? AbstractDungeon.player.getWinStreakKey() + "_BETA" : AbstractDungeon.player.getWinStreakKey();
        CardCrawlGame.publisherIntegration.incrementStat(winStreakStatId, 1);
        logger.info("WIN STREAK  " + CardCrawlGame.publisherIntegration.getStat(winStreakStatId));
        this.totalDamageDealt = CardCrawlGame.publisherIntegration.getStat(HEART_DMG_KEY);
        boolean bl = skipUpload = Settings.isModded || !Settings.isStandardRun();
        if (!skipUpload) {
            this.winstreak = CardCrawlGame.publisherIntegration.getStat(winStreakStatId);
            String leaderboardWinStreakStatId = Settings.isBeta ? AbstractDungeon.player.getLeaderboardWinStreakKey() + "_BETA" : AbstractDungeon.player.getLeaderboardWinStreakKey();
            CardCrawlGame.publisherIntegration.uploadLeaderboardScore(leaderboardWinStreakStatId, this.winstreak);
        }
        CardCrawlGame.playerPref.putInteger("DMG_DEALT", this.damageDealt + CardCrawlGame.playerPref.getInteger("DMG_DEALT", 0));
        if (this.totalDamageDealt <= 0L) {
            this.totalDamageDealt = CardCrawlGame.playerPref.getInteger("DMG_DEALT", 0);
        }
    }

    private void goToFinalAct() {
        this.fadeColor.a = 0.0f;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.DOOR_UNLOCK;
        CardCrawlGame.mainMenuScreen.doorUnlockScreen.open(true);
    }

    @Override
    public void update() {
        super.update();
        if (!RoomEventDialog.waitForInput) {
            this.buttonEffect(this.roomEventText.getSelectedOption());
        }
        if (this.startHeartAnimation && this.hasFocus) {
            this.eventFadeTimer -= Gdx.graphics.getDeltaTime();
            if (this.eventFadeTimer < 0.0f) {
                this.eventFadeTimer = 0.0f;
            }
            this.npc.skeleton.setY(Interpolation.pow2Out.apply(Settings.HEIGHT, AbstractDungeon.floorY - 80.0f * Settings.scale, this.eventFadeTimer / 3.0f));
            this.fadeColor.a = MathHelper.slowColorLerpSnap(this.fadeColor.a, 1.0f);
            this.npc.setTimeScale(Interpolation.fade.apply(0.8f, 1.5f, this.eventFadeTimer / 3.0f));
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                this.screen = CUR_SCREEN.MIDDLE;
                this.roomEventText.updateBodyText(AbstractDungeon.player.getSpireHeartText());
                this.roomEventText.updateDialogOption(0, OPTIONS[1]);
                break;
            }
            case MIDDLE: {
                this.screen = CUR_SCREEN.MIDDLE_2;
                this.roomEventText.updateBodyText(DESCRIPTIONS[1] + this.damageDealt + DESCRIPTIONS[2]);
                this.roomEventText.updateDialogOption(0, OPTIONS[0]);
                Color color = AbstractDungeon.player.getSlashAttackColor();
                AbstractGameAction.AttackEffect[] effects = AbstractDungeon.player.getSpireHeartSlashEffect();
                int HITS = effects.length;
                int damagePerTick = this.damageDealt / HITS;
                int remainder = this.damageDealt % HITS;
                int[] damages = new int[HITS];
                for (int i = 0; i < HITS; ++i) {
                    damages[i] = damagePerTick;
                    if (remainder <= 0) continue;
                    int n = i;
                    damages[n] = damages[n] + 1;
                    --remainder;
                }
                float tmp = 0.0f;
                AbstractDungeon.effectList.add(new BorderFlashEffect(color, true));
                for (int i = 0; i < HITS; ++i) {
                    AbstractDungeon.effectList.add(new DamageHeartEffect(tmp += MathUtils.random(0.05f, 0.2f), this.x, this.y, effects[i], damages[i]));
                }
                break;
            }
            case MIDDLE_2: {
                if (Settings.isFinalActAvailable && Settings.hasRubyKey && Settings.hasEmeraldKey && Settings.hasSapphireKey) {
                    this.startHeartAnimation = true;
                    CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.XLONG, false);
                    CardCrawlGame.screenShake.rumble(5.0f);
                    this.screen = CUR_SCREEN.GO_TO_ENDING;
                    this.roomEventText.updateBodyText(DESCRIPTIONS[11] + DESCRIPTIONS[12] + DESCRIPTIONS[13] + DESCRIPTIONS[14]);
                    this.roomEventText.updateDialogOption(0, OPTIONS[3]);
                    break;
                }
                this.screen = CUR_SCREEN.DEATH;
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                if (this.globalDamageDealt <= 0L) {
                    this.roomEventText.updateBodyText(DESCRIPTIONS[3] + numberFormat.format(this.totalDamageDealt) + DESCRIPTIONS[4] + DESCRIPTIONS[7]);
                } else {
                    this.roomEventText.updateBodyText(DESCRIPTIONS[3] + numberFormat.format(this.totalDamageDealt) + DESCRIPTIONS[4] + DESCRIPTIONS[5] + numberFormat.format(this.globalDamageDealt) + DESCRIPTIONS[6] + DESCRIPTIONS[7]);
                }
                this.roomEventText.updateDialogOption(0, OPTIONS[2]);
                break;
            }
            case DEATH: {
                AbstractDungeon.player.isDying = true;
                this.hasFocus = false;
                this.roomEventText.hide();
                AbstractDungeon.player.isDead = true;
                AbstractDungeon.deathScreen = new DeathScreen(null);
                break;
            }
            case GO_TO_ENDING: {
                this.roomEventText.clear();
                this.hasFocus = false;
                this.roomEventText.hide();
                this.goToFinalAct();
                break;
            }
            default: {
                logger.info("WHY YOU CALLED?");
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.fadeColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        if (this.npc != null) {
            this.npc.render(sb);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.npc != null) {
            this.npc.dispose();
            this.npc = null;
        }
    }

    private static enum CUR_SCREEN {
        INTRO,
        MIDDLE,
        MIDDLE_2,
        DEATH,
        GO_TO_ENDING;

    }
}

