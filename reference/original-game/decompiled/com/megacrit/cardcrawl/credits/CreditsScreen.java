/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.credits;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.credits.CreditLine;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.CreditStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.DoorUnlockScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import java.util.ArrayList;

public class CreditsScreen {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CreditsScreen");
    private Color screenColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private float fadeInTimer = 2.0f;
    private float targetY;
    private float currentY;
    private static final float scrollSpeed = 50.0f * Settings.scale;
    private static final int W = 720;
    private ArrayList<CreditLine> lines = new ArrayList();
    private static final float LINE_SPACING = 45.0f;
    private static final float SECTION_SPACING = 150.0f;
    private static final float SCROLL_START_Y = 400.0f * Settings.scale;
    private static final float THANK_YOU_TIME = 3.0f;
    private float thankYouTimer = 3.0f;
    private Color thankYouColor = Settings.CREAM_COLOR.cpy();
    private static float END_OF_CREDITS_Y = 15000.0f * Settings.scale;
    private String THANKS_MSG;
    private static Texture logoImg = null;
    private float skipTimer;
    private static final float SKIP_MENU_UP_DUR = 2.0f;
    private static final float SKIP_APPEAR_TIME = 0.5f;
    private boolean isSkippable;
    private boolean closingSkipMenu;
    private boolean showNeowAfter;
    private static final float SKIP_START_X = -300.0f * Settings.scale;
    private static final float SKIP_END_X = 50.0f * Settings.scale;
    private float skipX;
    private float tmpY;

    public CreditsScreen() {
        this.THANKS_MSG = CardCrawlGame.languagePack.getCreditString((String)"THANKS_MSG").HEADER;
        this.showNeowAfter = false;
        this.tmpY = -400.0f;
        this.targetY = this.currentY = SCROLL_START_Y;
        this.creditLineHelper("DEV");
        this.creditLineHelper("OPS");
        this.creditLineHelper("SOUND");
        this.creditLineHelper("VOICE");
        this.creditLineHelper("PORTRAITS");
        this.creditLineHelper("ILLUSTRATION");
        this.creditLineHelper("ANIMATION");
        if (Settings.isConsoleBuild) {
            this.creditLineHelper("PORTING");
            this.creditLineHelper("PUBLISHING");
            this.creditLineHelper("KAKEHASHI");
            END_OF_CREDITS_Y += 1600.0f * Settings.scale;
        }
        this.creditLineHelper("LOC_ZHS");
        this.creditLineHelper("LOC_ZHT");
        this.creditLineHelper("LOC_DEU");
        this.creditLineHelper("LOC_DUT");
        this.creditLineHelper("LOC_EPO");
        this.creditLineHelper("LOC_FIN");
        this.creditLineHelper("LOC_FRA");
        this.creditLineHelper("LOC_GRE");
        this.creditLineHelper("LOC_IND");
        this.creditLineHelper("LOC_ITA");
        this.creditLineHelper("LOC_JPN");
        this.creditLineHelper("LOC_KOR");
        this.creditLineHelper("LOC_NOR");
        this.creditLineHelper("LOC_POL");
        this.creditLineHelper("LOC_PTB");
        this.creditLineHelper("LOC_RUS");
        this.creditLineHelper("LOC_SPA");
        this.creditLineHelper("LOC_SRB");
        this.creditLineHelper("LOC_THA");
        this.creditLineHelper("LOC_TUR");
        this.creditLineHelper("LOC_UKR");
        this.creditLineHelper("LOC_VIE");
        this.creditLineHelper("LOC_ADDITIONAL");
        this.creditLineHelper("TEST");
        this.creditLineHelper("SPECIAL");
        if (logoImg == null) {
            switch (Settings.language) {
                default: 
            }
            logoImg = ImageMaster.loadImage("images/ui/credits_logo/eng.png");
        }
    }

    private void creditLineHelper(String id) {
        CreditStrings str = CardCrawlGame.languagePack.getCreditString(id);
        this.lines.add(new CreditLine(str.HEADER, this.tmpY -= 150.0f, true));
        for (int i = 0; i < str.NAMES.length; ++i) {
            this.lines.add(new CreditLine(str.NAMES[i], this.tmpY -= 45.0f, false));
        }
    }

    public void open(boolean playCreditsBgm) {
        if (playCreditsBgm) {
            this.showNeowAfter = true;
            CardCrawlGame.playCreditsBgm = false;
            CardCrawlGame.music.playTempBgmInstantly("CREDITS", true);
        } else {
            this.showNeowAfter = false;
        }
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.CREDITS;
        this.skipTimer = 0.0f;
        this.isSkippable = false;
        this.closingSkipMenu = false;
        this.skipX = SKIP_START_X;
        GameCursor.hidden = true;
        this.thankYouColor.a = 0.0f;
        this.screenColor.a = 0.0f;
        this.thankYouTimer = 3.0f;
        CardCrawlGame.mainMenuScreen.darken();
        this.fadeInTimer = 2.0f;
        this.targetY = this.currentY = SCROLL_START_Y;
    }

    public void update() {
        if (InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            this.close();
        }
        if (InputHelper.isMouseDown_R) {
            this.targetY -= Gdx.graphics.getDeltaTime() * scrollSpeed * 4.0f;
        } else {
            this.targetY += Gdx.graphics.getDeltaTime() * scrollSpeed;
            if (this.currentY > END_OF_CREDITS_Y) {
                this.thankYouTimer -= Gdx.graphics.getDeltaTime();
                if (this.thankYouTimer < 0.0f) {
                    this.thankYouTimer = 0.0f;
                }
                this.thankYouColor.a = Interpolation.fade.apply(1.0f, 0.0f, this.thankYouTimer / 3.0f);
            }
        }
        if (this.thankYouColor.a == 0.0f) {
            if (Gdx.input.isKeyJustPressed(62)) {
                this.targetY = SCROLL_START_Y;
            }
            if (InputHelper.scrolledUp) {
                this.targetY -= 100.0f * Settings.scale;
            } else if (InputHelper.scrolledDown) {
                this.targetY += 100.0f * Settings.scale;
            }
            if (CInputActionSet.up.isJustPressed()) {
                this.targetY -= 300.0f * Settings.scale;
            } else if (CInputActionSet.down.isJustPressed()) {
                this.targetY += 300.0f * Settings.scale;
            } else if (CInputActionSet.inspectDown.isJustPressed()) {
                this.targetY -= 1000.0f * Settings.scale;
            } else if (CInputActionSet.inspectUp.isJustPressed()) {
                this.targetY += 1000.0f * Settings.scale;
            }
        }
        this.currentY = MathHelper.scrollSnapLerpSpeed(this.currentY, this.targetY);
        this.updateFade();
        this.skipLogic();
    }

    private void skipLogic() {
        if (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) {
            if (this.isSkippable) {
                this.close();
            } else if (!this.isSkippable && this.skipTimer == 0.0f) {
                this.skipTimer = 0.5f;
                this.skipX = SKIP_END_X;
            }
        }
        if (this.skipTimer != 0.0f) {
            this.skipTimer -= Gdx.graphics.getDeltaTime();
            this.skipX = !this.isSkippable && !this.closingSkipMenu ? Interpolation.swingIn.apply(SKIP_END_X, SKIP_START_X, this.skipTimer * 2.0f) : (this.closingSkipMenu ? Interpolation.fade.apply(SKIP_START_X, SKIP_END_X, this.skipTimer * 2.0f) : SKIP_END_X);
            if (this.skipTimer < 0.0f) {
                if (!this.isSkippable && !this.closingSkipMenu) {
                    this.isSkippable = true;
                    this.skipTimer = 2.0f;
                } else if (!this.closingSkipMenu) {
                    this.closingSkipMenu = true;
                    this.isSkippable = false;
                    this.skipTimer = 0.5f;
                } else {
                    this.isSkippable = false;
                    this.closingSkipMenu = false;
                    this.skipTimer = 0.0f;
                }
            }
        }
    }

    public void close() {
        if (this.showNeowAfter) {
            CardCrawlGame.music.justFadeOutTempBGM();
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.NEOW_SCREEN;
            CardCrawlGame.mainMenuScreen.neowNarrateScreen.open();
        } else if (DoorUnlockScreen.show) {
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.DOOR_UNLOCK;
            CardCrawlGame.mainMenuScreen.doorUnlockScreen.open(false);
        } else if (CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.CREDITS) {
            CardCrawlGame.mainMenuScreen.lighten();
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
            CardCrawlGame.music.fadeOutTempBGM();
            GameCursor.hidden = false;
        }
    }

    private void updateFade() {
        this.fadeInTimer -= Gdx.graphics.getDeltaTime();
        if (this.fadeInTimer < 0.0f) {
            this.fadeInTimer = 0.0f;
        }
        this.screenColor.a = Interpolation.fade.apply(1.0f, 0.0f, this.fadeInTimer / 2.0f);
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.screenColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.screenColor.a));
        sb.draw(logoImg, (float)Settings.WIDTH / 2.0f - 360.0f, this.currentY - 360.0f, 360.0f, 360.0f, 720.0f, 720.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 720, 720, false, false);
        for (CreditLine c : this.lines) {
            c.render(sb, this.currentY);
        }
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.THANKS_MSG, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f, this.thankYouColor);
        if (Settings.isTouchScreen) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, CreditsScreen.uiStrings.TEXT[2], this.skipX, 50.0f * Settings.scale, Settings.BLUE_TEXT_COLOR);
        } else if (!Settings.isControllerMode) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, CreditsScreen.uiStrings.TEXT[0], this.skipX, 50.0f * Settings.scale, Settings.BLUE_TEXT_COLOR);
        } else {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, CreditsScreen.uiStrings.TEXT[1], this.skipX + 46.0f * Settings.scale, 50.0f * Settings.scale, Settings.BLUE_TEXT_COLOR);
            sb.setColor(Color.WHITE);
            sb.draw(CInputActionSet.select.getKeyImg(), this.skipX - 32.0f + 10.0f * Settings.scale, -32.0f + 44.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.9f, Settings.scale * 0.9f, 0.0f, 0, 0, 64, 64, false, false);
        }
    }
}

