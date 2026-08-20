/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.compendium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import java.util.ArrayList;

public class PotionViewScreen
implements ScrollBarListener {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("PotionViewScreen");
    public static final String[] TEXT = PotionViewScreen.uiStrings.TEXT;
    private static final float SPACE = 80.0f * Settings.scale;
    private static final float START_X = 600.0f * Settings.scale;
    private float scrollY;
    private float targetY;
    private float scrollUpperBound;
    private float scrollLowerBound;
    private int row;
    private int col;
    public MenuCancelButton button;
    private ArrayList<AbstractPotion> commonPotions;
    private ArrayList<AbstractPotion> uncommonPotions;
    private ArrayList<AbstractPotion> rarePotions;
    private boolean grabbedScreen;
    private float grabStartY;
    private ScrollBar scrollBar;
    private Hitbox controllerPotionHb;

    public PotionViewScreen() {
        this.targetY = this.scrollY = (float)Settings.HEIGHT;
        this.scrollUpperBound = (float)Settings.HEIGHT - 100.0f * Settings.scale;
        this.scrollLowerBound = (float)Settings.HEIGHT / 2.0f;
        this.row = 0;
        this.col = 0;
        this.button = new MenuCancelButton();
        this.commonPotions = null;
        this.uncommonPotions = null;
        this.rarePotions = null;
        this.grabbedScreen = false;
        this.grabStartY = 0.0f;
        this.controllerPotionHb = null;
        this.scrollBar = new ScrollBar(this);
    }

    public void open() {
        this.controllerPotionHb = null;
        this.sortOnOpen();
        this.button.show(TEXT[0]);
        this.targetY = this.scrollUpperBound - 50.0f * Settings.scale;
        this.scrollY = Settings.HEIGHT;
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.POTION_VIEW;
    }

    private void sortOnOpen() {
        this.commonPotions = PotionHelper.getPotionsByRarity(AbstractPotion.PotionRarity.COMMON);
        this.uncommonPotions = PotionHelper.getPotionsByRarity(AbstractPotion.PotionRarity.UNCOMMON);
        this.rarePotions = PotionHelper.getPotionsByRarity(AbstractPotion.PotionRarity.RARE);
    }

    public void update() {
        boolean isScrollingScrollBar;
        this.updateControllerInput();
        if (Settings.isModded && Settings.isControllerMode && this.controllerPotionHb != null) {
            if ((float)Gdx.input.getY() > (float)Settings.HEIGHT * 0.7f) {
                this.targetY += Settings.SCROLL_SPEED;
                if (this.targetY > this.scrollUpperBound) {
                    this.targetY = this.scrollUpperBound;
                }
            } else if ((float)Gdx.input.getY() < (float)Settings.HEIGHT * 0.3f) {
                this.targetY -= Settings.SCROLL_SPEED;
                if (this.targetY < this.scrollLowerBound) {
                    this.targetY = this.scrollLowerBound;
                }
            }
        }
        this.button.update();
        if (this.button.hb.clicked || InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            this.button.hide();
            CardCrawlGame.mainMenuScreen.panelScreen.refresh();
        }
        if (!(isScrollingScrollBar = this.scrollBar.update())) {
            this.updateScrolling();
        }
        InputHelper.justClickedLeft = false;
        this.updateList(this.commonPotions);
        this.updateList(this.uncommonPotions);
        this.updateList(this.rarePotions);
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        PotionCategory category = PotionCategory.NONE;
        int index = 0;
        boolean anyHovered = false;
        for (AbstractPotion r : this.commonPotions) {
            if (r.hb.hovered) {
                anyHovered = true;
                category = PotionCategory.COMMON;
                break;
            }
            ++index;
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractPotion r : this.uncommonPotions) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = PotionCategory.UNCOMMON;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractPotion r : this.rarePotions) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = PotionCategory.RARE;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            System.out.println("NONE HOVERED");
            CInputHelper.setCursor(this.commonPotions.get((int)0).hb);
        } else {
            switch (category) {
                case COMMON: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if (index >= 10) {
                            index -= 10;
                        }
                        CInputHelper.setCursor(this.commonPotions.get((int)index).hb);
                        this.controllerPotionHb = this.commonPotions.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > this.commonPotions.size() - 1) {
                            if ((index %= 10) > this.uncommonPotions.size() - 1) {
                                index = this.uncommonPotions.size() - 1;
                            }
                            CInputHelper.setCursor(this.uncommonPotions.get((int)index).hb);
                            this.controllerPotionHb = this.uncommonPotions.get((int)index).hb;
                            break;
                        }
                        CInputHelper.setCursor(this.commonPotions.get((int)index).hb);
                        this.controllerPotionHb = this.commonPotions.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = this.commonPotions.size() - 1;
                        }
                        CInputHelper.setCursor(this.commonPotions.get((int)index).hb);
                        this.controllerPotionHb = this.commonPotions.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > this.commonPotions.size() - 1) {
                        index = 0;
                    }
                    CInputHelper.setCursor(this.commonPotions.get((int)index).hb);
                    this.controllerPotionHb = this.commonPotions.get((int)index).hb;
                    break;
                }
                case UNCOMMON: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if ((index -= 10) < 0) {
                            index = this.commonPotions.size() - 1;
                            CInputHelper.setCursor(this.commonPotions.get((int)index).hb);
                            this.controllerPotionHb = this.commonPotions.get((int)index).hb;
                            break;
                        }
                        CInputHelper.setCursor(this.uncommonPotions.get((int)index).hb);
                        this.controllerPotionHb = this.uncommonPotions.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > this.uncommonPotions.size() - 1) {
                            if ((index %= 10) > this.rarePotions.size() - 1) {
                                index = this.rarePotions.size() - 1;
                            }
                            CInputHelper.setCursor(this.rarePotions.get((int)index).hb);
                            this.controllerPotionHb = this.rarePotions.get((int)index).hb;
                            break;
                        }
                        CInputHelper.setCursor(this.uncommonPotions.get((int)index).hb);
                        this.controllerPotionHb = this.uncommonPotions.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = this.uncommonPotions.size() - 1;
                        }
                        CInputHelper.setCursor(this.uncommonPotions.get((int)index).hb);
                        this.controllerPotionHb = this.uncommonPotions.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > this.uncommonPotions.size() - 1) {
                        index = 0;
                    }
                    CInputHelper.setCursor(this.uncommonPotions.get((int)index).hb);
                    this.controllerPotionHb = this.uncommonPotions.get((int)index).hb;
                    break;
                }
                case RARE: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        CInputHelper.setCursor(this.uncommonPotions.get((int)index).hb);
                        this.controllerPotionHb = this.uncommonPotions.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = this.rarePotions.size() - 1;
                        }
                        CInputHelper.setCursor(this.rarePotions.get((int)index).hb);
                        this.controllerPotionHb = this.rarePotions.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > this.rarePotions.size() - 1) {
                        index = 0;
                    }
                    CInputHelper.setCursor(this.rarePotions.get((int)index).hb);
                    this.controllerPotionHb = this.rarePotions.get((int)index).hb;
                    break;
                }
            }
        }
    }

    private void updateScrolling() {
        int y = InputHelper.mY;
        if (!this.grabbedScreen && Settings.isModded) {
            if (InputHelper.scrolledDown) {
                this.targetY += Settings.SCROLL_SPEED;
            } else if (InputHelper.scrolledUp) {
                this.targetY -= Settings.SCROLL_SPEED;
            }
            if (InputHelper.justClickedLeft) {
                this.grabbedScreen = true;
                this.grabStartY = (float)y - this.targetY;
            }
        } else if (InputHelper.isMouseDown && Settings.isModded) {
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

    private void updateList(ArrayList<AbstractPotion> list) {
        for (AbstractPotion r : list) {
            r.hb.update();
            r.hb.move(r.posX, r.posY);
            r.update();
        }
    }

    public void render(SpriteBatch sb) {
        this.row = -1;
        this.col = 0;
        this.renderList(sb, TEXT[1], TEXT[2], this.commonPotions);
        this.renderList(sb, TEXT[3], TEXT[4], this.uncommonPotions);
        this.renderList(sb, TEXT[5], TEXT[6], this.rarePotions);
        this.button.render(sb);
        if (Settings.isModded) {
            this.scrollBar.render(sb);
        }
    }

    private void renderList(SpriteBatch sb, String msg, String desc, ArrayList<AbstractPotion> list) {
        this.row += 2;
        FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, msg, START_X - 50.0f * Settings.scale, this.scrollY + 4.0f * Settings.scale - SPACE * (float)this.row, 99999.0f, 0.0f, Settings.GOLD_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, desc, START_X - 50.0f * Settings.scale + FontHelper.getSmartWidth(FontHelper.buttonLabelFont, msg, 99999.0f, 0.0f), this.scrollY - 0.0f * Settings.scale - SPACE * (float)this.row, 99999.0f, 0.0f, Settings.CREAM_COLOR);
        ++this.row;
        this.col = 0;
        for (AbstractPotion r : list) {
            if (this.col == 10) {
                this.col = 0;
                ++this.row;
            }
            r.posX = START_X + SPACE * (float)this.col;
            r.posY = this.scrollY - SPACE * (float)this.row;
            r.labRender(sb);
            ++this.col;
        }
    }

    @Override
    public void scrolledUsingBar(float newPercent) {
        float newPosition;
        if (!Settings.isModded) {
            return;
        }
        this.scrollY = newPosition = MathHelper.valueFromPercentBetween(this.scrollLowerBound, this.scrollUpperBound, newPercent);
        this.targetY = newPosition;
        this.updateBarPosition();
    }

    private void updateBarPosition() {
        float percent = MathHelper.percentFromValueBetween(this.scrollLowerBound, this.scrollUpperBound, this.scrollY);
        this.scrollBar.parentScrolledToPercent(percent);
    }

    private static enum PotionCategory {
        NONE,
        COMMON,
        UNCOMMON,
        RARE;

    }
}

