/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.CallingBell;
import com.megacrit.cardcrawl.relics.Cauldron;
import com.megacrit.cardcrawl.relics.CharonsAshes;
import com.megacrit.cardcrawl.relics.ChemicalX;
import com.megacrit.cardcrawl.relics.Damaru;
import com.megacrit.cardcrawl.relics.MembershipCard;
import com.megacrit.cardcrawl.relics.SingingBowl;
import com.megacrit.cardcrawl.relics.Tingsha;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;

public class SingleRelicViewPopup {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("SingleViewRelicPopup");
    public static final String[] TEXT = SingleRelicViewPopup.uiStrings.TEXT;
    public boolean isOpen = false;
    private ArrayList<AbstractRelic> group;
    private AbstractRelic relic;
    private AbstractRelic prevRelic;
    private AbstractRelic nextRelic;
    private static final int W = 128;
    private Texture relicFrameImg;
    private Texture largeImg;
    private float fadeTimer = 0.0f;
    private Color fadeColor = Color.BLACK.cpy();
    private Hitbox popupHb;
    private Hitbox prevHb;
    private Hitbox nextHb;
    private String rarityLabel = "";
    private static final String LARGE_IMG_DIR = "images/largeRelics/";
    private static final float DESC_LINE_SPACING = 30.0f * Settings.scale;
    private static final float DESC_LINE_WIDTH = 418.0f * Settings.scale;
    private final float RELIC_OFFSET_Y = 76.0f * Settings.scale;

    public void open(AbstractRelic relic, ArrayList<AbstractRelic> group) {
        CardCrawlGame.isPopupOpen = true;
        relic.playLandingSFX();
        this.prevRelic = null;
        this.nextRelic = null;
        this.prevHb = null;
        this.nextHb = null;
        for (int i = 0; i < group.size(); ++i) {
            if (group.get(i) != relic) continue;
            if (i != 0) {
                this.prevRelic = group.get(i - 1);
            }
            if (i == group.size() - 1) break;
            this.nextRelic = group.get(i + 1);
            break;
        }
        this.prevHb = new Hitbox(160.0f * Settings.scale, 160.0f * Settings.scale);
        this.nextHb = new Hitbox(160.0f * Settings.scale, 160.0f * Settings.scale);
        this.prevHb.move((float)Settings.WIDTH / 2.0f - 400.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f);
        this.nextHb.move((float)Settings.WIDTH / 2.0f + 400.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f);
        this.popupHb = new Hitbox(550.0f * Settings.scale, 680.0f * Settings.scale);
        this.popupHb.move((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f);
        this.isOpen = true;
        this.group = group;
        this.relic = relic;
        this.fadeTimer = 0.25f;
        this.fadeColor.a = 0.0f;
        this.generateRarityLabel();
        this.generateFrameImg();
        this.relic.isSeen = UnlockTracker.isRelicSeen(relic.relicId);
        this.initializeLargeImg();
    }

    public void open(AbstractRelic relic) {
        CardCrawlGame.isPopupOpen = true;
        relic.playLandingSFX();
        this.prevRelic = null;
        this.nextRelic = null;
        this.prevHb = null;
        this.nextHb = null;
        this.popupHb = new Hitbox(550.0f * Settings.scale, 680.0f * Settings.scale);
        this.popupHb.move((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f);
        this.isOpen = true;
        this.group = null;
        this.relic = relic;
        this.fadeTimer = 0.25f;
        this.fadeColor.a = 0.0f;
        this.generateRarityLabel();
        this.generateFrameImg();
        this.relic.isSeen = UnlockTracker.isRelicSeen(relic.relicId);
        this.initializeLargeImg();
    }

    private void initializeLargeImg() {
        this.largeImg = ImageMaster.loadImage(LARGE_IMG_DIR + this.relic.imgUrl);
    }

    public void close() {
        CardCrawlGame.isPopupOpen = false;
        this.isOpen = false;
        InputHelper.justReleasedClickLeft = false;
        if (this.largeImg != null) {
            this.largeImg.dispose();
            this.largeImg = null;
        }
        if (this.relicFrameImg != null) {
            this.relicFrameImg.dispose();
            this.relicFrameImg = null;
        }
    }

    public void update() {
        this.popupHb.update();
        this.updateArrows();
        this.updateInput();
        this.updateFade();
        this.updateSoundEffect();
    }

    private void updateArrows() {
        if (this.prevRelic != null) {
            this.prevHb.update();
            if (this.prevHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
            if (this.prevHb.clicked || this.prevRelic != null && CInputActionSet.pageLeftViewDeck.isJustPressed()) {
                this.openPrev();
            }
        }
        if (this.nextRelic != null) {
            this.nextHb.update();
            if (this.nextHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
            if (this.nextHb.clicked || this.nextRelic != null && CInputActionSet.pageRightViewExhaust.isJustPressed()) {
                this.openNext();
            }
        }
    }

    private void updateInput() {
        if (InputHelper.justClickedLeft) {
            if (this.prevRelic != null && this.prevHb.hovered) {
                this.prevHb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
                return;
            }
            if (this.nextRelic != null && this.nextHb.hovered) {
                this.nextHb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
                return;
            }
        }
        if (InputHelper.justReleasedClickLeft) {
            if (!this.popupHb.hovered) {
                this.close();
                FontHelper.ClearSRVFontTextures();
            }
        } else if (InputHelper.pressedEscape || CInputActionSet.cancel.isJustPressed()) {
            CInputActionSet.cancel.unpress();
            InputHelper.pressedEscape = false;
            if (AbstractDungeon.screen == null || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE) {
                AbstractDungeon.isScreenUp = false;
            }
            this.close();
            FontHelper.ClearSRVFontTextures();
        }
        if (this.prevRelic != null && InputActionSet.left.isJustPressed()) {
            this.openPrev();
        } else if (this.nextRelic != null && InputActionSet.right.isJustPressed()) {
            this.openNext();
        }
    }

    private void openPrev() {
        this.close();
        this.open(this.prevRelic, this.group);
        this.fadeTimer = 0.0f;
        this.fadeColor.a = 0.9f;
    }

    private void openNext() {
        this.close();
        this.open(this.nextRelic, this.group);
        this.fadeTimer = 0.0f;
        this.fadeColor.a = 0.9f;
    }

    private void updateFade() {
        this.fadeTimer -= Gdx.graphics.getDeltaTime();
        if (this.fadeTimer < 0.0f) {
            this.fadeTimer = 0.0f;
        }
        this.fadeColor.a = Interpolation.pow2In.apply(0.9f, 0.0f, this.fadeTimer * 4.0f);
    }

    private void updateSoundEffect() {
        String key = null;
        if (this.relic instanceof Tingsha) {
            key = "TINGSHA";
        } else if (this.relic instanceof Damaru) {
            key = "DAMARU";
        } else if (this.relic instanceof SingingBowl) {
            key = "SINGING_BOWL";
        } else if (this.relic instanceof CallingBell) {
            key = "BELL";
        } else if (this.relic instanceof ChemicalX) {
            key = "POTION_3";
        } else if (this.relic instanceof Cauldron) {
            key = "POTION_1";
        } else if (this.relic instanceof MembershipCard) {
            key = "SHOP_PURCHASE";
        } else if (this.relic instanceof CharonsAshes) {
            key = "CARD_BURN";
        }
        if (InputActionSet.selectCard_1.isJustPressed()) {
            CardCrawlGame.sound.playA(key, -0.2f);
        } else if (InputActionSet.selectCard_2.isJustPressed()) {
            CardCrawlGame.sound.playA(key, -0.15f);
        } else if (InputActionSet.selectCard_3.isJustPressed()) {
            CardCrawlGame.sound.playA(key, -0.1f);
        } else if (InputActionSet.selectCard_4.isJustPressed()) {
            CardCrawlGame.sound.playA(key, -0.05f);
        } else if (InputActionSet.selectCard_5.isJustPressed()) {
            CardCrawlGame.sound.playA(key, 0.0f);
        } else if (InputActionSet.selectCard_6.isJustPressed()) {
            CardCrawlGame.sound.playA(key, 0.05f);
        } else if (InputActionSet.selectCard_7.isJustPressed()) {
            CardCrawlGame.sound.playA(key, 0.1f);
        } else if (InputActionSet.selectCard_8.isJustPressed()) {
            CardCrawlGame.sound.playA(key, 0.15f);
        } else if (InputActionSet.selectCard_9.isJustPressed()) {
            CardCrawlGame.sound.playA(key, 0.2f);
        } else if (InputActionSet.selectCard_10.isJustPressed()) {
            CardCrawlGame.sound.playA(key, 0.25f);
        }
    }

    private void generateRarityLabel() {
        switch (this.relic.tier) {
            case BOSS: {
                this.rarityLabel = TEXT[0];
                break;
            }
            case COMMON: {
                this.rarityLabel = TEXT[1];
                break;
            }
            case DEPRECATED: {
                this.rarityLabel = TEXT[2];
                break;
            }
            case RARE: {
                this.rarityLabel = TEXT[3];
                break;
            }
            case SHOP: {
                this.rarityLabel = TEXT[4];
                break;
            }
            case SPECIAL: {
                this.rarityLabel = TEXT[5];
                break;
            }
            case STARTER: {
                this.rarityLabel = TEXT[6];
                break;
            }
            case UNCOMMON: {
                this.rarityLabel = TEXT[7];
                break;
            }
        }
    }

    private void generateFrameImg() {
        if (!this.relic.isSeen) {
            this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameCommon.png");
            return;
        }
        switch (this.relic.tier) {
            case BOSS: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameBoss.png");
                break;
            }
            case COMMON: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameCommon.png");
                break;
            }
            case DEPRECATED: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameCommon.png");
                break;
            }
            case RARE: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameRare.png");
                break;
            }
            case SHOP: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameRare.png");
                break;
            }
            case SPECIAL: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameRare.png");
                break;
            }
            case STARTER: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameCommon.png");
                break;
            }
            case UNCOMMON: {
                this.relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameUncommon.png");
                break;
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.fadeColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        this.renderPopupBg(sb);
        this.renderFrame(sb);
        this.renderArrows(sb);
        this.renderRelicImage(sb);
        this.renderName(sb);
        this.renderRarity(sb);
        this.renderDescription(sb);
        this.renderQuote(sb);
        this.renderTips(sb);
        this.popupHb.render(sb);
        if (this.prevHb != null) {
            this.prevHb.render(sb);
        }
        if (this.nextHb != null) {
            this.nextHb.render(sb);
        }
    }

    private void renderPopupBg(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.RELIC_POPUP, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 540.0f, 960.0f, 540.0f, 1920.0f, 1080.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1080, false, false);
    }

    private void renderFrame(SpriteBatch sb) {
        sb.draw(this.relicFrameImg, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 540.0f, 960.0f, 540.0f, 1920.0f, 1080.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1080, false, false);
    }

    private void renderArrows(SpriteBatch sb) {
        if (this.prevRelic != null) {
            sb.draw(ImageMaster.POPUP_ARROW, this.prevHb.cX - 128.0f, this.prevHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.pageLeftViewDeck.getKeyImg(), this.prevHb.cX - 32.0f + 0.0f * Settings.scale, this.prevHb.cY - 32.0f + 100.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            if (this.prevHb.hovered) {
                sb.setBlendFunction(770, 1);
                sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.5f));
                sb.draw(ImageMaster.POPUP_ARROW, this.prevHb.cX - 128.0f, this.prevHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
                sb.setColor(Color.WHITE);
                sb.setBlendFunction(770, 771);
            }
        }
        if (this.nextRelic != null) {
            sb.draw(ImageMaster.POPUP_ARROW, this.nextHb.cX - 128.0f, this.nextHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, true, false);
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.nextHb.cX - 32.0f + 0.0f * Settings.scale, this.nextHb.cY - 32.0f + 100.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            if (this.nextHb.hovered) {
                sb.setBlendFunction(770, 1);
                sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.5f));
                sb.draw(ImageMaster.POPUP_ARROW, this.nextHb.cX - 128.0f, this.nextHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, true, false);
                sb.setColor(Color.WHITE);
                sb.setBlendFunction(770, 771);
            }
        }
    }

    private void renderRelicImage(SpriteBatch sb) {
        if (UnlockTracker.isRelicLocked(this.relic.relicId)) {
            sb.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
            sb.draw(ImageMaster.RELIC_LOCK_OUTLINE, (float)Settings.WIDTH / 2.0f - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f + this.RELIC_OFFSET_Y, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 128, 128, false, false);
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.RELIC_LOCK, (float)Settings.WIDTH / 2.0f - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f + this.RELIC_OFFSET_Y, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 128, 128, false, false);
            return;
        }
        if (!this.relic.isSeen) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.75f));
        } else {
            sb.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
        }
        sb.draw(this.relic.outlineImg, (float)Settings.WIDTH / 2.0f - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f + this.RELIC_OFFSET_Y, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 128, 128, false, false);
        if (!this.relic.isSeen) {
            sb.setColor(Color.BLACK);
        } else {
            sb.setColor(Color.WHITE);
        }
        if (this.largeImg == null) {
            sb.draw(this.relic.img, (float)Settings.WIDTH / 2.0f - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f + this.RELIC_OFFSET_Y, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 128, 128, false, false);
        } else {
            sb.draw(this.largeImg, (float)Settings.WIDTH / 2.0f - 128.0f, (float)Settings.HEIGHT / 2.0f - 128.0f + this.RELIC_OFFSET_Y, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
        }
    }

    private void renderName(SpriteBatch sb) {
        if (UnlockTracker.isRelicLocked(this.relic.relicId)) {
            FontHelper.renderWrappedText(sb, FontHelper.SCP_cardDescFont, TEXT[8], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 265.0f * Settings.scale, 9999.0f, Settings.CREAM_COLOR, 0.9f);
        } else if (this.relic.isSeen) {
            FontHelper.renderWrappedText(sb, FontHelper.SCP_cardDescFont, this.relic.name, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 280.0f * Settings.scale, 9999.0f, Settings.CREAM_COLOR, 0.9f);
        } else {
            FontHelper.renderWrappedText(sb, FontHelper.SCP_cardDescFont, TEXT[9], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 265.0f * Settings.scale, 9999.0f, Settings.CREAM_COLOR, 0.9f);
        }
    }

    private void renderRarity(SpriteBatch sb) {
        Color tmpColor;
        switch (this.relic.tier) {
            case BOSS: {
                tmpColor = Settings.RED_TEXT_COLOR;
                break;
            }
            case RARE: {
                tmpColor = Settings.GOLD_COLOR;
                break;
            }
            case UNCOMMON: {
                tmpColor = Settings.BLUE_TEXT_COLOR;
                break;
            }
            case COMMON: {
                tmpColor = Settings.CREAM_COLOR;
                break;
            }
            case STARTER: {
                tmpColor = Settings.CREAM_COLOR;
                break;
            }
            case SPECIAL: {
                tmpColor = Settings.GOLD_COLOR;
                break;
            }
            case SHOP: {
                tmpColor = Settings.GOLD_COLOR;
                break;
            }
            default: {
                tmpColor = Settings.CREAM_COLOR;
            }
        }
        if (this.relic.isSeen) {
            if (Settings.language == Settings.GameLanguage.VIE) {
                FontHelper.renderWrappedText(sb, FontHelper.cardDescFont_N, TEXT[10] + this.rarityLabel, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 240.0f * Settings.scale, 9999.0f, tmpColor, 1.0f);
            } else {
                FontHelper.renderWrappedText(sb, FontHelper.cardDescFont_N, this.rarityLabel + TEXT[10], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 240.0f * Settings.scale, 9999.0f, tmpColor, 1.0f);
            }
        }
    }

    private void renderDescription(SpriteBatch sb) {
        if (UnlockTracker.isRelicLocked(this.relic.relicId)) {
            FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N, TEXT[11], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 140.0f * Settings.scale, Settings.CREAM_COLOR, 1.0f);
        } else if (this.relic.isSeen) {
            FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, this.relic.description, (float)Settings.WIDTH / 2.0f - 200.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f - 140.0f * Settings.scale - FontHelper.getSmartHeight(FontHelper.cardDescFont_N, this.relic.description, DESC_LINE_WIDTH, DESC_LINE_SPACING) / 2.0f, DESC_LINE_WIDTH, DESC_LINE_SPACING, Settings.CREAM_COLOR);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N, TEXT[12], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 140.0f * Settings.scale, Settings.CREAM_COLOR, 1.0f);
        }
    }

    private void renderQuote(SpriteBatch sb) {
        if (this.relic.isSeen) {
            if (this.relic.flavorText != null) {
                FontHelper.renderWrappedText(sb, FontHelper.SRV_quoteFont, this.relic.flavorText, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 310.0f * Settings.scale, DESC_LINE_WIDTH, Settings.CREAM_COLOR, 1.0f);
            } else {
                FontHelper.renderWrappedText(sb, FontHelper.SRV_quoteFont, "\"Missing quote...\"", (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 300.0f * Settings.scale, DESC_LINE_WIDTH, Settings.CREAM_COLOR, 1.0f);
            }
        }
    }

    private void renderTips(SpriteBatch sb) {
        if (this.relic.isSeen) {
            ArrayList<PowerTip> t = new ArrayList<PowerTip>();
            if (this.relic.tips.size() > 1) {
                for (int i = 1; i < this.relic.tips.size(); ++i) {
                    t.add(this.relic.tips.get(i));
                }
            }
            if (!t.isEmpty()) {
                TipHelper.queuePowerTips((float)Settings.WIDTH / 2.0f + 340.0f * Settings.scale, 420.0f * Settings.scale, t);
            }
        }
    }
}

