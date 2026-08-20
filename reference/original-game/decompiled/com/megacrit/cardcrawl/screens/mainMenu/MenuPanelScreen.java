/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.integrations.DistributorFactory;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuPanelButton;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import java.util.ArrayList;

public class MenuPanelScreen {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("MenuPanels");
    public static final String[] TEXT = MenuPanelScreen.uiStrings.TEXT;
    public ArrayList<MainMenuPanelButton> panels = new ArrayList();
    private PanelScreen screen;
    private static final float PANEL_Y = (float)Settings.HEIGHT / 2.0f;
    public MenuCancelButton button = new MenuCancelButton();

    public void open(PanelScreen screenType) {
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.PANEL_MENU;
        this.screen = screenType;
        this.initializePanels();
        this.button.show(CharacterSelectScreen.TEXT[5]);
        CardCrawlGame.mainMenuScreen.darken();
    }

    public void hide() {
    }

    private void initializePanels() {
        this.panels.clear();
        switch (this.screen) {
            case PLAY: {
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.PLAY_NORMAL, MainMenuPanelButton.PanelColor.BLUE, (float)Settings.WIDTH / 2.0f - 450.0f * Settings.scale, PANEL_Y));
                if (CardCrawlGame.mainMenuScreen.statsScreen.statScreenUnlocked()) {
                    this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.PLAY_DAILY, MainMenuPanelButton.PanelColor.BEIGE, (float)Settings.WIDTH / 2.0f, PANEL_Y));
                } else {
                    this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.PLAY_DAILY, MainMenuPanelButton.PanelColor.GRAY, (float)Settings.WIDTH / 2.0f, PANEL_Y));
                }
                if (StatsScreen.all.highestDaily > 0) {
                    this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.PLAY_CUSTOM, MainMenuPanelButton.PanelColor.RED, (float)Settings.WIDTH / 2.0f + 450.0f * Settings.scale, PANEL_Y));
                    break;
                }
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.PLAY_CUSTOM, MainMenuPanelButton.PanelColor.GRAY, (float)Settings.WIDTH / 2.0f + 450.0f * Settings.scale, PANEL_Y));
                break;
            }
            case COMPENDIUM: {
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.INFO_CARD, MainMenuPanelButton.PanelColor.BLUE, (float)Settings.WIDTH / 2.0f - 450.0f * Settings.scale, PANEL_Y));
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.INFO_RELIC, MainMenuPanelButton.PanelColor.BEIGE, (float)Settings.WIDTH / 2.0f, PANEL_Y));
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.INFO_POTION, MainMenuPanelButton.PanelColor.RED, (float)Settings.WIDTH / 2.0f + 450.0f * Settings.scale, PANEL_Y));
                break;
            }
            case STATS: {
                float offset = 225.0f;
                if (DistributorFactory.isLeaderboardEnabled()) {
                    offset = 450.0f;
                }
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.STAT_CHAR, MainMenuPanelButton.PanelColor.BLUE, (float)Settings.WIDTH / 2.0f - offset * Settings.scale, PANEL_Y));
                if (DistributorFactory.isLeaderboardEnabled()) {
                    this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.STAT_LEADERBOARDS, MainMenuPanelButton.PanelColor.BEIGE, (float)Settings.WIDTH / 2.0f, PANEL_Y));
                }
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.STAT_HISTORY, MainMenuPanelButton.PanelColor.RED, (float)Settings.WIDTH / 2.0f + offset * Settings.scale, PANEL_Y));
                break;
            }
            case SETTINGS: {
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.SETTINGS_GAME, MainMenuPanelButton.PanelColor.BLUE, (float)Settings.WIDTH / 2.0f - 450.0f * Settings.scale, PANEL_Y));
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.SETTINGS_INPUT, MainMenuPanelButton.PanelColor.BLUE, (float)Settings.WIDTH / 2.0f, PANEL_Y));
                this.panels.add(new MainMenuPanelButton(MainMenuPanelButton.PanelClickResult.SETTINGS_CREDITS, MainMenuPanelButton.PanelColor.BLUE, (float)Settings.WIDTH / 2.0f + 450.0f * Settings.scale, PANEL_Y));
                break;
            }
        }
    }

    public void update() {
        this.button.update();
        if (this.button.hb.clicked || InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
            this.button.hide();
            CardCrawlGame.mainMenuScreen.lighten();
        }
        for (MainMenuPanelButton panel : this.panels) {
            panel.update();
        }
    }

    public void render(SpriteBatch sb) {
        for (MainMenuPanelButton panel : this.panels) {
            panel.render(sb);
        }
        this.button.render(sb);
    }

    public void refresh() {
        this.button.hideInstantly();
        this.button.show(CharacterSelectScreen.TEXT[5]);
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.PANEL_MENU;
    }

    public static enum PanelScreen {
        PLAY,
        COMPENDIUM,
        STATS,
        SETTINGS;

    }
}

