/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.UpgradeShineParticleEffect;

public class BattleStartEffect
extends AbstractGameEffect {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("BattleStartEffect");
    public static final String[] TEXT = BattleStartEffect.uiStrings.TEXT;
    private static final float EFFECT_DUR = 4.0f;
    private static final float HEIGHT_DIV_2 = (float)Settings.HEIGHT / 2.0f;
    private static final float WIDTH_DIV_2 = (float)Settings.WIDTH / 2.0f;
    private boolean surpriseAttack;
    private boolean soundPlayed = false;
    private boolean bossFight = false;
    private Color bgColor;
    private static final float TARGET_HEIGHT = 150.0f * Settings.scale;
    private static final float BG_RECT_EXPAND_SPEED = 3.0f;
    private float currentHeight = 0.0f;
    private String battleStartMessage;
    private static final String BATTLE_START_MSG = TEXT[0];
    public static final String PLAYER_TURN_MSG = TEXT[1];
    public static final String ENEMY_TURN_MSG = TEXT[2];
    public static final String TURN_TXT = TEXT[3];
    private String turnMsg;
    private static final float TEXT_FADE_SPEED = 5.0f;
    private static final float MAIN_MSG_OFFSET_Y = 20.0f * Settings.scale;
    private static final float TURN_MSG_OFFSET_Y = -30.0f * Settings.scale;
    private Color turnMessageColor = new Color(0.7f, 0.7f, 0.7f, 0.0f);
    private float timer1 = 1.0f;
    private float timer2 = 1.0f;
    private static final float MSG_VANISH_X = (float)(-Settings.WIDTH) * 0.25f;
    private float firstMessageX = (float)Settings.WIDTH / 2.0f;
    private float secondMessageX = (float)Settings.WIDTH * 1.5f;
    private boolean showHb = false;
    private static TextureAtlas.AtlasRegion img = null;
    private static final float SWORD_ANIM_TIME = 0.5f;
    private float swordTimer = 0.5f;
    private static final float SWORD_START_X = -50.0f * Settings.scale;
    private static final float SWORD_DEST_X = (float)Settings.WIDTH / 2.0f + 0.0f * Settings.scale;
    private float swordX;
    private float swordY;
    private float swordAngle;
    private boolean swordSound1 = false;
    private Color swordColor = new Color(0.9f, 0.9f, 0.85f, 0.0f);

    public BattleStartEffect(boolean surpriseAttack) {
        this.duration = 4.0f;
        this.startingDuration = 4.0f;
        this.surpriseAttack = surpriseAttack;
        this.bgColor = new Color(AbstractDungeon.fadeColor.r / 2.0f, AbstractDungeon.fadeColor.g / 2.0f, AbstractDungeon.fadeColor.b / 2.0f, 0.0f);
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/battleStartSword");
        }
        this.scale = Settings.scale;
        this.swordY = (float)Settings.HEIGHT / 2.0f - (float)BattleStartEffect.img.packedHeight / 2.0f + 20.0f * Settings.scale;
        this.turnMsg = surpriseAttack ? ENEMY_TURN_MSG : PLAYER_TURN_MSG;
        this.color = Settings.GOLD_COLOR.cpy();
        this.color.a = 0.0f;
        this.battleStartMessage = Settings.usesOrdinal ? Integer.toString(GameActionManager.turn) + BattleStartEffect.getOrdinalNaming(GameActionManager.turn) + TURN_TXT : (Settings.language == Settings.GameLanguage.VIE ? TURN_TXT + " " + Integer.toString(GameActionManager.turn) : Integer.toString(GameActionManager.turn) + TURN_TXT);
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
            this.bossFight = true;
            CardCrawlGame.sound.play("BATTLE_START_BOSS");
        } else if (MathUtils.randomBoolean()) {
            CardCrawlGame.sound.play("BATTLE_START_1");
        } else {
            CardCrawlGame.sound.play("BATTLE_START_2");
        }
    }

    public static String getOrdinalNaming(int i) {
        return i % 100 == 11 || i % 100 == 12 || i % 100 == 13 ? "th" : (new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"})[i % 10];
    }

    @Override
    public void update() {
        if (!this.showHb) {
            AbstractDungeon.player.showHealthBar();
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                m.showHealthBar();
            }
            this.showHb = true;
        }
        if (this.duration > 3.0f) {
            this.currentHeight = MathUtils.lerp(this.currentHeight, TARGET_HEIGHT, Gdx.graphics.getDeltaTime() * 3.0f);
        } else if (this.duration < 0.5f) {
            this.currentHeight = MathUtils.lerp(this.currentHeight, 0.0f, Gdx.graphics.getDeltaTime() * 3.0f);
        }
        if (this.duration < 3.0f && this.timer1 != 0.0f) {
            this.timer1 -= Gdx.graphics.getDeltaTime();
            if (this.timer1 < 0.0f) {
                this.timer1 = 0.0f;
            }
            this.firstMessageX = Interpolation.pow2In.apply(this.firstMessageX, MSG_VANISH_X, 1.0f - this.timer1);
        } else if (this.duration < 3.0f && this.timer2 != 0.0f) {
            if (!this.soundPlayed) {
                CardCrawlGame.sound.play("TURN_EFFECT");
                AbstractDungeon.getMonsters().showIntent();
                this.soundPlayed = true;
            }
            this.timer2 -= Gdx.graphics.getDeltaTime();
            if (this.timer2 < 0.0f) {
                this.timer2 = 0.0f;
            }
            this.secondMessageX = Interpolation.pow2In.apply(this.secondMessageX, WIDTH_DIV_2, 1.0f - this.timer2);
        }
        this.color.a = this.duration > 1.0f ? MathUtils.lerp(this.color.a, 1.0f, Gdx.graphics.getDeltaTime() * 5.0f) : MathUtils.lerp(this.color.a, 0.0f, Gdx.graphics.getDeltaTime() * 5.0f);
        this.bgColor.a = this.color.a * 0.75f;
        this.turnMessageColor.a = this.color.a;
        if (Settings.FAST_MODE) {
            this.duration -= Gdx.graphics.getDeltaTime();
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.updateSwords();
    }

    private void updateSwords() {
        this.swordTimer -= Gdx.graphics.getDeltaTime();
        if (this.swordTimer < 0.0f) {
            this.swordTimer = 0.0f;
        }
        this.swordColor.a = Interpolation.fade.apply(1.0f, 0.01f, this.swordTimer / 0.5f);
        if (this.bossFight) {
            if (this.swordTimer < 0.1f && !this.swordSound1) {
                this.swordSound1 = true;
                CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
                for (int i = 0; i < 30; ++i) {
                    if (MathUtils.randomBoolean()) {
                        AbstractDungeon.effectsQueue.add(new UpgradeShineParticleEffect((float)Settings.WIDTH / 2.0f + MathUtils.random(-150.0f, 150.0f) * Settings.scale, (float)Settings.HEIGHT / 2.0f + MathUtils.random(-10.0f, 50.0f) * Settings.scale));
                        continue;
                    }
                    AbstractDungeon.topLevelEffectsQueue.add(new UpgradeShineParticleEffect((float)Settings.WIDTH / 2.0f + MathUtils.random(-150.0f, 150.0f) * Settings.scale, (float)Settings.HEIGHT / 2.0f + MathUtils.random(-10.0f, 50.0f) * Settings.scale));
                }
            }
            this.swordX = Interpolation.pow3Out.apply(SWORD_DEST_X, SWORD_START_X, this.swordTimer / 0.5f);
            this.swordAngle = Interpolation.pow3Out.apply(-50.0f, 500.0f, this.swordTimer / 0.5f);
        } else {
            this.swordX = SWORD_DEST_X;
            this.swordAngle = -50.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.bgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, HEIGHT_DIV_2 - this.currentHeight / 2.0f, (float)Settings.WIDTH, this.currentHeight);
        this.renderSwords(sb);
        FontHelper.renderFontCentered(sb, FontHelper.bannerNameFont, BATTLE_START_MSG, this.firstMessageX, HEIGHT_DIV_2 + MAIN_MSG_OFFSET_Y, this.color, 1.0f);
        FontHelper.renderFontCentered(sb, FontHelper.bannerNameFont, this.turnMsg, this.secondMessageX, HEIGHT_DIV_2 + MAIN_MSG_OFFSET_Y, this.color, 1.0f);
        if (!this.surpriseAttack) {
            FontHelper.renderFontCentered(sb, FontHelper.turnNumFont, this.battleStartMessage, this.secondMessageX, HEIGHT_DIV_2 + TURN_MSG_OFFSET_Y, this.turnMessageColor);
        }
    }

    @Override
    public void dispose() {
    }

    private void renderSwords(SpriteBatch sb) {
        sb.setColor(this.swordColor);
        sb.draw(img, (float)Settings.WIDTH - this.swordX - (float)BattleStartEffect.img.packedWidth / 2.0f + this.firstMessageX - (float)Settings.WIDTH / 2.0f, this.swordY, (float)BattleStartEffect.img.packedWidth / 2.0f, (float)BattleStartEffect.img.packedHeight / 2.0f, BattleStartEffect.img.packedWidth, BattleStartEffect.img.packedHeight, -this.scale, -this.scale, -this.swordAngle + 180.0f);
        sb.draw(img, this.swordX - (float)BattleStartEffect.img.packedWidth / 2.0f + this.firstMessageX - (float)Settings.WIDTH / 2.0f, this.swordY, (float)BattleStartEffect.img.packedWidth / 2.0f, (float)BattleStartEffect.img.packedHeight / 2.0f, BattleStartEffect.img.packedWidth, BattleStartEffect.img.packedHeight, this.scale, this.scale, this.swordAngle);
    }
}

