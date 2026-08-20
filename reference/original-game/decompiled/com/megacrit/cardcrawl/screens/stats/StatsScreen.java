/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.stats;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.AchievementStrings;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import com.megacrit.cardcrawl.screens.stats.AchievementGrid;
import com.megacrit.cardcrawl.screens.stats.AchievementItem;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StatsScreen
implements ScrollBarListener {
    private static final Logger logger = LogManager.getLogger(StatsScreen.class.getName());
    private static final AchievementStrings achievementStrings = CardCrawlGame.languagePack.getAchievementString("StatsScreen");
    public static final String[] NAMES = StatsScreen.achievementStrings.NAMES;
    public static final String[] TEXT = StatsScreen.achievementStrings.TEXT;
    public MenuCancelButton button = new MenuCancelButton();
    public Hitbox allCharsHb = new Hitbox(150.0f * Settings.scale, 150.0f * Settings.scale);
    public Hitbox ironcladHb = new Hitbox(150.0f * Settings.scale, 150.0f * Settings.scale);
    public Hitbox silentHb;
    public Hitbox defectHb;
    public Hitbox watcherHb;
    public Hitbox controllerHb;
    public boolean screenUp = false;
    private static final float SHOW_X = 300.0f * Settings.scale;
    private static final float HIDE_X = -800.0f * Settings.scale;
    private float screenX = HIDE_X;
    private float targetX = HIDE_X;
    private boolean grabbedScreen = false;
    private float grabStartY = 0.0f;
    private float scrollTargetY = 0.0f;
    private float scrollY = 0.0f;
    private float scrollLowerBound = -Settings.DEFAULT_SCROLL_LIMIT;
    private float scrollUpperBound = Settings.DEFAULT_SCROLL_LIMIT;
    private ScrollBar scrollBar = null;
    public static CharStat all;
    public static AchievementGrid achievements;
    public static TextureAtlas atlas;

    public StatsScreen() {
        logger.info("Loading character stats.");
        CardCrawlGame.characterManager.refreshAllCharStats();
        all = new CharStat(CardCrawlGame.characterManager.getAllCharacterStats());
        achievements = new AchievementGrid();
        Settings.totalPlayTime = StatsScreen.all.playTime;
    }

    public void refreshData() {
        logger.info("Refreshing stats screen data.");
        CardCrawlGame.characterManager.refreshAllCharStats();
        all = new CharStat(CardCrawlGame.characterManager.getAllCharacterStats());
        achievements = new AchievementGrid();
        Settings.totalPlayTime = StatsScreen.all.playTime;
    }

    public void update() {
        this.updateControllerInput();
        if (Settings.isControllerMode && this.controllerHb != null) {
            if ((float)Gdx.input.getY() > (float)Settings.HEIGHT * 0.75f) {
                this.scrollTargetY += Settings.SCROLL_SPEED;
            } else if ((float)Gdx.input.getY() < (float)Settings.HEIGHT * 0.25f) {
                this.scrollTargetY -= Settings.SCROLL_SPEED;
            }
        }
        if (Settings.isControllerMode && this.controllerHb != null) {
            Gdx.input.setCursorPosition((int)this.controllerHb.cX, (int)((float)Settings.HEIGHT - this.controllerHb.cY));
        }
        this.button.update();
        if (this.button.hb.clicked || InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
            this.hide();
        }
        this.screenX = MathHelper.uiLerpSnap(this.screenX, this.targetX);
        boolean isDraggingScrollBar = this.scrollBar.update();
        if (!isDraggingScrollBar) {
            this.updateScrolling();
        }
        achievements.update();
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        boolean anyHovered = false;
        int index = 0;
        this.allCharsHb.update();
        this.ironcladHb.update();
        if (this.silentHb != null) {
            this.silentHb.update();
        }
        if (this.defectHb != null) {
            this.defectHb.update();
        }
        if (this.watcherHb != null) {
            this.watcherHb.update();
        }
        if (this.allCharsHb != null && this.allCharsHb.hovered) {
            anyHovered = true;
        }
        index = 0;
        if (!anyHovered) {
            for (AchievementItem a : StatsScreen.achievements.items) {
                a.hb.update();
                if (a.hb.hovered) {
                    anyHovered = true;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            if (this.ironcladHb != null && this.ironcladHb.hovered) {
                anyHovered = true;
            }
            if (this.silentHb != null && this.silentHb.hovered) {
                anyHovered = true;
            }
            if (this.defectHb != null && this.defectHb.hovered) {
                anyHovered = true;
            }
            if (this.watcherHb != null && this.watcherHb.hovered) {
                anyHovered = true;
            }
        }
        if (!anyHovered) {
            CInputHelper.setCursor(this.allCharsHb);
            this.controllerHb = this.allCharsHb;
        } else if (this.allCharsHb.hovered) {
            if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(StatsScreen.achievements.items.get((int)0).hb);
                this.controllerHb = StatsScreen.achievements.items.get((int)0).hb;
            }
        } else if (this.ironcladHb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                index = StatsScreen.achievements.items.size() - StatsScreen.achievements.items.size() % 5;
                CInputHelper.setCursor(StatsScreen.achievements.items.get((int)index).hb);
                this.controllerHb = StatsScreen.achievements.items.get((int)index).hb;
            } else if ((CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) && this.silentHb != null) {
                CInputHelper.setCursor(this.silentHb);
                this.controllerHb = this.silentHb;
            }
        } else if (this.silentHb != null && this.silentHb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.ironcladHb);
                this.controllerHb = this.ironcladHb;
            } else if ((CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) && this.defectHb != null) {
                CInputHelper.setCursor(this.defectHb);
                this.controllerHb = this.defectHb;
            }
        } else if (this.defectHb != null && this.defectHb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.silentHb);
                this.controllerHb = this.silentHb;
            } else if ((CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) && this.watcherHb != null) {
                CInputHelper.setCursor(this.watcherHb);
                this.controllerHb = this.watcherHb;
            }
        } else if (this.watcherHb != null && this.watcherHb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.defectHb);
                this.controllerHb = this.defectHb;
            }
        } else if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
            if ((index -= 5) < 0) {
                CInputHelper.setCursor(this.allCharsHb);
                this.controllerHb = this.allCharsHb;
            } else {
                CInputHelper.setCursor(StatsScreen.achievements.items.get((int)index).hb);
                this.controllerHb = StatsScreen.achievements.items.get((int)index).hb;
            }
        } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
            if ((index += 5) > StatsScreen.achievements.items.size() - 1) {
                CInputHelper.setCursor(this.ironcladHb);
                this.controllerHb = this.ironcladHb;
            } else {
                CInputHelper.setCursor(StatsScreen.achievements.items.get((int)index).hb);
                this.controllerHb = StatsScreen.achievements.items.get((int)index).hb;
            }
        } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
            if ((--index % 5 == 4 || index == -1) && (index += 5) > StatsScreen.achievements.items.size() - 1) {
                index = StatsScreen.achievements.items.size() - 1;
            }
            CInputHelper.setCursor(StatsScreen.achievements.items.get((int)index).hb);
            this.controllerHb = StatsScreen.achievements.items.get((int)index).hb;
        } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
            if (++index % 5 == 0) {
                index -= 5;
            }
            if (index > StatsScreen.achievements.items.size() - 1) {
                index = StatsScreen.achievements.items.size() - StatsScreen.achievements.items.size() % 5;
            }
            CInputHelper.setCursor(StatsScreen.achievements.items.get((int)index).hb);
            this.controllerHb = StatsScreen.achievements.items.get((int)index).hb;
        }
    }

    private void updateScrolling() {
        int y = InputHelper.mY;
        if (!this.grabbedScreen) {
            if (InputHelper.scrolledDown) {
                this.scrollTargetY += Settings.SCROLL_SPEED;
            } else if (InputHelper.scrolledUp) {
                this.scrollTargetY -= Settings.SCROLL_SPEED;
            }
            if (InputHelper.justClickedLeft) {
                this.grabbedScreen = true;
                this.grabStartY = (float)y - this.scrollTargetY;
            }
        } else if (InputHelper.isMouseDown) {
            this.scrollTargetY = (float)y - this.grabStartY;
        } else {
            this.grabbedScreen = false;
        }
        this.scrollY = MathHelper.scrollSnapLerpSpeed(this.scrollY, this.scrollTargetY);
        this.resetScrolling();
        this.updateBarPosition();
    }

    private void calculateScrollBounds() {
        this.scrollUpperBound = !UnlockTracker.isCharacterLocked("Watcher") ? 3400.0f * Settings.scale : (!UnlockTracker.isCharacterLocked("Defect") ? 3000.0f * Settings.scale : (!UnlockTracker.isCharacterLocked("The Silent") ? 2550.0f * Settings.scale : 2250.0f * Settings.scale));
        this.scrollLowerBound = 100.0f * Settings.yScale;
    }

    private void resetScrolling() {
        if (this.scrollTargetY < this.scrollLowerBound) {
            this.scrollTargetY = MathHelper.scrollSnapLerpSpeed(this.scrollTargetY, this.scrollLowerBound);
        } else if (this.scrollTargetY > this.scrollUpperBound) {
            this.scrollTargetY = MathHelper.scrollSnapLerpSpeed(this.scrollTargetY, this.scrollUpperBound);
        }
    }

    public void open() {
        if (!Settings.isConsoleBuild) {
            if (CardCrawlGame.publisherIntegration.isInitialized() && CardCrawlGame.publisherIntegration.getNumUnlockedAchievements() >= 45) {
                CardCrawlGame.publisherIntegration.unlockAchievement("ETERNAL_ONE");
            }
            StatsScreen.retroactiveAmethystUnlock();
        }
        if (UnlockTracker.isAchievementUnlocked("RUBY_PLUS") && UnlockTracker.isAchievementUnlocked("EMERALD_PLUS") && UnlockTracker.isAchievementUnlocked("SAPPHIRE_PLUS")) {
            UnlockTracker.unlockAchievement("THE_ENDING");
        }
        if (atlas == null) {
            atlas = new TextureAtlas(Gdx.files.internal("achievements/achievements.atlas"));
            logger.info("Loaded texture Achievement texture atlas.");
        }
        this.controllerHb = null;
        this.scrollTargetY = 0.0f;
        this.debugCharacterUnlock();
        this.targetX = SHOW_X;
        this.button.show(TEXT[0]);
        this.screenUp = true;
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.STATS;
        if (this.scrollBar == null) {
            this.refreshData();
            this.calculateScrollBounds();
            this.scrollBar = new ScrollBar(this);
            logger.info("Creating new scrollbar for Stats Screen.");
        } else {
            achievements.refreshImg();
        }
        if (Settings.isControllerMode) {
            Gdx.input.setCursorPosition((int)this.allCharsHb.cX, Settings.HEIGHT - (int)this.allCharsHb.cY);
            this.controllerHb = this.allCharsHb;
        }
        this.debugAchievementUnlock();
    }

    private void debugAchievementUnlock() {
        if (Settings.isInfo) {
            for (AchievementItem i : StatsScreen.achievements.items) {
                UnlockTracker.unlockAchievement(i.key);
            }
        }
    }

    private void debugCharacterUnlock() {
        if (Settings.isInfo) {
            for (String s : UnlockTracker.lockedCharacters) {
                UnlockTracker.hardUnlockOverride(s);
            }
        }
    }

    public void hide() {
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
        }
        CardCrawlGame.sound.play("DECK_CLOSE", 0.1f);
        this.targetX = HIDE_X;
        this.button.hide();
        this.screenUp = false;
        CardCrawlGame.mainMenuScreen.panelScreen.refresh();
    }

    public static void updateFurthestAscent(int floor) {
        AbstractDungeon.player.getCharStat().furthestAscent(floor);
    }

    public static void updateHighestScore(int score) {
        AbstractDungeon.player.getCharStat().highestScore(score);
    }

    public static void updateHighestDailyScore(int score) {
        AbstractDungeon.player.getCharStat().highestDaily(score);
    }

    public static void updateVictoryTime(long time) {
        logger.info("Saving fastest victory...");
        AbstractDungeon.player.getCharStat().updateFastestVictory(time);
    }

    public static void incrementFloorClimbed() {
        AbstractDungeon.player.getCharStat().incrementFloorClimbed();
    }

    public static boolean isPlayingHighestAscension(Prefs p) {
        return AbstractDungeon.ascensionLevel == p.getInteger("ASCENSION_LEVEL", 1);
    }

    public static void retroactiveAscend10Unlock(Prefs pref) {
        if (pref.getInteger("ASCENSION_LEVEL", 0) >= 11) {
            UnlockTracker.unlockAchievement("ASCEND_10");
        }
    }

    public static void retroactiveAscend20Unlock(Prefs pref) {
        if (pref.getInteger("ASCENSION_LEVEL", 0) >= 21) {
            UnlockTracker.unlockAchievement("ASCEND_20");
        }
    }

    public static void retroactiveAmethystUnlock() {
        if (UnlockTracker.isAchievementUnlocked("AMETHYST_PLUS")) {
            UnlockTracker.unlockAchievement("AMETHYST");
        }
    }

    public static int getVictory(CharStat c) {
        return c.getVictoryCount();
    }

    public static void unlockAscension(CharStat c) {
        c.unlockAscension();
    }

    public static void incrementVictory(CharStat c) {
        c.incrementVictory();
    }

    public static void incrementAscension(CharStat c) {
        c.incrementAscension();
    }

    public static void incrementDeath(CharStat c) {
        c.incrementDeath();
    }

    public static void incrementVictoryIfZero(CharStat c) {
        if (c.getVictoryCount() == 0) {
            c.incrementVictory();
        }
    }

    public static void incrementEnemySlain() {
        AbstractDungeon.player.getCharStat().incrementEnemySlain();
    }

    public static void incrementBossSlain() {
        AbstractDungeon.player.getCharStat().incrementBossSlain();
    }

    public static void incrementPlayTime(long time) {
        AbstractDungeon.player.getCharStat().incrementPlayTime(time);
    }

    public void render(SpriteBatch sb) {
        this.renderStatScreen(sb);
        this.button.render(sb);
        this.scrollBar.render(sb);
    }

    private void renderStatScreen(SpriteBatch sb) {
        float renderY = this.scrollY;
        StatsScreen.renderHeader(sb, NAMES[0], this.screenX, renderY);
        all.render(sb, this.screenX, renderY);
        StatsScreen.renderHeader(sb, NAMES[1], this.screenX, renderY -= 400.0f * Settings.scale);
        achievements.render(sb, renderY);
        renderY -= 2200.0f * Settings.scale;
        for (AbstractPlayer c : CardCrawlGame.characterManager.getAllCharacters()) {
            c.renderStatScreen(sb, this.screenX, renderY);
            renderY -= 400.0f * Settings.scale;
        }
        if (Settings.isControllerMode) {
            this.allCharsHb.move(300.0f * Settings.scale, this.scrollY + 600.0f * Settings.scale);
            this.ironcladHb.move(300.0f * Settings.scale, this.scrollY - 1600.0f * Settings.scale);
            if (this.silentHb != null) {
                this.silentHb.move(300.0f * Settings.scale, this.scrollY - 2000.0f * Settings.scale);
            }
            if (this.defectHb != null) {
                this.defectHb.move(300.0f * Settings.scale, this.scrollY - 2400.0f * Settings.scale);
            }
            if (this.watcherHb != null) {
                this.watcherHb.move(300.0f * Settings.scale, this.scrollY - 2800.0f * Settings.scale);
            }
        }
    }

    public static void renderHeader(SpriteBatch sb, String text, float screenX, float renderY) {
        FontHelper.renderSmartText(sb, FontHelper.charTitleFont, text, screenX + 50.0f * Settings.scale, renderY + 850.0f * Settings.yScale, 9999.0f, 32.0f * Settings.scale, Settings.CREAM_COLOR);
    }

    public boolean statScreenUnlocked() {
        ArrayList<CharStat> allCharStats = CardCrawlGame.characterManager.getAllCharacterStats();
        for (CharStat cs : allCharStats) {
            if (cs.bossKilled <= 0 && cs.getDeathCount() <= 0) continue;
            return true;
        }
        return false;
    }

    public boolean dailiesUnlocked() {
        if (Settings.isDemo) {
            return false;
        }
        return AbstractDungeon.player.getCharStat().furthestAscent > 17;
    }

    public boolean trialsUnlocked() {
        return AbstractDungeon.player.getCharStat().getVictoryCount() > 0;
    }

    public int getTotalVictories() {
        ArrayList<CharStat> allCharStats = CardCrawlGame.characterManager.getAllCharacterStats();
        int victoryTotal = 0;
        for (CharStat cs : allCharStats) {
            victoryTotal += cs.getVictoryCount();
        }
        return victoryTotal;
    }

    @Override
    public void scrolledUsingBar(float newPercent) {
        this.scrollTargetY = this.scrollY = MathHelper.valueFromPercentBetween(this.scrollLowerBound, this.scrollUpperBound, newPercent);
        this.updateBarPosition();
    }

    private void updateBarPosition() {
        float percent = MathHelper.percentFromValueBetween(this.scrollLowerBound, this.scrollUpperBound, this.scrollY);
        this.scrollBar.parentScrolledToPercent(percent);
    }

    static {
        atlas = null;
    }
}

