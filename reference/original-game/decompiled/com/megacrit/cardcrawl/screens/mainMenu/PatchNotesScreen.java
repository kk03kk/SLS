/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

public class PatchNotesScreen
implements ScrollBarListener {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("PatchNotesScreen");
    public static final String[] TEXT = PatchNotesScreen.uiStrings.TEXT;
    private static String text = null;
    private FileHandle log;
    private static final float START_Y = (float)Settings.HEIGHT - 300.0f * Settings.scale;
    private float scrollY;
    private float targetY;
    private float scrollLowerBound;
    private float scrollUpperBound;
    public MenuCancelButton button;
    private static final float LINE_WIDTH = 1200.0f * Settings.scale;
    private static final float LINE_SPACING = 30.0f * Settings.scale;
    private boolean grabbedScreen;
    private float grabStartY;
    private ScrollBar scrollBar;

    public PatchNotesScreen() {
        this.targetY = this.scrollY = START_Y;
        this.scrollLowerBound = (float)Settings.HEIGHT - 300.0f * Settings.scale;
        this.scrollUpperBound = 2400.0f * Settings.scale;
        this.button = new MenuCancelButton();
        this.grabbedScreen = false;
        this.grabStartY = 0.0f;
        this.scrollBar = new ScrollBar(this);
    }

    public void open() {
        this.button.show(TEXT[0]);
        this.targetY = this.scrollLowerBound;
        this.scrollY = (float)Settings.HEIGHT - 400.0f * Settings.scale;
        CardCrawlGame.mainMenuScreen.darken();
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.PATCH_NOTES;
        if (text == null) {
            this.log = Settings.isBeta ? Gdx.files.internal("changelog" + File.separator + "notes.txt") : Gdx.files.internal("changelog" + File.separator + "notes_main.txt");
            this.openLog();
            this.scrollUpperBound = this.calculateHeight() + 300.0f * Settings.scale;
            if (this.scrollUpperBound < this.scrollLowerBound) {
                this.scrollUpperBound = this.scrollLowerBound + 100.0f * Settings.scale;
            }
        } else {
            this.targetY = this.scrollY = START_Y;
        }
    }

    private float calculateHeight() {
        return FontHelper.getHeight(FontHelper.tipBodyFont, text, 1.0f);
    }

    private void openLog() {
        try (BufferedReader br = new BufferedReader(this.log.reader());){
            StringBuilder sb = new StringBuilder();
            String line = "";
            try {
                line = br.readLine();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            text = sb.toString();
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        boolean isDraggingScrollBar;
        if (Settings.isControllerMode) {
            if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                this.targetY += Settings.SCROLL_SPEED * 2.0f;
            } else if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                this.targetY -= Settings.SCROLL_SPEED * 2.0f;
            } else if (CInputActionSet.drawPile.isJustPressed()) {
                this.targetY -= Settings.SCROLL_SPEED * 10.0f;
            } else if (CInputActionSet.discardPile.isJustPressed()) {
                this.targetY += Settings.SCROLL_SPEED * 10.0f;
            }
        }
        this.button.update();
        if (this.button.hb.clicked || InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
            this.button.hide();
            CardCrawlGame.mainMenuScreen.lighten();
        }
        if (!(isDraggingScrollBar = this.scrollBar.update())) {
            this.updateScrolling();
        }
        InputHelper.justClickedLeft = false;
    }

    private void updateScrolling() {
        int y = InputHelper.mY;
        if (!this.grabbedScreen) {
            if (InputHelper.scrolledDown) {
                this.targetY += Settings.SCROLL_SPEED;
            } else if (InputHelper.scrolledUp) {
                this.targetY -= Settings.SCROLL_SPEED;
            }
            if (InputHelper.justClickedLeft) {
                this.grabbedScreen = true;
                this.grabStartY = (float)y - this.targetY;
            }
        } else if (InputHelper.isMouseDown) {
            this.targetY = (float)y - this.grabStartY;
        } else {
            this.grabbedScreen = false;
        }
        this.scrollY = MathHelper.scrollSnapLerpSpeed(this.scrollY, this.targetY);
        this.resetScrolling();
        this.updateBarPosition();
    }

    private void resetScrolling() {
        if (this.targetY < this.scrollLowerBound) {
            this.targetY = MathHelper.scrollSnapLerpSpeed(this.targetY, this.scrollLowerBound);
        } else if (this.targetY > this.scrollUpperBound) {
            this.targetY = MathHelper.scrollSnapLerpSpeed(this.targetY, this.scrollUpperBound);
        }
    }

    public void render(SpriteBatch sb) {
        FontHelper.renderSmartText(sb, FontHelper.panelNameFont, TEXT[1], 250.0f * Settings.scale, this.scrollY + 70.0f * Settings.scale, LINE_WIDTH, LINE_SPACING, Settings.CREAM_COLOR);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, text, 300.0f * Settings.scale, this.scrollY, Settings.CREAM_COLOR);
        this.button.render(sb);
        this.scrollBar.render(sb);
    }

    @Override
    public void scrolledUsingBar(float newPercent) {
        this.targetY = this.scrollY = MathHelper.valueFromPercentBetween(this.scrollLowerBound, this.scrollUpperBound, newPercent);
        this.updateBarPosition();
    }

    private void updateBarPosition() {
        float percent = MathHelper.percentFromValueBetween(this.scrollLowerBound, this.scrollUpperBound, this.scrollY);
        this.scrollBar.parentScrolledToPercent(percent);
    }
}

