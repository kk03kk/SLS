/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.daily.TimeHelper;
import com.megacrit.cardcrawl.daily.TimeLookup;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.BadWordChecker;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.integrations.DistributorFactory;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.screens.leaderboards.LeaderboardEntry;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DailyScreen {
    private static final Logger logger = LogManager.getLogger(DailyScreen.class.getName());
    public static final String[] TEXT = CardCrawlGame.languagePack.getUIString((String)"DailyScreen").TEXT;
    public static final String[] TEXT_2 = CardCrawlGame.languagePack.getUIString((String)"LeaderboardsScreen").TEXT;
    private long lastDaily = 0L;
    public AbstractPlayer todaysChar = null;
    private Color screenColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private Color fadeColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private Color redTextColor = new Color(1.0f, 0.3f, 0.3f, 0.0f);
    private Color creamColor = new Color(1.0f, 0.965f, 0.886f, 0.0f);
    private float alphaTarget = 0.0f;
    private MenuCancelButton cancelButton = new MenuCancelButton();
    private ConfirmButton confirmButton = new ConfirmButton(TEXT[4]);
    private float screenX = -1100.0f * Settings.scale;
    private float targetX = -1100.0f * Settings.scale;
    private float header_x = 186.0f * Settings.scale;
    private float date_x;
    private float body_x = 208.0f * Settings.scale;
    private float char_x = 304.0f * Settings.scale;
    private float mode_desc_x = 264.0f * Settings.scale;
    private float mod_icon_x = 200.0f * Settings.scale;
    private float center_mod_offset_x = 500.0f * Settings.scale;
    private static final int CHAR_W = 128;
    private static final int MOD_W = 64;
    private static final float TITLE_Y = (float)Settings.HEIGHT / 2.0f + 350.0f * Settings.scale;
    private static final float TIME_LEFT_Y = (float)Settings.HEIGHT / 2.0f + 310.0f * Settings.scale;
    private static final float CHAR_IMAGE_Y = (float)Settings.HEIGHT / 2.0f + 160.0f * Settings.scale;
    private static final float CHAR_HEADER_Y = (float)Settings.HEIGHT / 2.0f + 250.0f * Settings.scale;
    private static final float MOD_HEADER_Y = (float)Settings.HEIGHT / 2.0f + 136.0f * Settings.scale;
    private static final float MOD_LINE_W = 500.0f * Settings.scale;
    private static final float MOD_LINE_SPACING = 30.0f * Settings.scale;
    private static final float MOD_SECTION_SPACING = 60.0f * Settings.scale;
    private DateFormat dFormat;
    private boolean timeLookupActive = false;
    private boolean timeUpdated = false;
    private Random todayRng;
    private long currentDay = 0L;
    public boolean waiting = true;
    public boolean viewMyScore = false;
    public int currentStartIndex = 1;
    public int currentEndIndex = 20;
    public ArrayList<LeaderboardEntry> entries = new ArrayList();
    private Hitbox prevHb;
    private Hitbox nextHb;
    private Hitbox viewMyScoreHb = new Hitbox(250.0f * Settings.scale, 80.0f * Settings.scale);
    private Hitbox prevDayHb;
    private Hitbox nextDayHb;
    public static final float RANK_X = 1000.0f * Settings.scale;
    public static final float NAME_X = 1160.0f * Settings.scale;
    public static final float SCORE_X = 1500.0f * Settings.scale;
    private float lineFadeInTimer = 0.0f;
    private static final float LINE_THICKNESS = 4.0f * Settings.scale;

    public DailyScreen() {
        if (!DistributorFactory.isLeaderboardEnabled()) {
            this.header_x += this.center_mod_offset_x;
            this.body_x += this.center_mod_offset_x;
            this.char_x += this.center_mod_offset_x;
            this.mode_desc_x += this.center_mod_offset_x;
            this.mod_icon_x += this.center_mod_offset_x;
        }
        this.date_x = this.header_x + FontHelper.getWidth(FontHelper.charTitleFont, TEXT[0], 1.0f) + 12.0f * Settings.scale;
        this.prevHb = new Hitbox(110.0f * Settings.scale, 110.0f * Settings.scale);
        this.prevHb.move(880.0f * Settings.scale, 530.0f * Settings.scale);
        this.nextHb = new Hitbox(110.0f * Settings.scale, 110.0f * Settings.scale);
        this.nextHb.move(1740.0f * Settings.scale, 530.0f * Settings.scale);
        this.prevDayHb = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        this.prevDayHb.move(1320.0f * Settings.scale, 900.0f * Settings.scale);
        this.nextDayHb = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        this.nextDayHb.move(1600.0f * Settings.scale, 900.0f * Settings.scale);
        this.viewMyScoreHb.move(1300.0f * Settings.scale, 80.0f * Settings.scale);
    }

    public void update() {
        this.cancelButton.update();
        if (this.cancelButton.hb.clicked) {
            FontHelper.ClearLeaderboardFontTextures();
            this.hide();
            CardCrawlGame.mainMenuScreen.panelScreen.refresh();
        }
        this.confirmButton.update();
        this.screenColor.a = MathHelper.popLerpSnap(this.screenColor.a, this.alphaTarget);
        this.screenX = MathHelper.uiLerpSnap(this.screenX, this.targetX);
        this.pingTimeServer();
        if (this.confirmButton.hb.clicked) {
            this.confirmButton.hb.clicked = false;
            CardCrawlGame.chosenCharacter = this.todaysChar.chosenClass;
            CardCrawlGame.mainMenuScreen.isFadingOut = true;
            this.hide();
            Settings.isTrial = false;
            Settings.isDailyRun = true;
            Settings.dailyDate = TimeHelper.daySince1970();
            Settings.isEndless = false;
            CardCrawlGame.mainMenuScreen.fadeOutMusic();
        }
        this.updateLeaderboardSection();
    }

    private void updateLeaderboardSection() {
        if (this.waiting && this.currentDay == 0L && TimeHelper.daySince1970() != 0L) {
            this.currentDay = TimeHelper.daySince1970();
            this.getData(1, 20);
        }
        if (!this.entries.isEmpty() && !this.waiting) {
            this.lineFadeInTimer = MathHelper.slowColorLerpSnap(this.lineFadeInTimer, 1.0f);
        }
        this.updateDateChangeArrows();
        this.updateArrows();
        this.updateViewMyScore();
    }

    private void updateDateChangeArrows() {
        float xOffset;
        if (this.waiting) {
            return;
        }
        this.prevDayHb.update();
        if (this.prevDayHb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        } else if (this.prevDayHb.hovered && InputHelper.justClickedLeft) {
            this.prevDayHb.clickStarted = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        } else if (this.prevDayHb.clicked || CInputActionSet.drawPile.isJustPressed()) {
            if (this.currentDay == 0L) {
                this.currentDay = TimeHelper.daySince1970();
            }
            --this.currentDay;
            this.prevDayHb.clicked = false;
            this.waiting = true;
            xOffset = FontHelper.getSmartWidth(FontHelper.panelNameFont, TimeHelper.getDate(this.currentDay), 9999.0f, 0.0f);
            this.nextDayHb.move(this.prevDayHb.cX + xOffset + 76.0f * Settings.scale, this.nextDayHb.cY);
            this.getData(1, 20);
        }
        if (this.currentDay != 0L && this.currentDay < TimeHelper.daySince1970()) {
            this.nextDayHb.update();
        }
        if (this.nextDayHb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        } else if (this.nextDayHb.hovered && InputHelper.justClickedLeft) {
            this.nextDayHb.clickStarted = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        } else if (this.nextDayHb.clicked || CInputActionSet.discardPile.isJustPressed() && this.currentDay != 0L && this.currentDay < TimeHelper.daySince1970()) {
            if (this.currentDay == 0L) {
                this.currentDay = TimeHelper.daySince1970();
            }
            ++this.currentDay;
            this.nextDayHb.clicked = false;
            this.waiting = true;
            xOffset = FontHelper.getSmartWidth(FontHelper.panelNameFont, TimeHelper.getDate(this.currentDay), 9999.0f, 0.0f);
            this.nextDayHb.move(this.prevDayHb.cX + xOffset + 76.0f * Settings.scale, this.nextDayHb.cY);
            this.getData(1, 20);
        }
    }

    private void updateArrows() {
        if (this.waiting) {
            return;
        }
        if (this.entries.size() == 20) {
            this.nextHb.update();
            if (this.nextHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            } else if (this.nextHb.hovered && InputHelper.justClickedLeft) {
                this.nextHb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            } else if (this.nextHb.clicked || CInputActionSet.pageRightViewExhaust.isJustPressed()) {
                this.nextHb.clicked = false;
                this.currentStartIndex = this.currentEndIndex + 1;
                this.currentEndIndex = this.currentStartIndex + 19;
                this.waiting = true;
                this.getData(this.currentStartIndex, this.currentEndIndex);
            }
        }
        if (this.currentStartIndex != 1) {
            this.prevHb.update();
            if (this.prevHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            } else if (this.prevHb.hovered && InputHelper.justClickedLeft) {
                this.prevHb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            } else if (this.prevHb.clicked || CInputActionSet.pageLeftViewDeck.isJustPressed()) {
                this.prevHb.clicked = false;
                this.currentStartIndex -= 20;
                if (this.currentStartIndex < 1) {
                    this.currentStartIndex = 1;
                }
                this.currentEndIndex = this.currentStartIndex + 19;
                this.waiting = true;
                this.getData(this.currentStartIndex, this.currentEndIndex);
            }
        }
    }

    private void updateViewMyScore() {
        if (this.waiting) {
            return;
        }
        this.viewMyScoreHb.update();
        if (this.viewMyScoreHb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        } else if (this.viewMyScoreHb.hovered && InputHelper.justClickedLeft) {
            this.viewMyScoreHb.clickStarted = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        } else if (this.viewMyScoreHb.clicked || CInputActionSet.topPanel.isJustPressed()) {
            this.viewMyScoreHb.clicked = false;
            this.viewMyScore = true;
            this.waiting = true;
            this.getData(this.currentStartIndex, this.currentEndIndex);
        }
    }

    private void getData(int startIndex, int endIndex) {
        if (this.currentDay != 0L) {
            CardCrawlGame.publisherIntegration.getDailyLeaderboard(this.currentDay, startIndex, endIndex);
        }
    }

    private void pingTimeServer() {
        if (TimeHelper.isTimeSet && !this.timeUpdated) {
            this.timeUpdated = true;
            this.dFormat = new SimpleDateFormat(TEXT[6]);
            this.dFormat.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
            this.determineLoadout();
            this.getData(1, 20);
        } else if (!this.timeLookupActive) {
            TimeLookup.fetchDailyTimeAsync();
            this.timeLookupActive = true;
        }
    }

    public void determineLoadout() {
        long todaySeed = TimeHelper.daySince1970();
        Settings.specialSeed = todaySeed;
        logger.info("Today's mods: " + ModHelper.getEnabledModIDs().toString());
        AbstractDungeon.isAscensionMode = false;
        this.todayRng = new Random(todaySeed);
        this.todaysChar = CardCrawlGame.characterManager.getRandomCharacter(this.todayRng);
        Settings.seed = this.todayRng.randomLong();
        String seedText = SeedHelper.getString(Settings.seed);
        if (BadWordChecker.containsBadWord(seedText)) {
            Settings.seed = SeedHelper.generateUnoffensiveSeed(this.todayRng);
        }
        AbstractDungeon.generateSeeds();
        ModHelper.setTodaysMods(todaySeed, this.todaysChar.chosenClass);
        this.confirmButton.isDisabled = false;
        this.confirmButton.show();
        logger.info(TEXT[5] + this.todaysChar.chosenClass.name());
    }

    public void open() {
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.DAILY;
        CardCrawlGame.mainMenuScreen.darken();
        this.alphaTarget = 0.8f;
        this.cancelButton.show(TEXT[3]);
        this.targetX = 100.0f * Settings.scale;
        this.confirmButton.isDisabled = true;
        this.confirmButton.hide();
        this.waiting = true;
        this.timeUpdated = false;
        this.viewMyScore = false;
        this.currentStartIndex = 1;
        this.currentEndIndex = 20;
        this.entries.clear();
    }

    public void hide() {
        this.alphaTarget = 0.0f;
        this.cancelButton.hide();
        this.targetX = -1100.0f * Settings.scale;
        this.confirmButton.hide();
    }

    public void render(SpriteBatch sb) {
        this.renderTitle(sb);
        if (TimeHelper.isTimeSet) {
            this.renderTimeLeft(sb);
            this.renderCharacterAndNotice(sb);
            this.renderMods(sb);
            this.confirmButton.render(sb);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.charDescFont, TEXT[1], this.screenX + 50.0f * Settings.scale, 786.0f * Settings.scale, 9999.0f, MOD_LINE_SPACING, Settings.CREAM_COLOR);
        }
        this.renderScoreHeaders(sb);
        this.renderScoreDateToggler(sb);
        this.renderViewMyScoreBox(sb);
        this.renderArrows(sb);
        this.renderScores(sb);
        this.renderDateToggleArrows(sb);
        this.renderDisclaimer(sb);
        this.cancelButton.render(sb);
    }

    private void renderDateToggleArrows(SpriteBatch sb) {
        sb.draw(ImageMaster.CF_LEFT_ARROW, this.prevDayHb.cX - 24.0f, this.prevDayHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 48, 48, false, false);
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.drawPile.getKeyImg(), this.prevDayHb.cX - 32.0f - 60.0f * Settings.scale, this.prevDayHb.cY - 32.0f + 0.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        if (this.currentDay != 0L && this.currentDay < TimeHelper.daySince1970()) {
            sb.draw(ImageMaster.CF_RIGHT_ARROW, this.nextDayHb.cX - 24.0f, this.nextDayHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 48, 48, false, false);
            this.nextDayHb.render(sb);
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.discardPile.getKeyImg(), this.nextDayHb.cX - 32.0f + 60.0f * Settings.scale, this.nextDayHb.cY - 32.0f + 0.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
        }
        this.prevDayHb.render(sb);
    }

    private void renderScoreDateToggler(SpriteBatch sb) {
        if (this.currentDay == 0L) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, TimeHelper.getTodayDate(), 1360.0f * Settings.scale, 910.0f * Settings.scale, new Color(0.53f, 0.808f, 0.92f, this.lineFadeInTimer));
        } else {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, TimeHelper.getDate(this.currentDay), 1360.0f * Settings.scale, 910.0f * Settings.scale, new Color(0.53f, 0.808f, 0.92f, this.lineFadeInTimer));
        }
    }

    private void renderTitle(SpriteBatch sb) {
        FontHelper.renderFontLeftDownAligned(sb, FontHelper.charTitleFont, TEXT[0], this.header_x, TITLE_Y, Settings.GOLD_COLOR);
        String offlineModeNotice = "";
        if (TimeHelper.isOfflineMode()) {
            offlineModeNotice = TEXT[16];
        }
        if (TimeHelper.isTimeSet) {
            FontHelper.renderFontLeftDownAligned(sb, FontHelper.charDescFont, TimeHelper.getTodayDate() + offlineModeNotice, this.date_x, TITLE_Y, Color.SKY);
        }
    }

    private void renderTimeLeft(SpriteBatch sb) {
        FontHelper.renderFontLeftDownAligned(sb, FontHelper.charDescFont, TEXT[7] + TimeHelper.getTimeLeft(), this.body_x, TIME_LEFT_Y, Settings.CREAM_COLOR);
    }

    private void renderCharacterAndNotice(SpriteBatch sb) {
        if (this.todaysChar == null) {
            return;
        }
        Texture img = null;
        StringBuilder builder = new StringBuilder("#y");
        builder.append(TEXT_2[2]);
        builder.append(" NL ");
        img = this.todaysChar.getCustomModeCharacterButtonImage();
        if (this.lastDaily != TimeHelper.daySince1970()) {
            builder.append(this.todaysChar.getLocalizedCharacterName());
        }
        if (img != null) {
            sb.draw(img, this.header_x, CHAR_IMAGE_Y, 128.0f * Settings.scale, 128.0f * Settings.scale);
        }
        if (this.lastDaily == TimeHelper.daySince1970()) {
            FontHelper.renderSmartText(sb, FontHelper.charDescFont, TEXT[2], this.char_x, CHAR_HEADER_Y, 9999.0f, MOD_LINE_SPACING, Settings.CREAM_COLOR);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.charDescFont, builder.toString(), this.char_x, CHAR_HEADER_Y, 9999.0f, MOD_LINE_SPACING, Settings.CREAM_COLOR);
        }
    }

    private void renderMods(SpriteBatch sb) {
        FontHelper.renderFont(sb, FontHelper.charDescFont, TEXT[13], this.header_x, MOD_HEADER_Y, Settings.GOLD_COLOR);
        float y = MOD_HEADER_Y - MOD_SECTION_SPACING;
        for (AbstractDailyMod mod : ModHelper.enabledMods) {
            StringBuilder builder = new StringBuilder();
            if (mod.positive) {
                builder.append(FontHelper.colorString(mod.name, "g"));
            } else {
                builder.append(FontHelper.colorString(mod.name, "r"));
            }
            builder.append(": ");
            builder.append(mod.description);
            FontHelper.renderSmartText(sb, FontHelper.charDescFont, builder.toString(), this.mode_desc_x, y, MOD_LINE_W, MOD_LINE_SPACING, Settings.CREAM_COLOR);
            sb.draw(mod.img, this.mod_icon_x, y - 44.0f * Settings.scale, 64.0f * Settings.scale, 64.0f * Settings.scale);
            y += FontHelper.getSmartHeight(FontHelper.charDescFont, builder.toString(), MOD_LINE_W, MOD_LINE_SPACING) - MOD_SECTION_SPACING;
        }
    }

    private void renderArrows(SpriteBatch sb) {
        boolean renderLeftArrow = true;
        if (this.currentStartIndex != 1 && renderLeftArrow) {
            this.fadeColor.a = this.lineFadeInTimer;
            sb.setColor(this.fadeColor);
            sb.draw(ImageMaster.POPUP_ARROW, this.prevHb.cX - 128.0f, this.prevHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 256, 256, false, false);
            if (this.prevHb.hovered) {
                sb.setBlendFunction(770, 1);
                this.fadeColor.a = this.lineFadeInTimer / 2.0f;
                sb.setColor(this.fadeColor);
                sb.draw(ImageMaster.POPUP_ARROW, this.prevHb.cX - 128.0f, this.prevHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 256, 256, false, false);
                sb.setBlendFunction(770, 771);
            }
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.pageLeftViewDeck.getKeyImg(), this.prevHb.cX - 32.0f + 0.0f * Settings.scale, this.prevHb.cY - 32.0f - 100.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            this.prevHb.render(sb);
        }
        if (this.entries.size() == 20) {
            this.fadeColor.a = this.lineFadeInTimer;
            sb.setColor(this.fadeColor);
            sb.draw(ImageMaster.POPUP_ARROW, this.nextHb.cX - 128.0f, this.nextHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 256, 256, true, false);
            if (this.nextHb.hovered) {
                sb.setBlendFunction(770, 1);
                this.fadeColor.a = this.lineFadeInTimer / 2.0f;
                sb.setColor(this.fadeColor);
                sb.draw(ImageMaster.POPUP_ARROW, this.nextHb.cX - 128.0f, this.nextHb.cY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 256, 256, true, false);
                sb.setBlendFunction(770, 771);
            }
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.nextHb.cX - 32.0f + 0.0f * Settings.scale, this.nextHb.cY - 32.0f - 100.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            this.nextHb.render(sb);
        }
    }

    private void renderDisclaimer(SpriteBatch sb) {
        this.redTextColor.a = this.lineFadeInTimer;
        if (!Settings.usesTrophies) {
            FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT[15], (float)Settings.WIDTH * 0.26f, 80.0f * Settings.scale * this.lineFadeInTimer, this.redTextColor);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT[18], (float)Settings.WIDTH * 0.26f, 80.0f * Settings.scale * this.lineFadeInTimer, this.redTextColor);
        }
    }

    private void renderScoreHeaders(SpriteBatch sb) {
        this.creamColor.a = this.lineFadeInTimer;
        FontHelper.renderFontRightTopAligned(sb, FontHelper.charTitleFont, TEXT[14], 1570.0f * Settings.scale, 980.0f * Settings.scale, new Color(0.937f, 0.784f, 0.317f, this.lineFadeInTimer));
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, TEXT_2[6], RANK_X, 860.0f * Settings.scale, this.creamColor);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, TEXT_2[7], NAME_X, 860.0f * Settings.scale, this.creamColor);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, TEXT_2[8], SCORE_X, 860.0f * Settings.scale, this.creamColor);
        sb.setColor(this.creamColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 1138.0f * Settings.scale, 168.0f * Settings.scale, LINE_THICKNESS, 692.0f * Settings.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 1480.0f * Settings.scale, 168.0f * Settings.scale, LINE_THICKNESS, 692.0f * Settings.scale);
        sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.lineFadeInTimer * 0.75f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 982.0f * Settings.scale, 814.0f * Settings.scale, 630.0f * Settings.scale, 16.0f * Settings.scale);
        sb.setColor(this.creamColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 982.0f * Settings.scale, 820.0f * Settings.scale, 630.0f * Settings.scale, LINE_THICKNESS);
    }

    private void renderViewMyScoreBox(SpriteBatch sb) {
        if (this.waiting) {
            return;
        }
        FontHelper.cardTitleFont.getData().setScale(1.0f);
        if (this.viewMyScoreHb.hovered) {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT_2[5], 1310.0f * Settings.scale, 80.0f * Settings.scale, Settings.GREEN_TEXT_COLOR);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT_2[5], 1310.0f * Settings.scale, 80.0f * Settings.scale, Settings.GOLD_COLOR);
        }
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.topPanel.getKeyImg(), 1270.0f * Settings.scale - 32.0f - FontHelper.getSmartWidth(FontHelper.cardTitleFont, TEXT_2[5], 9999.0f, 0.0f) / 2.0f, -32.0f + 80.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.viewMyScoreHb.render(sb);
    }

    private void renderScores(SpriteBatch sb) {
        if (DistributorFactory.isLeaderboardEnabled()) {
            if (!this.waiting) {
                if (this.entries.isEmpty()) {
                    FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT_2[12], 1300.0f * Settings.scale, 540.0f * Settings.scale, Settings.GOLD_COLOR);
                } else {
                    for (int i = 0; i < this.entries.size(); ++i) {
                        this.entries.get(i).render(sb, i);
                    }
                }
            } else if (CardCrawlGame.publisherIntegration.isInitialized()) {
                FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT_2[9], 1300.0f * Settings.scale, 540.0f * Settings.scale, Settings.GOLD_COLOR);
            } else {
                FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT_2[11], 1300.0f * Settings.scale, 540.0f * Settings.scale, Settings.RED_TEXT_COLOR);
            }
        }
    }
}

