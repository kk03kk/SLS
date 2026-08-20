/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.compendium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;

public class RelicViewScreen
implements ScrollBarListener {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("RelicViewScreen");
    public static final String[] TEXT = RelicViewScreen.uiStrings.TEXT;
    private static final float SPACE = 80.0f * Settings.scale;
    private static final float START_X = 600.0f * Settings.scale;
    private static final float START_Y = (float)Settings.HEIGHT - 300.0f * Settings.scale;
    private float scrollY;
    private float targetY;
    private float scrollLowerBound;
    private float scrollUpperBound;
    private int row;
    private int col;
    public MenuCancelButton button;
    private AbstractRelic hoveredRelic;
    private AbstractRelic clickStartedRelic;
    private ArrayList<AbstractRelic> relicGroup;
    private boolean grabbedScreen;
    private float grabStartY;
    private ScrollBar scrollBar;
    private Hitbox controllerRelicHb;

    public RelicViewScreen() {
        this.targetY = this.scrollY = START_Y;
        this.scrollLowerBound = (float)Settings.HEIGHT - 100.0f * Settings.scale;
        this.scrollUpperBound = 3000.0f * Settings.scale;
        this.row = 0;
        this.col = 0;
        this.button = new MenuCancelButton();
        this.hoveredRelic = null;
        this.clickStartedRelic = null;
        this.relicGroup = null;
        this.grabbedScreen = false;
        this.grabStartY = 0.0f;
        this.controllerRelicHb = null;
        this.scrollBar = new ScrollBar(this);
    }

    public void open() {
        this.controllerRelicHb = null;
        if (Settings.isInfo) {
            RelicLibrary.unlockAndSeeAllRelics();
        }
        this.sortOnOpen();
        this.button.show(TEXT[0]);
        this.targetY = this.scrollLowerBound;
        this.scrollY = (float)Settings.HEIGHT - 400.0f * Settings.scale;
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.RELIC_VIEW;
    }

    private void sortOnOpen() {
        RelicLibrary.starterList = RelicLibrary.sortByStatus(RelicLibrary.starterList, false);
        RelicLibrary.commonList = RelicLibrary.sortByStatus(RelicLibrary.commonList, false);
        RelicLibrary.uncommonList = RelicLibrary.sortByStatus(RelicLibrary.uncommonList, false);
        RelicLibrary.rareList = RelicLibrary.sortByStatus(RelicLibrary.rareList, false);
        RelicLibrary.bossList = RelicLibrary.sortByStatus(RelicLibrary.bossList, false);
        RelicLibrary.specialList = RelicLibrary.sortByStatus(RelicLibrary.specialList, false);
        RelicLibrary.shopList = RelicLibrary.sortByStatus(RelicLibrary.shopList, false);
    }

    public void update() {
        if (!CardCrawlGame.relicPopup.isOpen) {
            boolean isScrollingScrollBar;
            this.updateControllerInput();
            if (Settings.isControllerMode && this.controllerRelicHb != null) {
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
            if (this.hoveredRelic != null) {
                CardCrawlGame.cursor.changeType(GameCursor.CursorType.INSPECT);
                if (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) {
                    this.clickStartedRelic = this.hoveredRelic;
                }
                if (InputHelper.justReleasedClickLeft || CInputActionSet.select.isJustPressed()) {
                    CInputActionSet.select.unpress();
                    if (this.hoveredRelic == this.clickStartedRelic) {
                        CardCrawlGame.relicPopup.open(this.hoveredRelic, this.relicGroup);
                        this.clickStartedRelic = null;
                    }
                }
            } else {
                this.clickStartedRelic = null;
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
            this.hoveredRelic = null;
            this.relicGroup = null;
            this.updateList(RelicLibrary.starterList);
            this.updateList(RelicLibrary.commonList);
            this.updateList(RelicLibrary.uncommonList);
            this.updateList(RelicLibrary.rareList);
            this.updateList(RelicLibrary.bossList);
            this.updateList(RelicLibrary.specialList);
            this.updateList(RelicLibrary.shopList);
            if (Settings.isControllerMode && this.controllerRelicHb != null) {
                Gdx.input.setCursorPosition((int)this.controllerRelicHb.cX, (int)((float)Settings.HEIGHT - this.controllerRelicHb.cY));
            }
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        RelicCategory category = RelicCategory.NONE;
        int index = 0;
        boolean anyHovered = false;
        for (AbstractRelic r : RelicLibrary.starterList) {
            if (r.hb.hovered) {
                anyHovered = true;
                category = RelicCategory.STARTER;
                break;
            }
            ++index;
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractRelic r : RelicLibrary.commonList) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = RelicCategory.COMMON;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractRelic r : RelicLibrary.uncommonList) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = RelicCategory.UNCOMMON;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractRelic r : RelicLibrary.rareList) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = RelicCategory.RARE;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractRelic r : RelicLibrary.bossList) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = RelicCategory.BOSS;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractRelic r : RelicLibrary.specialList) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = RelicCategory.EVENT;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractRelic r : RelicLibrary.shopList) {
                if (r.hb.hovered) {
                    anyHovered = true;
                    category = RelicCategory.SHOP;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            Gdx.input.setCursorPosition((int)RelicLibrary.starterList.get((int)0).hb.cX, Settings.HEIGHT - (int)RelicLibrary.starterList.get((int)0).hb.cY);
        } else {
            switch (category) {
                case STARTER: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) break;
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) <= RelicLibrary.starterList.size() - 1) break;
                        if ((index %= 10) > RelicLibrary.commonList.size() - 1) {
                            index = RelicLibrary.commonList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.commonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.commonList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.commonList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = RelicLibrary.starterList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.starterList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.starterList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.starterList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > RelicLibrary.starterList.size() - 1) {
                        index = 0;
                    }
                    Gdx.input.setCursorPosition((int)RelicLibrary.starterList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.starterList.get((int)index).hb.cY);
                    this.controllerRelicHb = RelicLibrary.starterList.get((int)index).hb;
                    break;
                }
                case COMMON: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if ((index -= 10) < 0) {
                            index = RelicLibrary.starterList.size() - 1;
                            Gdx.input.setCursorPosition((int)RelicLibrary.starterList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.starterList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.starterList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.commonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.commonList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.commonList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > RelicLibrary.commonList.size() - 1) {
                            if ((index %= 10) > RelicLibrary.uncommonList.size() - 1) {
                                index = RelicLibrary.uncommonList.size() - 1;
                            }
                            Gdx.input.setCursorPosition((int)RelicLibrary.uncommonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.uncommonList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.uncommonList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.commonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.commonList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.commonList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = RelicLibrary.commonList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.commonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.commonList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.commonList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > RelicLibrary.commonList.size() - 1) {
                        index = 0;
                    }
                    Gdx.input.setCursorPosition((int)RelicLibrary.commonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.commonList.get((int)index).hb.cY);
                    this.controllerRelicHb = RelicLibrary.commonList.get((int)index).hb;
                    break;
                }
                case UNCOMMON: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if ((index -= 10) < 0) {
                            index = RelicLibrary.commonList.size() - 1;
                            Gdx.input.setCursorPosition((int)RelicLibrary.commonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.commonList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.commonList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.uncommonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.uncommonList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.uncommonList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > RelicLibrary.uncommonList.size() - 1) {
                            if ((index %= 10) > RelicLibrary.rareList.size() - 1) {
                                index = RelicLibrary.rareList.size() - 1;
                            }
                            Gdx.input.setCursorPosition((int)RelicLibrary.rareList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.rareList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.rareList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.uncommonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.uncommonList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.uncommonList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = RelicLibrary.uncommonList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.uncommonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.uncommonList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.uncommonList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > RelicLibrary.uncommonList.size() - 1) {
                        index = 0;
                    }
                    Gdx.input.setCursorPosition((int)RelicLibrary.uncommonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.uncommonList.get((int)index).hb.cY);
                    this.controllerRelicHb = RelicLibrary.uncommonList.get((int)index).hb;
                    break;
                }
                case RARE: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if ((index -= 10) < 0) {
                            index = RelicLibrary.uncommonList.size() - 1;
                            Gdx.input.setCursorPosition((int)RelicLibrary.uncommonList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.uncommonList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.uncommonList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.rareList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.rareList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.rareList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > RelicLibrary.rareList.size() - 1) {
                            if ((index %= 10) > RelicLibrary.bossList.size() - 1) {
                                index = RelicLibrary.bossList.size() - 1;
                            }
                            Gdx.input.setCursorPosition((int)RelicLibrary.bossList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.bossList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.bossList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.rareList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.rareList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.rareList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = RelicLibrary.rareList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.rareList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.rareList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.rareList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > RelicLibrary.rareList.size() - 1) {
                        index = 0;
                    }
                    Gdx.input.setCursorPosition((int)RelicLibrary.rareList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.rareList.get((int)index).hb.cY);
                    this.controllerRelicHb = RelicLibrary.rareList.get((int)index).hb;
                    break;
                }
                case BOSS: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if ((index -= 10) < 0) {
                            index = RelicLibrary.rareList.size() - 1;
                            Gdx.input.setCursorPosition((int)RelicLibrary.rareList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.rareList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.rareList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.bossList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.bossList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.bossList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > RelicLibrary.bossList.size() - 1) {
                            if ((index %= 10) > RelicLibrary.specialList.size() - 1) {
                                index = RelicLibrary.specialList.size() - 1;
                            }
                            Gdx.input.setCursorPosition((int)RelicLibrary.specialList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.specialList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.specialList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.bossList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.bossList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.bossList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = RelicLibrary.bossList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.bossList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.bossList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.bossList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > RelicLibrary.bossList.size() - 1) {
                        index = 0;
                    }
                    Gdx.input.setCursorPosition((int)RelicLibrary.bossList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.bossList.get((int)index).hb.cY);
                    this.controllerRelicHb = RelicLibrary.bossList.get((int)index).hb;
                    break;
                }
                case EVENT: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if ((index -= 10) < 0) {
                            index = RelicLibrary.bossList.size() - 1;
                            Gdx.input.setCursorPosition((int)RelicLibrary.bossList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.bossList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.bossList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.specialList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.specialList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.specialList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > RelicLibrary.specialList.size() - 1) {
                            if ((index %= 10) > RelicLibrary.shopList.size() - 1) {
                                index = RelicLibrary.shopList.size() - 1;
                            }
                            Gdx.input.setCursorPosition((int)RelicLibrary.shopList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.shopList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.shopList.get((int)index).hb;
                            break;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.specialList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.specialList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.specialList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = RelicLibrary.specialList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.specialList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.specialList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.specialList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > RelicLibrary.specialList.size() - 1) {
                        index = 0;
                    }
                    Gdx.input.setCursorPosition((int)RelicLibrary.specialList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.specialList.get((int)index).hb.cY);
                    this.controllerRelicHb = RelicLibrary.specialList.get((int)index).hb;
                    break;
                }
                case SHOP: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if ((index -= 10) < 0) {
                            index = RelicLibrary.specialList.size() - 1;
                            Gdx.input.setCursorPosition((int)RelicLibrary.specialList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.specialList.get((int)index).hb.cY);
                            this.controllerRelicHb = RelicLibrary.specialList.get((int)index).hb;
                            break;
                        }
                        if (index > RelicLibrary.shopList.size() - 1) {
                            index = RelicLibrary.shopList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.shopList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.shopList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.shopList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if ((index += 10) > RelicLibrary.shopList.size() - 1) break;
                        Gdx.input.setCursorPosition((int)RelicLibrary.shopList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.shopList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.shopList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = RelicLibrary.shopList.size() - 1;
                        }
                        Gdx.input.setCursorPosition((int)RelicLibrary.shopList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.shopList.get((int)index).hb.cY);
                        this.controllerRelicHb = RelicLibrary.shopList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.right.isJustPressed() && !CInputActionSet.altRight.isJustPressed()) break;
                    if (++index > RelicLibrary.shopList.size() - 1) {
                        index = 0;
                    }
                    Gdx.input.setCursorPosition((int)RelicLibrary.shopList.get((int)index).hb.cX, Settings.HEIGHT - (int)RelicLibrary.shopList.get((int)index).hb.cY);
                    this.controllerRelicHb = RelicLibrary.shopList.get((int)index).hb;
                    break;
                }
            }
        }
    }

    private void updateScrolling() {
        if (!CardCrawlGame.relicPopup.isOpen) {
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
    }

    private void resetScrolling() {
        if (this.targetY < this.scrollLowerBound) {
            this.targetY = MathHelper.scrollSnapLerpSpeed(this.targetY, this.scrollLowerBound);
        } else if (this.targetY > this.scrollUpperBound) {
            this.targetY = MathHelper.scrollSnapLerpSpeed(this.targetY, this.scrollUpperBound);
        }
    }

    private void updateList(ArrayList<AbstractRelic> list) {
        if (!CardCrawlGame.relicPopup.isOpen) {
            for (AbstractRelic r : list) {
                r.hb.move(r.currentX, r.currentY);
                r.update();
                if (!r.hb.hovered) continue;
                this.hoveredRelic = r;
                this.relicGroup = list;
            }
        }
    }

    public void render(SpriteBatch sb) {
        this.row = -1;
        this.col = 0;
        this.renderList(sb, TEXT[1], TEXT[2], RelicLibrary.starterList);
        this.renderList(sb, TEXT[3], TEXT[4], RelicLibrary.commonList);
        this.renderList(sb, TEXT[5], TEXT[6], RelicLibrary.uncommonList);
        this.renderList(sb, TEXT[7], TEXT[8], RelicLibrary.rareList);
        this.renderList(sb, TEXT[9], TEXT[10], RelicLibrary.bossList);
        this.renderList(sb, TEXT[11], TEXT[12], RelicLibrary.specialList);
        this.renderList(sb, TEXT[13], TEXT[14], RelicLibrary.shopList);
        this.button.render(sb);
        this.scrollBar.render(sb);
    }

    private void renderList(SpriteBatch sb, String msg, String desc, ArrayList<AbstractRelic> list) {
        this.row += 2;
        FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, msg, START_X - 50.0f * Settings.scale, this.scrollY + 4.0f * Settings.scale - SPACE * (float)this.row, 99999.0f, 0.0f, Settings.GOLD_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, desc, START_X - 50.0f * Settings.scale + FontHelper.getSmartWidth(FontHelper.buttonLabelFont, msg, 99999.0f, 0.0f), this.scrollY - 0.0f * Settings.scale - SPACE * (float)this.row, 99999.0f, 0.0f, Settings.CREAM_COLOR);
        ++this.row;
        this.col = 0;
        for (AbstractRelic r : list) {
            if (this.col == 10) {
                this.col = 0;
                ++this.row;
            }
            r.currentX = START_X + SPACE * (float)this.col;
            r.currentY = this.scrollY - SPACE * (float)this.row;
            if (RelicLibrary.redList.contains(r)) {
                if (UnlockTracker.isRelicLocked(r.relicId)) {
                    r.renderLock(sb, Settings.RED_RELIC_COLOR);
                } else {
                    r.render(sb, false, Settings.RED_RELIC_COLOR);
                }
            } else if (RelicLibrary.greenList.contains(r)) {
                if (UnlockTracker.isRelicLocked(r.relicId)) {
                    r.renderLock(sb, Settings.GREEN_RELIC_COLOR);
                } else {
                    r.render(sb, false, Settings.GREEN_RELIC_COLOR);
                }
            } else if (RelicLibrary.blueList.contains(r)) {
                if (UnlockTracker.isRelicLocked(r.relicId)) {
                    r.renderLock(sb, Settings.BLUE_RELIC_COLOR);
                } else {
                    r.render(sb, false, Settings.BLUE_RELIC_COLOR);
                }
            } else if (RelicLibrary.whiteList.contains(r)) {
                if (UnlockTracker.isRelicLocked(r.relicId)) {
                    r.renderLock(sb, Settings.PURPLE_RELIC_COLOR);
                } else {
                    r.render(sb, false, Settings.PURPLE_RELIC_COLOR);
                }
            } else if (UnlockTracker.isRelicLocked(r.relicId)) {
                r.renderLock(sb, Settings.TWO_THIRDS_TRANSPARENT_BLACK_COLOR);
            } else {
                r.render(sb, false, Settings.TWO_THIRDS_TRANSPARENT_BLACK_COLOR);
            }
            ++this.col;
        }
    }

    @Override
    public void scrolledUsingBar(float newPercent) {
        float newPosition;
        this.scrollY = newPosition = MathHelper.valueFromPercentBetween(this.scrollLowerBound, this.scrollUpperBound, newPercent);
        this.targetY = newPosition;
        this.updateBarPosition();
    }

    private void updateBarPosition() {
        if (!CardCrawlGame.relicPopup.isOpen) {
            float percent = MathHelper.percentFromValueBetween(this.scrollLowerBound, this.scrollUpperBound, this.scrollY);
            this.scrollBar.parentScrolledToPercent(percent);
        }
    }

    private static enum RelicCategory {
        STARTER,
        COMMON,
        UNCOMMON,
        RARE,
        BOSS,
        EVENT,
        SHOP,
        NONE;

    }
}

