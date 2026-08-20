/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.GameDataStringBuilder;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.vfx.FloatyEffect;
import com.megacrit.cardcrawl.vfx.GlowRelicParticle;
import com.megacrit.cardcrawl.vfx.SmokePuffEffect;
import java.util.ArrayList;
import java.util.Scanner;

public class AbstractBlight {
    public String name;
    public String description;
    public String blightID;
    public Texture img;
    public Texture outlineImg;
    public boolean unique;
    public int increment;
    private static final String IMG_DIR = "images/blights/";
    private static final String OUTLINE_DIR = "images/blights/outline/";
    public float floatModAmount;
    public boolean isDone = false;
    public boolean isAnimating = false;
    public boolean isObtained = false;
    public int cost;
    public int counter = -1;
    public ArrayList<PowerTip> tips = new ArrayList();
    public String imgUrl = "";
    public static final int RAW_W = 128;
    public float currentX;
    public float currentY;
    public float targetX;
    public float targetY;
    private static final float START_X = 64.0f * Settings.xScale;
    private static final float START_Y = Settings.isMobile ? (float)Settings.HEIGHT - 206.0f * Settings.scale : (float)Settings.HEIGHT - 176.0f * Settings.scale;
    private static final Color PASSIVE_OUTLINE_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.33f);
    private Color flashColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private Color goldOutlineColor = new Color(1.0f, 0.9f, 0.4f, 0.0f);
    public boolean isSeen = false;
    public float scale = Settings.scale;
    public static final int MAX_BLIGHTS_PER_PAGE = 25;
    public Hitbox hb = new Hitbox(AbstractRelic.PAD_X, AbstractRelic.PAD_X);
    private static final float OBTAIN_SPEED = 6.0f;
    private static final float OBTAIN_THRESHOLD = 0.5f;
    private float rotation = 0.0f;
    public boolean discarded = false;
    protected boolean pulse = false;
    private float animationTimer = 0.0f;
    public float flashTimer = 0.0f;
    private static final float FLASH_ANIM_TIME = 2.0f;
    private static final float DEFAULT_ANIM_SCALE = 4.0f;
    private float glowTimer = 0.0f;
    private FloatyEffect f_effect = new FloatyEffect(10.0f, 0.2f);
    private static float offsetX = 0.0f;

    public AbstractBlight(String setId, String name, String description, String imgName, boolean unique) {
        this.blightID = setId;
        this.name = name;
        this.description = description;
        this.unique = unique;
        this.img = ImageMaster.loadImage(IMG_DIR + imgName);
        this.outlineImg = ImageMaster.loadImage(OUTLINE_DIR + imgName);
        this.increment = 0;
        this.tips.add(new PowerTip(name, description));
    }

    public void spawn(float x, float y) {
        AbstractDungeon.effectsQueue.add(new SmokePuffEffect(x, y));
        this.currentX = x;
        this.currentY = y;
        this.isAnimating = true;
        this.isObtained = false;
        this.f_effect.x = 0.0f;
        this.f_effect.y = 0.0f;
        this.hb = new Hitbox(AbstractRelic.PAD_X, AbstractRelic.PAD_X);
    }

    public void renderInTopPanel(SpriteBatch sb) {
        if (Settings.hideRelics) {
            return;
        }
        this.renderOutline(sb, true);
        sb.setColor(Color.WHITE);
        sb.draw(this.img, this.currentX - 64.0f + offsetX, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        this.renderCounter(sb, true);
        this.renderFlash(sb, true);
        this.hb.render(sb);
    }

    public void render(SpriteBatch sb) {
        if (Settings.hideRelics) {
            return;
        }
        if (this.isDone) {
            this.renderOutline(sb, false);
        }
        if (!(this.isObtained || AbstractDungeon.isScreenUp && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.BOSS_REWARD && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.SHOP)) {
            if (this.hb.hovered) {
                this.renderTip(sb);
            }
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                if (this.hb.hovered) {
                    sb.setColor(PASSIVE_OUTLINE_COLOR);
                    sb.draw(this.outlineImg, this.currentX - 64.0f + this.f_effect.x, this.currentY - 64.0f + this.f_effect.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
                } else {
                    sb.setColor(PASSIVE_OUTLINE_COLOR);
                    sb.draw(this.outlineImg, this.currentX - 64.0f + this.f_effect.x, this.currentY - 64.0f + this.f_effect.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
                }
            }
        }
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
            if (!this.isObtained) {
                sb.setColor(Color.WHITE);
                sb.draw(this.img, this.currentX - 64.0f + this.f_effect.x, this.currentY - 64.0f + this.f_effect.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            } else {
                sb.setColor(Color.WHITE);
                sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
                this.renderCounter(sb, false);
            }
        } else {
            sb.setColor(Color.WHITE);
            sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            this.renderCounter(sb, false);
        }
        if (this.isDone) {
            this.renderFlash(sb, false);
        }
        this.hb.render(sb);
    }

    protected void updateAnimation() {
        if (this.animationTimer != 0.0f) {
            this.animationTimer -= Gdx.graphics.getDeltaTime();
            if (this.animationTimer < 0.0f) {
                this.animationTimer = 0.0f;
            }
        }
    }

    public void render(SpriteBatch sb, boolean renderAmount, Color outlineColor) {
        if (this.isSeen) {
            this.renderOutline(outlineColor, sb, false);
        } else {
            this.renderOutline(Color.LIGHT_GRAY, sb, false);
        }
        if (this.isSeen) {
            sb.setColor(Color.WHITE);
        } else if (this.hb.hovered) {
            sb.setColor(Settings.HALF_TRANSPARENT_BLACK_COLOR);
        } else {
            sb.setColor(Color.BLACK);
        }
        sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        if (this.hb.hovered) {
            this.renderTip(sb);
        }
        this.hb.render(sb);
    }

    public void renderCounter(SpriteBatch sb, boolean inTopPanel) {
        if (this.counter > -1) {
            FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(this.counter), this.currentX + 30.0f * Settings.scale, this.currentY - 7.0f * Settings.scale, Color.WHITE);
        }
    }

    public void renderOutline(Color c, SpriteBatch sb, boolean inTopPanel) {
        sb.setColor(c);
        if (AbstractDungeon.screen != null && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NEOW_UNLOCK) {
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 15.0f, Settings.scale * 2.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 15.0f, this.rotation, 0, 0, 128, 128, false, false);
        } else if (this.hb.hovered && Settings.isControllerMode) {
            sb.setBlendFunction(770, 1);
            this.goldOutlineColor.a = 0.6f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L) / 5.0f;
            sb.setColor(this.goldOutlineColor);
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            sb.setBlendFunction(770, 771);
        } else {
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        }
    }

    public void renderOutline(SpriteBatch sb, boolean inTopPanel) {
        if (this.hb.hovered && Settings.isControllerMode) {
            sb.setBlendFunction(770, 1);
            this.goldOutlineColor.a = 0.6f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L) / 5.0f;
            sb.setColor(this.goldOutlineColor);
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            sb.setBlendFunction(770, 771);
        } else {
            sb.setColor(PASSIVE_OUTLINE_COLOR);
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        }
    }

    public void renderFlash(SpriteBatch sb, boolean inTopPanel) {
        float tmp = Interpolation.exp10In.apply(0.0f, 4.0f, this.flashTimer / 2.0f);
        sb.setBlendFunction(770, 1);
        this.flashColor.a = this.flashTimer * 0.2f;
        sb.setColor(this.flashColor);
        sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale + tmp, this.scale + tmp, this.rotation, 0, 0, 128, 128, false, false);
        sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale + tmp * 0.66f, this.scale + tmp * 0.66f, this.rotation, 0, 0, 128, 128, false, false);
        sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale + tmp / 3.0f, this.scale + tmp / 3.0f, this.rotation, 0, 0, 128, 128, false, false);
        sb.setBlendFunction(770, 771);
    }

    public void flash() {
        this.flashTimer = 2.0f;
    }

    public void renderTip(SpriteBatch sb) {
        if ((float)InputHelper.mX < 1400.0f * Settings.scale) {
            if (CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.RELIC_VIEW) {
                TipHelper.queuePowerTips(180.0f * Settings.scale, (float)Settings.HEIGHT * 0.7f, this.tips);
            } else {
                TipHelper.queuePowerTips((float)InputHelper.mX + 50.0f * Settings.scale, (float)InputHelper.mY + 50.0f * Settings.scale, this.tips);
            }
        } else {
            TipHelper.queuePowerTips((float)InputHelper.mX - 350.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, this.tips);
        }
    }

    protected void initializeTips() {
        Scanner desc = new Scanner(this.description);
        while (desc.hasNext()) {
            String s = desc.next();
            if (s.charAt(0) == '#') {
                s = s.substring(2);
            }
            s = s.replace(',', ' ');
            s = s.replace('.', ' ');
            s = s.trim();
            s = s.toLowerCase();
            boolean alreadyExists = false;
            if (!GameDictionary.keywords.containsKey(s)) continue;
            s = GameDictionary.parentWord.get(s);
            for (PowerTip t : this.tips) {
                if (!t.header.toLowerCase().equals(s)) continue;
                alreadyExists = true;
                break;
            }
            if (alreadyExists) continue;
            this.tips.add(new PowerTip(TipHelper.capitalize(s), GameDictionary.keywords.get(s)));
        }
        desc.close();
    }

    public void obtain() {
        this.hb.hovered = false;
        int slot = AbstractDungeon.player.blights.size();
        this.targetX = START_X + (float)slot * AbstractRelic.PAD_X;
        this.targetY = START_Y;
        AbstractDungeon.player.blights.add(this);
    }

    public void instantObtain(AbstractPlayer p, int slot, boolean callOnEquip) {
        this.isDone = true;
        this.isObtained = true;
        if (slot >= p.blights.size()) {
            p.blights.add(this);
        } else {
            p.blights.set(slot, this);
        }
        this.currentX = START_X + (float)slot * AbstractRelic.PAD_X;
        this.currentY = START_Y;
        this.targetX = this.currentX;
        this.targetY = this.currentY;
        this.hb.move(this.currentX, this.currentY);
        if (callOnEquip) {
            this.onEquip();
        }
    }

    private void updateFlash() {
        if (this.flashTimer != 0.0f) {
            this.flashTimer -= Gdx.graphics.getDeltaTime();
            if (this.flashTimer < 0.0f) {
                this.flashTimer = this.pulse ? 1.0f : 0.0f;
            }
        }
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void update() {
        this.updateFlash();
        if (!this.isDone) {
            if (this.isAnimating) {
                this.glowTimer -= Gdx.graphics.getDeltaTime();
                if (this.glowTimer < 0.0f) {
                    this.glowTimer = 0.5f;
                    AbstractDungeon.effectList.add(new GlowRelicParticle(this.img, this.currentX + this.f_effect.x, this.currentY + this.f_effect.y, this.rotation));
                }
                this.f_effect.update();
                this.scale = this.hb.hovered ? Settings.scale * 1.5f : MathHelper.scaleLerpSnap(this.scale, Settings.scale * 1.1f);
            } else {
                this.scale = this.hb.hovered ? Settings.scale * 1.25f : MathHelper.scaleLerpSnap(this.scale, Settings.scale);
            }
            if (this.isObtained) {
                if (this.rotation != 0.0f) {
                    this.rotation = MathUtils.lerp(this.rotation, 0.0f, Gdx.graphics.getDeltaTime() * 6.0f * 2.0f);
                }
                if (this.currentX != this.targetX) {
                    this.currentX = MathUtils.lerp(this.currentX, this.targetX, Gdx.graphics.getDeltaTime() * 6.0f);
                    if (Math.abs(this.currentX - this.targetX) < 0.5f) {
                        this.currentX = this.targetX;
                    }
                }
                if (this.currentY != this.targetY) {
                    this.currentY = MathUtils.lerp(this.currentY, this.targetY, Gdx.graphics.getDeltaTime() * 6.0f);
                    if (Math.abs(this.currentY - this.targetY) < 0.5f) {
                        this.currentY = this.targetY;
                    }
                }
                if (this.currentY == this.targetY && this.currentX == this.targetX) {
                    this.isDone = true;
                    this.onEquip();
                    this.hb.move(this.currentX, this.currentY);
                }
                this.scale = Settings.scale;
            }
            if (this.hb != null) {
                this.hb.update();
                if (this.hb.hovered && (!AbstractDungeon.isScreenUp || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NEOW_UNLOCK) {
                    if (InputHelper.justClickedLeft && !this.isObtained) {
                        InputHelper.justClickedLeft = false;
                        this.hb.clickStarted = true;
                    }
                    if ((this.hb.clicked || CInputActionSet.select.isJustPressed()) && !this.isObtained) {
                        CInputActionSet.select.unpress();
                        this.hb.clicked = false;
                        if (!Settings.isTouchScreen) {
                            this.bossObtainLogic();
                        } else {
                            AbstractDungeon.bossRelicScreen.confirmButton.show();
                            AbstractDungeon.bossRelicScreen.confirmButton.isDisabled = false;
                            AbstractDungeon.bossRelicScreen.touchBlight = this;
                        }
                    }
                }
            }
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                this.updateAnimation();
            }
        } else {
            this.hb.update();
            this.scale = this.hb.hovered && AbstractDungeon.topPanel.potionUi.isHidden ? Settings.scale * 1.25f : MathHelper.scaleLerpSnap(this.scale, Settings.scale);
        }
    }

    public void bossObtainLogic() {
        if (AbstractDungeon.player.hasBlight(this.blightID)) {
            AbstractDungeon.player.getBlight(this.blightID).stack();
            this.isObtained = true;
        } else {
            this.obtain();
            InputHelper.justClickedLeft = false;
            this.isObtained = true;
            this.f_effect.x = 0.0f;
            this.f_effect.y = 0.0f;
        }
    }

    public void onPlayCard(AbstractCard card, AbstractMonster m) {
    }

    public boolean canPlay(AbstractCard card) {
        return true;
    }

    public void onVictory() {
    }

    public void atBattleStart() {
    }

    public void atTurnStart() {
    }

    public void onPlayerEndTurn() {
    }

    public void onBossDefeat() {
    }

    public void onCreateEnemy(AbstractMonster m) {
    }

    public void effect() {
    }

    public void onEquip() {
    }

    public float effectFloat() {
        return this.floatModAmount;
    }

    public void incrementUp() {
    }

    public void setIncrement(int newInc) {
        this.increment = newInc;
    }

    public static String gameDataUploadHeader() {
        GameDataStringBuilder sb = new GameDataStringBuilder();
        sb.addFieldData("name");
        sb.addFieldData("text");
        return sb.toString();
    }

    public String gameDataUploadData() {
        GameDataStringBuilder sb = new GameDataStringBuilder();
        sb.addFieldData(this.name);
        sb.addFieldData(this.description);
        return sb.toString();
    }

    public void stack() {
    }

    public void updateDescription(AbstractPlayer.PlayerClass c) {
    }

    public void updateDescription() {
    }
}

