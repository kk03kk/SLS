/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TipHelper {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TipHelper");
    public static final String[] TEXT = TipHelper.uiStrings.TEXT;
    private static final Logger logger = LogManager.getLogger(TipHelper.class.getName());
    private static boolean renderedTipThisFrame = false;
    private static boolean isCard = false;
    private static float drawX;
    private static float drawY;
    private static ArrayList<String> KEYWORDS;
    private static ArrayList<PowerTip> POWER_TIPS;
    private static String HEADER;
    private static String BODY;
    private static AbstractCard card;
    private static final Color BASE_COLOR;
    private static final float CARD_TIP_PAD;
    private static final float SHADOW_DIST_Y;
    private static final float SHADOW_DIST_X;
    private static final float BOX_EDGE_H;
    private static final float BOX_BODY_H;
    private static final float BOX_W;
    private static GlyphLayout gl;
    private static float textHeight;
    private static final float TEXT_OFFSET_X;
    private static final float HEADER_OFFSET_Y;
    private static final float ORB_OFFSET_Y;
    private static final float BODY_OFFSET_Y;
    private static final float BODY_TEXT_WIDTH;
    private static final float TIP_DESC_LINE_SPACING;
    private static final float POWER_ICON_OFFSET_X;

    public static void render(SpriteBatch sb) {
        if (!Settings.hidePopupDetails && renderedTipThisFrame) {
            if (AbstractDungeon.player != null && (AbstractDungeon.player.inSingleTargetMode || AbstractDungeon.player.isDraggingCard && !Settings.isTouchScreen)) {
                HEADER = null;
                BODY = null;
                card = null;
                renderedTipThisFrame = false;
                return;
            }
            if (Settings.isTouchScreen && AbstractDungeon.player != null && AbstractDungeon.player.isHoveringDropZone) {
                HEADER = null;
                BODY = null;
                card = null;
                renderedTipThisFrame = false;
                return;
            }
            if (isCard && card != null) {
                if (TipHelper.card.current_x > (float)Settings.WIDTH * 0.75f) {
                    TipHelper.renderKeywords(TipHelper.card.current_x - AbstractCard.IMG_WIDTH / 2.0f - CARD_TIP_PAD - BOX_W, TipHelper.card.current_y + AbstractCard.IMG_HEIGHT / 2.0f - BOX_EDGE_H, sb, KEYWORDS);
                } else {
                    TipHelper.renderKeywords(TipHelper.card.current_x + AbstractCard.IMG_WIDTH / 2.0f + CARD_TIP_PAD, TipHelper.card.current_y + AbstractCard.IMG_HEIGHT / 2.0f - BOX_EDGE_H, sb, KEYWORDS);
                }
                card = null;
                isCard = false;
            } else if (HEADER != null) {
                textHeight = -FontHelper.getSmartHeight(FontHelper.tipBodyFont, BODY, BODY_TEXT_WIDTH, TIP_DESC_LINE_SPACING) - 7.0f * Settings.scale;
                TipHelper.renderTipBox(drawX, drawY, sb, HEADER, BODY);
                HEADER = null;
            } else {
                TipHelper.renderPowerTips(drawX, drawY, sb, POWER_TIPS);
            }
            renderedTipThisFrame = false;
        }
    }

    public static void renderGenericTip(float x, float y, String header, String body) {
        if (!Settings.hidePopupDetails) {
            if (!renderedTipThisFrame) {
                renderedTipThisFrame = true;
                HEADER = header;
                BODY = body;
                drawX = x;
                drawY = y;
            } else if (HEADER == null && !KEYWORDS.isEmpty()) {
                logger.info("! " + KEYWORDS.get(0));
            }
        }
    }

    public static void queuePowerTips(float x, float y, ArrayList<PowerTip> powerTips) {
        if (!renderedTipThisFrame) {
            renderedTipThisFrame = true;
            drawX = x;
            drawY = y;
            POWER_TIPS = powerTips;
        } else if (HEADER == null && !KEYWORDS.isEmpty()) {
            logger.info("! " + KEYWORDS.get(0));
        }
    }

    public static void renderTipForCard(AbstractCard c, SpriteBatch sb, ArrayList<String> keywords) {
        if (!renderedTipThisFrame) {
            isCard = true;
            card = c;
            TipHelper.convertToReadable(keywords);
            KEYWORDS = keywords;
            renderedTipThisFrame = true;
        }
    }

    private static void convertToReadable(ArrayList<String> keywords) {
        ArrayList add = new ArrayList();
        keywords.addAll(add);
    }

    private static void renderPowerTips(float x, float y, SpriteBatch sb, ArrayList<PowerTip> powerTips) {
        float originalY = y;
        boolean offsetLeft = false;
        if (x > (float)Settings.WIDTH / 2.0f) {
            offsetLeft = true;
        }
        float offset = 0.0f;
        for (PowerTip tip : powerTips) {
            textHeight = TipHelper.getPowerTipHeight(tip);
            float offsetChange = textHeight + BOX_EDGE_H * 3.15f;
            if (offset + offsetChange >= (float)Settings.HEIGHT * 0.7f) {
                y = originalY;
                offset = 0.0f;
                x = offsetLeft ? (x -= 324.0f * Settings.scale) : (x += 324.0f * Settings.scale);
            }
            TipHelper.renderTipBox(x, y, sb, tip.header, tip.body);
            gl.setText(FontHelper.tipHeaderFont, tip.header, Color.WHITE, 0.0f, -1, false);
            if (tip.img != null) {
                sb.setColor(Color.WHITE);
                sb.draw(tip.img, x + TEXT_OFFSET_X + TipHelper.gl.width + 5.0f * Settings.scale, y - 10.0f * Settings.scale, 32.0f * Settings.scale, 32.0f * Settings.scale);
            } else if (tip.imgRegion != null) {
                sb.setColor(Color.WHITE);
                sb.draw(tip.imgRegion, x + TipHelper.gl.width + POWER_ICON_OFFSET_X - (float)tip.imgRegion.packedWidth / 2.0f, y + 5.0f * Settings.scale - (float)tip.imgRegion.packedHeight / 2.0f, (float)tip.imgRegion.packedWidth / 2.0f, (float)tip.imgRegion.packedHeight / 2.0f, tip.imgRegion.packedWidth, tip.imgRegion.packedHeight, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f);
            }
            y -= offsetChange;
            offset += offsetChange;
        }
    }

    private static float getPowerTipHeight(PowerTip powerTip) {
        return -FontHelper.getSmartHeight(FontHelper.tipBodyFont, powerTip.body, BODY_TEXT_WIDTH, TIP_DESC_LINE_SPACING) - 7.0f * Settings.scale;
    }

    public static float calculateAdditionalOffset(ArrayList<PowerTip> powerTips, float hBcY) {
        if (powerTips.isEmpty()) {
            return 0.0f;
        }
        return (1.0f - hBcY / (float)Settings.HEIGHT) * TipHelper.getTallestOffset(powerTips) - (TipHelper.getPowerTipHeight(powerTips.get(0)) + BOX_EDGE_H * 3.15f) / 2.0f;
    }

    public static float calculateToAvoidOffscreen(ArrayList<PowerTip> powerTips, float hBcY) {
        if (powerTips.isEmpty()) {
            return 0.0f;
        }
        return Math.max(0.0f, TipHelper.getTallestOffset(powerTips) - hBcY);
    }

    private static float getTallestOffset(ArrayList<PowerTip> powerTips) {
        float currentOffset = 0.0f;
        float maxOffset = 0.0f;
        for (PowerTip p : powerTips) {
            float offsetChange = TipHelper.getPowerTipHeight(p) + BOX_EDGE_H * 3.15f;
            if (currentOffset + offsetChange >= (float)Settings.HEIGHT * 0.7f) {
                currentOffset = 0.0f;
            }
            if (!((currentOffset += offsetChange) > maxOffset)) continue;
            maxOffset = currentOffset;
        }
        return maxOffset;
    }

    private static void renderKeywords(float x, float y, SpriteBatch sb, ArrayList<String> keywords) {
        if (keywords.size() >= 4) {
            y += (float)(keywords.size() - 1) * 62.0f * Settings.scale;
        }
        for (String s : keywords) {
            if (!GameDictionary.keywords.containsKey(s)) {
                logger.info("MISSING: " + s + " in Dictionary!");
                continue;
            }
            textHeight = -FontHelper.getSmartHeight(FontHelper.tipBodyFont, GameDictionary.keywords.get(s), BODY_TEXT_WIDTH, TIP_DESC_LINE_SPACING) - 7.0f * Settings.scale;
            TipHelper.renderBox(sb, s, x, y);
            y -= textHeight + BOX_EDGE_H * 3.15f;
        }
    }

    private static void renderTipBox(float x, float y, SpriteBatch sb, String title, String description) {
        float h = textHeight;
        sb.setColor(Settings.TOP_PANEL_SHADOW_COLOR);
        sb.draw(ImageMaster.KEYWORD_TOP, x + SHADOW_DIST_X, y - SHADOW_DIST_Y, BOX_W, BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BODY, x + SHADOW_DIST_X, y - h - BOX_EDGE_H - SHADOW_DIST_Y, BOX_W, h + BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BOT, x + SHADOW_DIST_X, y - h - BOX_BODY_H - SHADOW_DIST_Y, BOX_W, BOX_EDGE_H);
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.KEYWORD_TOP, x, y, BOX_W, BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BODY, x, y - h - BOX_EDGE_H, BOX_W, h + BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BOT, x, y - h - BOX_BODY_H, BOX_W, BOX_EDGE_H);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipHeaderFont, title, x + TEXT_OFFSET_X, y + HEADER_OFFSET_Y, Settings.GOLD_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, description, x + TEXT_OFFSET_X, y + BODY_OFFSET_Y, BODY_TEXT_WIDTH, TIP_DESC_LINE_SPACING, BASE_COLOR);
    }

    public static void renderTipEnergy(SpriteBatch sb, TextureAtlas.AtlasRegion region, float x, float y) {
        sb.setColor(Color.WHITE);
        sb.draw(region.getTexture(), x + region.offsetX * Settings.scale, y + region.offsetY * Settings.scale, 0.0f, 0.0f, region.packedWidth, region.packedHeight, Settings.scale, Settings.scale, 0.0f, region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight(), false, false);
    }

    private static void renderBox(SpriteBatch sb, String word, float x, float y) {
        TextureAtlas.AtlasRegion currentOrb;
        float h = textHeight;
        sb.setColor(Settings.TOP_PANEL_SHADOW_COLOR);
        sb.draw(ImageMaster.KEYWORD_TOP, x + SHADOW_DIST_X, y - SHADOW_DIST_Y, BOX_W, BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BODY, x + SHADOW_DIST_X, y - h - BOX_EDGE_H - SHADOW_DIST_Y, BOX_W, h + BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BOT, x + SHADOW_DIST_X, y - h - BOX_BODY_H - SHADOW_DIST_Y, BOX_W, BOX_EDGE_H);
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.KEYWORD_TOP, x, y, BOX_W, BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BODY, x, y - h - BOX_EDGE_H, BOX_W, h + BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BOT, x, y - h - BOX_BODY_H, BOX_W, BOX_EDGE_H);
        TextureAtlas.AtlasRegion atlasRegion = currentOrb = AbstractDungeon.player != null ? AbstractDungeon.player.getOrb() : AbstractCard.orb_red;
        if (word.equals("[R]") || word.equals("[G]") || word.equals("[B]") || word.equals("[W]") || word.equals("[E]")) {
            if (word.equals("[R]")) {
                TipHelper.renderTipEnergy(sb, AbstractCard.orb_red, x + TEXT_OFFSET_X, y + ORB_OFFSET_Y);
            } else if (word.equals("[G]")) {
                TipHelper.renderTipEnergy(sb, AbstractCard.orb_green, x + TEXT_OFFSET_X, y + ORB_OFFSET_Y);
            } else if (word.equals("[B]")) {
                TipHelper.renderTipEnergy(sb, AbstractCard.orb_blue, x + TEXT_OFFSET_X, y + ORB_OFFSET_Y);
            } else if (word.equals("[W]")) {
                TipHelper.renderTipEnergy(sb, AbstractCard.orb_purple, x + TEXT_OFFSET_X, y + ORB_OFFSET_Y);
            } else if (word.equals("[E]")) {
                TipHelper.renderTipEnergy(sb, currentOrb, x + TEXT_OFFSET_X, y + ORB_OFFSET_Y);
            }
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipHeaderFont, TipHelper.capitalize(TEXT[0]), x + TEXT_OFFSET_X * 2.5f, y + HEADER_OFFSET_Y, Settings.GOLD_COLOR);
        } else {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipHeaderFont, TipHelper.capitalize(word), x + TEXT_OFFSET_X, y + HEADER_OFFSET_Y, Settings.GOLD_COLOR);
        }
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, GameDictionary.keywords.get(word), x + TEXT_OFFSET_X, y + BODY_OFFSET_Y, BODY_TEXT_WIDTH, TIP_DESC_LINE_SPACING, BASE_COLOR);
    }

    public static String capitalize(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); ++i) {
            char tmp = input.charAt(i);
            tmp = i == 0 ? Character.toUpperCase(tmp) : Character.toLowerCase(tmp);
            sb.append(tmp);
        }
        return sb.toString();
    }

    static {
        KEYWORDS = new ArrayList();
        POWER_TIPS = new ArrayList();
        HEADER = null;
        BODY = null;
        BASE_COLOR = new Color(1.0f, 0.9725f, 0.8745f, 1.0f);
        CARD_TIP_PAD = 12.0f * Settings.scale;
        SHADOW_DIST_Y = 14.0f * Settings.scale;
        SHADOW_DIST_X = 9.0f * Settings.scale;
        BOX_EDGE_H = 32.0f * Settings.scale;
        BOX_BODY_H = 64.0f * Settings.scale;
        BOX_W = 320.0f * Settings.scale;
        gl = new GlyphLayout();
        TEXT_OFFSET_X = 22.0f * Settings.scale;
        HEADER_OFFSET_Y = 12.0f * Settings.scale;
        ORB_OFFSET_Y = -8.0f * Settings.scale;
        BODY_OFFSET_Y = -20.0f * Settings.scale;
        BODY_TEXT_WIDTH = 280.0f * Settings.scale;
        TIP_DESC_LINE_SPACING = 26.0f * Settings.scale;
        POWER_ICON_OFFSET_X = 40.0f * Settings.scale;
    }
}

