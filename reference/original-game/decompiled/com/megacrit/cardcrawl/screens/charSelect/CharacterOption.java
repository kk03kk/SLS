/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.charSelect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class CharacterOption {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CharacterOption");
    public static final String[] TEXT = CharacterOption.uiStrings.TEXT;
    private Texture buttonImg;
    private Texture portraitImg;
    private String portraitUrl;
    public AbstractPlayer c;
    public boolean selected = false;
    public boolean locked = false;
    public Hitbox hb;
    private static final float HB_W = 150.0f * Settings.scale;
    private static final int BUTTON_W = 220;
    public static final String ASSETS_DIR = "images/ui/charSelect/";
    private static final Color BLACK_OUTLINE_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);
    private Color glowColor = new Color(1.0f, 0.8f, 0.2f, 0.0f);
    private static final int ICON_W = 64;
    private static final float DEST_INFO_X = Settings.isMobile ? 160.0f * Settings.scale : 200.0f * Settings.scale;
    private static final float START_INFO_X = -800.0f * Settings.xScale;
    private float infoX = START_INFO_X;
    private float infoY = (float)Settings.HEIGHT / 2.0f;
    public String name = "";
    private static final float NAME_OFFSET_Y = 200.0f * Settings.scale;
    private String hp;
    private int gold;
    private String flavorText;
    private CharSelectInfo charInfo;
    private int unlocksRemaining;
    private int maxAscensionLevel = 1;

    public CharacterOption(String optionName, AbstractPlayer c, Texture buttonImg, Texture portraitImg) {
        this.name = optionName;
        this.hb = new Hitbox(HB_W, HB_W);
        this.buttonImg = buttonImg;
        this.portraitImg = portraitImg;
        this.c = c;
        this.charInfo = null;
        this.charInfo = c.getLoadout();
        this.hp = this.charInfo.hp;
        this.gold = this.charInfo.gold;
        this.flavorText = this.charInfo.flavorText;
        this.unlocksRemaining = 5 - UnlockTracker.getUnlockLevel(c.chosenClass);
    }

    public CharacterOption(String optionName, AbstractPlayer c, String buttonUrl, String portraitImg) {
        this.name = optionName;
        this.hb = new Hitbox(HB_W, HB_W);
        this.buttonImg = ImageMaster.loadImage(ASSETS_DIR + buttonUrl);
        this.portraitUrl = c.getPortraitImageName();
        this.c = c;
        this.charInfo = null;
        this.charInfo = c.getLoadout();
        this.hp = this.charInfo.hp;
        this.gold = this.charInfo.gold;
        this.flavorText = this.charInfo.flavorText;
        this.unlocksRemaining = 5 - UnlockTracker.getUnlockLevel(c.chosenClass);
    }

    public CharacterOption(AbstractPlayer c) {
        this.hb = new Hitbox(HB_W, HB_W);
        this.buttonImg = ImageMaster.CHAR_SELECT_LOCKED;
        this.locked = true;
        this.c = c;
    }

    public void saveChosenAscensionLevel(int level) {
        Prefs pref = this.c.getPrefs();
        pref.putInteger("LAST_ASCENSION_LEVEL", level);
        pref.flush();
    }

    public void incrementAscensionLevel(int level) {
        if (level > this.maxAscensionLevel) {
            return;
        }
        this.saveChosenAscensionLevel(level);
        CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = level;
        CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString = CharacterSelectScreen.A_TEXT[level - 1];
    }

    public void decrementAscensionLevel(int level) {
        if (level == 0) {
            return;
        }
        this.saveChosenAscensionLevel(level);
        CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = level;
        CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString = CharacterSelectScreen.A_TEXT[level - 1];
    }

    public void update() {
        this.updateHitbox();
        this.updateInfoPosition();
    }

    private void updateHitbox() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.playA("UI_HOVER", -0.3f);
        }
        if (this.hb.hovered && this.locked) {
            if (this.c.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
                TipHelper.renderGenericTip((float)InputHelper.mX + 70.0f * Settings.xScale, (float)InputHelper.mY - 10.0f * Settings.scale, TEXT[0], TEXT[1]);
            } else if (this.c.chosenClass == AbstractPlayer.PlayerClass.DEFECT) {
                TipHelper.renderGenericTip((float)InputHelper.mX + 70.0f * Settings.xScale, (float)InputHelper.mY - 10.0f * Settings.scale, TEXT[0], TEXT[3]);
            } else if (this.c.chosenClass == AbstractPlayer.PlayerClass.WATCHER) {
                TipHelper.renderGenericTip((float)InputHelper.mX + 70.0f * Settings.xScale, (float)InputHelper.mY - 10.0f * Settings.scale, TEXT[0], TEXT[10]);
            }
        }
        if (InputHelper.justClickedLeft && !this.locked && this.hb.hovered) {
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.4f);
            this.hb.clickStarted = true;
        }
        if (this.hb.clicked) {
            this.hb.clicked = false;
            if (!this.selected) {
                CardCrawlGame.mainMenuScreen.charSelectScreen.deselectOtherOptions(this);
                this.selected = true;
                CardCrawlGame.mainMenuScreen.charSelectScreen.justSelected();
                CardCrawlGame.chosenCharacter = this.c.chosenClass;
                CardCrawlGame.mainMenuScreen.charSelectScreen.confirmButton.isDisabled = false;
                CardCrawlGame.mainMenuScreen.charSelectScreen.confirmButton.show();
                CardCrawlGame.mainMenuScreen.charSelectScreen.bgCharImg = this.portraitUrl != null ? ImageMaster.loadImage(ASSETS_DIR + this.portraitUrl) : this.portraitImg;
                Prefs pref = this.c.getPrefs();
                if (!this.locked) {
                    this.c.doCharSelectScreenSelectEffect();
                }
                if (pref != null) {
                    int ascensionLevel;
                    CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = pref.getInteger("LAST_ASCENSION_LEVEL", 1);
                    if (20 < CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel) {
                        CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = 20;
                    }
                    this.maxAscensionLevel = pref.getInteger("ASCENSION_LEVEL", 1);
                    if (20 < this.maxAscensionLevel) {
                        this.maxAscensionLevel = 20;
                    }
                    if ((ascensionLevel = CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel) > this.maxAscensionLevel) {
                        ascensionLevel = this.maxAscensionLevel;
                    }
                    CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString = ascensionLevel > 0 ? CharacterSelectScreen.A_TEXT[ascensionLevel - 1] : "";
                }
            }
        }
    }

    private void updateInfoPosition() {
        this.infoX = this.selected ? MathHelper.uiLerpSnap(this.infoX, DEST_INFO_X) : MathHelper.uiLerpSnap(this.infoX, START_INFO_X);
    }

    public void render(SpriteBatch sb) {
        this.renderOptionButton(sb);
        this.renderInfo(sb);
        this.hb.render(sb);
    }

    private void renderOptionButton(SpriteBatch sb) {
        if (this.selected) {
            this.glowColor.a = 0.25f + (MathUtils.cosDeg(System.currentTimeMillis() / 4L % 360L) + 1.25f) / 3.5f;
            sb.setColor(this.glowColor);
        } else {
            sb.setColor(BLACK_OUTLINE_COLOR);
        }
        sb.draw(ImageMaster.CHAR_OPT_HIGHLIGHT, this.hb.cX - 110.0f, this.hb.cY - 110.0f, 110.0f, 110.0f, 220.0f, 220.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 220, 220, false, false);
        if (this.selected || this.hb.hovered) {
            sb.setColor(Color.WHITE);
        } else {
            sb.setColor(Color.LIGHT_GRAY);
        }
        sb.draw(this.buttonImg, this.hb.cX - 110.0f, this.hb.cY - 110.0f, 110.0f, 110.0f, 220.0f, 220.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 220, 220, false, false);
    }

    private void renderInfo(SpriteBatch sb) {
        if (!this.name.equals("")) {
            if (!Settings.isMobile) {
                FontHelper.renderSmartText(sb, FontHelper.bannerNameFont, this.name, this.infoX - 35.0f * Settings.scale, this.infoY + NAME_OFFSET_Y, 99999.0f, 38.0f * Settings.scale, Settings.GOLD_COLOR);
                sb.draw(ImageMaster.TP_HP, this.infoX - 10.0f * Settings.scale - 32.0f, this.infoY + 95.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                FontHelper.renderSmartText(sb, FontHelper.tipHeaderFont, TEXT[4] + this.hp, this.infoX + 18.0f * Settings.scale, this.infoY + 102.0f * Settings.scale, 10000.0f, 10000.0f, Settings.RED_TEXT_COLOR);
                sb.draw(ImageMaster.TP_GOLD, this.infoX + 190.0f * Settings.scale - 32.0f, this.infoY + 95.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                FontHelper.renderSmartText(sb, FontHelper.tipHeaderFont, TEXT[5] + Integer.toString(this.gold), this.infoX + 220.0f * Settings.scale, this.infoY + 102.0f * Settings.scale, 10000.0f, 10000.0f, Settings.GOLD_COLOR);
                FontHelper.renderSmartText(sb, FontHelper.tipHeaderFont, this.flavorText, this.infoX - 26.0f * Settings.scale, this.infoY + 40.0f * Settings.scale, 10000.0f, 30.0f * Settings.scale, Settings.CREAM_COLOR);
                if (this.unlocksRemaining > 0) {
                    FontHelper.renderSmartText(sb, FontHelper.tipHeaderFont, Integer.toString(this.unlocksRemaining) + TEXT[6], this.infoX - 26.0f * Settings.scale, this.infoY - 112.0f * Settings.scale, 10000.0f, 10000.0f, Settings.CREAM_COLOR);
                    int unlockProgress = UnlockTracker.getCurrentProgress(this.c.chosenClass);
                    int unlockCost = UnlockTracker.getCurrentScoreCost(this.c.chosenClass);
                    FontHelper.renderSmartText(sb, FontHelper.tipHeaderFont, Integer.toString(unlockProgress) + "/" + unlockCost + TEXT[9], this.infoX - 26.0f * Settings.scale, this.infoY - 140.0f * Settings.scale, 10000.0f, 10000.0f, Settings.CREAM_COLOR);
                }
                this.renderRelics(sb);
            } else {
                FontHelper.renderSmartText(sb, FontHelper.bannerNameFont, this.name, this.infoX - 35.0f * Settings.scale, this.infoY + 350.0f * Settings.scale, 99999.0f, 38.0f * Settings.scale, Settings.GOLD_COLOR, 1.1f);
                sb.draw(ImageMaster.TP_HP, this.infoX - 10.0f * Settings.scale - 32.0f, this.infoY + 230.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, TEXT[4] + this.hp, this.infoX + 18.0f * Settings.scale, this.infoY + 243.0f * Settings.scale, 10000.0f, 10000.0f, Settings.RED_TEXT_COLOR, 0.8f);
                sb.draw(ImageMaster.TP_GOLD, this.infoX + 260.0f * Settings.scale - 32.0f, this.infoY + 230.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, TEXT[5] + Integer.toString(this.gold), this.infoX + 290.0f * Settings.scale, this.infoY + 243.0f * Settings.scale, 10000.0f, 10000.0f, Settings.GOLD_COLOR, 0.8f);
                if (this.selected) {
                    FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, this.flavorText, this.infoX - 26.0f * Settings.scale, this.infoY + 170.0f * Settings.scale, 10000.0f, 40.0f * Settings.scale, Settings.CREAM_COLOR, 0.9f);
                }
                if (this.unlocksRemaining > 0) {
                    FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, Integer.toString(this.unlocksRemaining) + TEXT[6], this.infoX - 26.0f * Settings.scale, this.infoY - 60.0f * Settings.scale, 10000.0f, 10000.0f, Settings.CREAM_COLOR, 0.8f);
                    int unlockProgress = UnlockTracker.getCurrentProgress(this.c.chosenClass);
                    int unlockCost = UnlockTracker.getCurrentScoreCost(this.c.chosenClass);
                    FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, Integer.toString(unlockProgress) + "/" + unlockCost + TEXT[9], this.infoX - 26.0f * Settings.scale, this.infoY - 100.0f * Settings.scale, 10000.0f, 10000.0f, Settings.CREAM_COLOR, 0.8f);
                }
                this.renderRelics(sb);
            }
        }
    }

    private void renderRelics(SpriteBatch sb) {
        block8: {
            block6: {
                block7: {
                    if (this.charInfo.relics.size() != 1) break block6;
                    if (Settings.isMobile) break block7;
                    sb.setColor(Settings.QUARTER_TRANSPARENT_BLACK_COLOR);
                    sb.draw(RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).outlineImg, this.infoX - 64.0f, this.infoY - 60.0f * Settings.scale - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 128, 128, false, false);
                    sb.setColor(Color.WHITE);
                    sb.draw(RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).img, this.infoX - 64.0f, this.infoY - 60.0f * Settings.scale - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 128, 128, false, false);
                    FontHelper.renderSmartText(sb, FontHelper.tipHeaderFont, RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).name, this.infoX + 44.0f * Settings.scale, this.infoY - 40.0f * Settings.scale, 10000.0f, 10000.0f, Settings.GOLD_COLOR);
                    String relicString = RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).description;
                    if (this.charInfo.name.equals(TEXT[7])) {
                        relicString = TEXT[8];
                    }
                    FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, relicString, this.infoX + 44.0f * Settings.scale, this.infoY - 66.0f * Settings.scale, 10000.0f, 10000.0f, Settings.CREAM_COLOR);
                    break block8;
                }
                sb.setColor(Settings.QUARTER_TRANSPARENT_BLACK_COLOR);
                sb.draw(RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).outlineImg, this.infoX - 64.0f, this.infoY + 30.0f * Settings.scale - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 1.4f, Settings.scale * 1.4f, 0.0f, 0, 0, 128, 128, false, false);
                sb.setColor(Color.WHITE);
                sb.draw(RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).img, this.infoX - 64.0f, this.infoY + 30.0f * Settings.scale - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 1.4f, Settings.scale * 1.4f, 0.0f, 0, 0, 128, 128, false, false);
                FontHelper.renderSmartText(sb, FontHelper.topPanelInfoFont, RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).name, this.infoX + 60.0f * Settings.scale, this.infoY + 60.0f * Settings.scale, 10000.0f, 10000.0f, Settings.GOLD_COLOR);
                String relicString = RelicLibrary.getRelic((String)this.charInfo.relics.get((int)0)).description;
                if (this.charInfo.name.equals(TEXT[7])) {
                    relicString = TEXT[8];
                }
                if (!this.selected) break block8;
                FontHelper.renderSmartText(sb, FontHelper.topPanelInfoFont, relicString, this.infoX + 60.0f * Settings.scale, this.infoY + 24.0f * Settings.scale, 10000.0f, 10000.0f, Settings.CREAM_COLOR);
                break block8;
            }
            for (int i = 0; i < this.charInfo.relics.size(); ++i) {
                AbstractRelic r = RelicLibrary.getRelic(this.charInfo.relics.get(i));
                r.updateDescription(this.charInfo.player.chosenClass);
                Hitbox relicHitbox = new Hitbox(80.0f * Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), 80.0f * Settings.scale);
                relicHitbox.move(this.infoX + (float)i * 72.0f * Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), this.infoY - 60.0f * Settings.scale);
                relicHitbox.render(sb);
                relicHitbox.update();
                if (relicHitbox.hovered) {
                    if ((float)InputHelper.mX < 1400.0f * Settings.scale) {
                        TipHelper.queuePowerTips((float)InputHelper.mX + 60.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, r.tips);
                    } else {
                        TipHelper.queuePowerTips((float)InputHelper.mX - 350.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, r.tips);
                    }
                }
                sb.setColor(new Color(0.0f, 0.0f, 0.0f, 0.25f));
                sb.draw(r.outlineImg, this.infoX - 64.0f + (float)i * 72.0f * Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), this.infoY - 60.0f * Settings.scale - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), 0.0f, 0, 0, 128, 128, false, false);
                sb.setColor(Color.WHITE);
                sb.draw(r.img, this.infoX - 64.0f + (float)i * 72.0f * Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), this.infoY - 60.0f * Settings.scale - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), Settings.scale * (0.01f + (1.0f - 0.019f * (float)this.charInfo.relics.size())), 0.0f, 0, 0, 128, 128, false, false);
            }
        }
    }
}

