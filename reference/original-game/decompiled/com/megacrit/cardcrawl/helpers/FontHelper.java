/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FontHelper {
    private static final Logger logger = LogManager.getLogger(FontHelper.class.getName());
    private static FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
    private static FreeTypeFontGenerator.FreeTypeBitmapFontData data = new FreeTypeFontGenerator.FreeTypeBitmapFontData();
    private static HashMap<String, FreeTypeFontGenerator> generators = new HashMap();
    private static FileHandle fontFile = null;
    private static float fontScale = 1.0f;
    private static Vector2 rotatedTextTmp = new Vector2(0.0f, 0.0f);
    private static Matrix4 rotatedTextMatrix = new Matrix4();
    private static final String TINY_NUMBERS_FONT = "font/04b03.ttf";
    private static final String ENG_DEFAULT_FONT = "font/Kreon-Regular.ttf";
    private static final String ENG_BOLD_FONT = "font/Kreon-Bold.ttf";
    private static final String ENG_ITALIC_FONT = "font/ZillaSlab-RegularItalic.otf";
    private static final String ENG_DRAMATIC_FONT = "font/FeDPrm27C.otf";
    private static final String ZHS_DEFAULT_FONT = "font/zhs/NotoSansMonoCJKsc-Regular.otf";
    private static final String ZHS_BOLD_FONT = "font/zhs/SourceHanSerifSC-Bold.otf";
    private static final String ZHS_ITALIC_FONT = "font/zhs/SourceHanSerifSC-Medium.otf";
    private static final String ZHT_DEFAULT_FONT = "font/zht/NotoSansCJKtc-Regular.otf";
    private static final String ZHT_BOLD_FONT = "font/zht/NotoSansCJKtc-Bold.otf";
    private static final String ZHT_ITALIC_FONT = "font/zht/NotoSansCJKtc-Medium.otf";
    private static final String EPO_DEFAULT_FONT = "font/epo/Andada-Regular.otf";
    private static final String EPO_BOLD_FONT = "font/epo/Andada-Bold.otf";
    private static final String EPO_ITALIC_FONT = "font/epo/Andada-Italic.otf";
    private static final String GRE_DEFAULT_FONT = "font/gre/Roboto-Regular.ttf";
    private static final String GRE_BOLD_FONT = "font/gre/Roboto-Bold.ttf";
    private static final String GRE_ITALIC_FONT = "font/gre/Roboto-Italic.ttf";
    private static final String JPN_DEFAULT_FONT = "font/jpn/NotoSansCJKjp-Regular.otf";
    private static final String JPN_BOLD_FONT = "font/jpn/NotoSansCJKjp-Bold.otf";
    private static final String JPN_ITALIC_FONT = "font/jpn/NotoSansCJKjp-Medium.otf";
    private static final String KOR_DEFAULT_FONT = "font/kor/GyeonggiCheonnyeonBatangBold.ttf";
    private static final String KOR_BOLD_FONT = "font/kor/GyeonggiCheonnyeonBatangBold.ttf";
    private static final String KOR_ITALIC_FONT = "font/kor/GyeonggiCheonnyeonBatangBold.ttf";
    private static final String RUS_DEFAULT_FONT = "font/rus/FiraSansExtraCondensed-Regular.ttf";
    private static final String RUS_BOLD_FONT = "font/rus/FiraSansExtraCondensed-Bold.ttf";
    private static final String RUS_ITALIC_FONT = "font/rus/FiraSansExtraCondensed-Italic.ttf";
    private static final String SRB_DEFAULT_FONT = "font/srb/InfluBG.otf";
    private static final String SRB_BOLD_FONT = "font/srb/InfluBG-Bold.otf";
    private static final String SRB_ITALIC_FONT = "font/srb/InfluBG-Italic.otf";
    private static final String THA_DEFAULT_FONT = "font/tha/CSChatThaiUI.ttf";
    private static final String THA_BOLD_FONT = "font/tha/CSChatThaiUI.ttf";
    private static final String THA_ITALIC_FONT = "font/tha/CSChatThaiUI.ttf";
    private static final String VIE_DEFAULT_FONT = "font/vie/Grenze-Regular.ttf";
    private static final String VIE_BOLD_FONT = "font/vie/Grenze-SemiBold.ttf";
    private static final String VIE_DRAMATIC_FONT = "font/vie/Grenze-Black.ttf";
    private static final String VIE_ITALIC_FONT = "font/vie/Grenze-RegularItalic.ttf";
    public static BitmapFont charDescFont;
    public static BitmapFont charTitleFont;
    public static BitmapFont cardTitleFont;
    public static BitmapFont cardTypeFont;
    public static BitmapFont cardEnergyFont_L;
    public static BitmapFont cardDescFont_N;
    public static BitmapFont cardDescFont_L;
    public static BitmapFont SCP_cardDescFont;
    public static BitmapFont SCP_cardEnergyFont;
    public static BitmapFont SCP_cardTitleFont_small;
    public static BitmapFont SRV_quoteFont;
    public static BitmapFont losePowerFont;
    public static BitmapFont energyNumFontRed;
    public static BitmapFont energyNumFontGreen;
    public static BitmapFont energyNumFontBlue;
    public static BitmapFont energyNumFontPurple;
    public static BitmapFont turnNumFont;
    public static BitmapFont damageNumberFont;
    public static BitmapFont buttonLabelFont;
    public static BitmapFont dungeonTitleFont;
    public static BitmapFont bannerNameFont;
    private static final float CARD_ENERGY_IMG_WIDTH;
    public static BitmapFont topPanelAmountFont;
    public static BitmapFont powerAmountFont;
    public static BitmapFont panelNameFont;
    public static BitmapFont healthInfoFont;
    public static BitmapFont blockInfoFont;
    public static BitmapFont topPanelInfoFont;
    public static BitmapFont tipHeaderFont;
    public static BitmapFont tipBodyFont;
    public static BitmapFont panelEndTurnFont;
    public static BitmapFont largeDialogOptionFont;
    public static BitmapFont smallDialogOptionFont;
    public static BitmapFont largeCardFont;
    public static BitmapFont menuBannerFont;
    public static BitmapFont leaderboardFont;
    public static GlyphLayout layout;
    private static TextureAtlas.AtlasRegion orb;
    private static Color color;
    private static float curWidth;
    private static float curHeight;
    private static float spaceWidth;
    private static int currentLine;
    private static Matrix4 mx4;
    private static StringBuilder newMsg;
    private static final int TIP_OFFSET_X = 50;
    private static final float TIP_PADDING = 8.0f;

    public static void initialize() {
        long startTime = System.currentTimeMillis();
        generators.clear();
        HashMap paramCreator = new HashMap();
        switch (Settings.language) {
            case ZHS: {
                fontFile = Gdx.files.internal(ZHS_DEFAULT_FONT);
                break;
            }
            case ZHT: {
                fontFile = Gdx.files.internal(ZHT_DEFAULT_FONT);
                break;
            }
            case EPO: {
                fontFile = Gdx.files.internal(EPO_DEFAULT_FONT);
                break;
            }
            case GRE: {
                fontFile = Gdx.files.internal(GRE_DEFAULT_FONT);
                break;
            }
            case JPN: {
                fontFile = Gdx.files.internal(JPN_DEFAULT_FONT);
                break;
            }
            case KOR: {
                fontFile = Gdx.files.internal("font/kor/GyeonggiCheonnyeonBatangBold.ttf");
                break;
            }
            case POL: 
            case RUS: 
            case UKR: {
                fontFile = Gdx.files.internal(RUS_DEFAULT_FONT);
                break;
            }
            case SRP: 
            case SRB: {
                fontFile = Gdx.files.internal(SRB_DEFAULT_FONT);
                break;
            }
            case THA: {
                fontFile = Gdx.files.internal("font/tha/CSChatThaiUI.ttf");
                fontScale = 0.95f;
                break;
            }
            case VIE: {
                fontFile = Gdx.files.internal(VIE_DEFAULT_FONT);
                break;
            }
            default: {
                fontFile = Gdx.files.internal(ENG_DEFAULT_FONT);
            }
        }
        FontHelper.data.xChars = new char[]{'\u52a8'};
        FontHelper.data.capChars = new char[]{'\u52a8'};
        FontHelper.param.hinting = FreeTypeFontGenerator.Hinting.Slight;
        FontHelper.param.spaceX = 0;
        FontHelper.param.kerning = true;
        paramCreator.clear();
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.gamma = 0.9f;
        FontHelper.param.borderGamma = 0.9f;
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        FontHelper.param.shadowOffsetX = (int)(4.0f * Settings.scale);
        charDescFont = Settings.isMobile ? FontHelper.prepFont(31.0f, false) : FontHelper.prepFont(30.0f, false);
        FontHelper.param.gamma = 1.8f;
        FontHelper.param.borderGamma = 1.8f;
        FontHelper.param.shadowOffsetX = (int)(6.0f * Settings.scale);
        charTitleFont = FontHelper.prepFont(44.0f, false);
        FontHelper.param.gamma = 0.9f;
        FontHelper.param.borderGamma = 0.9f;
        FontHelper.param.shadowOffsetX = Math.round(3.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(3.0f * Settings.scale);
        FontHelper.param.borderStraight = false;
        FontHelper.param.shadowColor = new Color(0.0f, 0.0f, 0.0f, 0.25f);
        FontHelper.param.borderColor = new Color(0.35f, 0.35f, 0.35f, 1.0f);
        FontHelper.param.borderWidth = 2.0f * Settings.scale;
        cardTitleFont = FontHelper.prepFont(27.0f, true);
        FontHelper.param.borderWidth = 2.25f * Settings.scale;
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowOffsetX = 1;
        FontHelper.param.shadowOffsetY = 1;
        FontHelper.param.spaceX = 0;
        cardDescFont_N = FontHelper.prepFont(24.0f, false);
        cardDescFont_L = FontHelper.prepFont(24.0f, true);
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        FontHelper.param.shadowOffsetX = Math.round(4.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(3.0f * Settings.scale);
        SCP_cardDescFont = FontHelper.prepFont(48.0f, true);
        FontHelper.param.shadowOffsetX = Math.round(6.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(6.0f * Settings.scale);
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        FontHelper.param.borderColor = new Color(0.35f, 0.35f, 0.35f, 1.0f);
        FontHelper.param.borderWidth = 4.0f * Settings.scale;
        SCP_cardTitleFont_small = FontHelper.prepFont(46.0f, true);
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        FontHelper.param.shadowOffsetX = Math.round(3.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(3.0f * Settings.scale);
        panelNameFont = FontHelper.prepFont(34.0f, true);
        FontHelper.param.shadowOffsetX = (int)(3.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = (int)(3.0f * Settings.scale);
        FontHelper.param.borderColor = new Color(0.67f, 0.06f, 0.22f, 1.0f);
        FontHelper.param.gamma = 0.9f;
        FontHelper.param.borderGamma = 0.9f;
        FontHelper.param.borderColor = new Color(0.4f, 0.1f, 0.1f, 1.0f);
        FontHelper.param.borderWidth = 0.0f;
        tipBodyFont = FontHelper.prepFont(22.0f, false);
        FontHelper.param.borderColor = new Color(0.1f, 0.1f, 0.1f, 0.5f);
        FontHelper.param.borderWidth = 2.0f * Settings.scale;
        topPanelAmountFont = FontHelper.prepFont(24.0f, false);
        FontHelper.param.borderColor = Color.valueOf("42514dff");
        FontHelper.param.shadowOffsetX = (int)(4.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = (int)(4.0f * Settings.scale);
        FontHelper.param.borderWidth = 3.0f * Settings.scale;
        panelEndTurnFont = FontHelper.prepFont(26.0f, false);
        FontHelper.param.spaceX = 0;
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowOffsetX = (int)(3.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = (int)(3.0f * Settings.scale);
        largeDialogOptionFont = FontHelper.prepFont(30.0f, false);
        FontHelper.largeDialogOptionFont.getData().markupEnabled = false;
        smallDialogOptionFont = FontHelper.prepFont(26.0f, false);
        FontHelper.smallDialogOptionFont.getData().markupEnabled = false;
        FontHelper.param.shadowOffsetX = 0;
        FontHelper.param.shadowOffsetY = 0;
        turnNumFont = FontHelper.prepFont(32.0f, true);
        FontHelper.param.borderWidth = 4.0f * Settings.scale;
        FontHelper.param.borderColor = new Color(0.3f, 0.3f, 0.3f, 1.0f);
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        FontHelper.param.shadowOffsetX = Math.round(3.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(3.0f * Settings.scale);
        losePowerFont = FontHelper.prepFont(36.0f, true);
        FontHelper.param.borderWidth = 3.0f * Settings.scale;
        FontHelper.param.borderColor = Color.DARK_GRAY;
        damageNumberFont = FontHelper.prepFont(48.0f, true);
        damageNumberFont.getData().setLineHeight(FontHelper.damageNumberFont.getData().lineHeight * 0.85f);
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.borderWidth = 2.7f * Settings.scale;
        FontHelper.param.shadowOffsetX = (int)(3.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = (int)(3.0f * Settings.scale);
        FontHelper.param.borderColor = new Color(0.45f, 0.1f, 0.12f, 1.0f);
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        healthInfoFont = Settings.isMobile ? FontHelper.prepFont(29.0f, false) : FontHelper.prepFont(22.0f, false);
        FontHelper.param.borderWidth = 4.0f * Settings.scale;
        FontHelper.param.spaceX = (int)(-2.5f * Settings.scale);
        FontHelper.param.borderColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        buttonLabelFont = Settings.isMobile ? FontHelper.prepFont(37.0f, true) : FontHelper.prepFont(32.0f, true);
        FontHelper.param.spaceX = 0;
        fontScale = 1.0f;
        fontFile = Gdx.files.internal(ENG_BOLD_FONT);
        FontHelper.param.borderStraight = true;
        FontHelper.param.borderWidth = 4.0f * Settings.scale;
        FontHelper.param.borderColor = new Color(0.4f, 0.15f, 0.15f, 1.0f);
        energyNumFontRed = FontHelper.prepFont(36.0f, true);
        FontHelper.param.borderColor = new Color(0.15f, 0.4f, 0.15f, 1.0f);
        energyNumFontGreen = FontHelper.prepFont(36.0f, true);
        FontHelper.param.borderColor = new Color(0.1f, 0.2f, 0.4f, 1.0f);
        energyNumFontBlue = FontHelper.prepFont(36.0f, true);
        FontHelper.param.borderColor = new Color(1595767551);
        energyNumFontPurple = FontHelper.prepFont(36.0f, true);
        FontHelper.param.borderWidth = 4.0f * Settings.scale;
        FontHelper.param.borderColor = new Color(0.3f, 0.3f, 0.3f, 1.0f);
        cardEnergyFont_L = FontHelper.prepFont(38.0f, true);
        FontHelper.param.borderWidth = 8.0f * Settings.scale;
        SCP_cardEnergyFont = FontHelper.prepFont(76.0f, true);
        FontHelper.param.shadowOffsetX = (int)(2.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = (int)(2.0f * Settings.scale);
        FontHelper.param.borderColor = new Color(0.0f, 0.33f, 0.2f, 0.8f);
        FontHelper.param.borderWidth = 2.7f * Settings.scale;
        FontHelper.param.spaceX = (int)(-0.9f * Settings.scale);
        blockInfoFont = Settings.isMobile ? FontHelper.prepFont(30.0f, false) : FontHelper.prepFont(24.0f, false);
        switch (Settings.language) {
            case ZHS: {
                fontFile = Gdx.files.internal(ZHS_BOLD_FONT);
                break;
            }
            case ZHT: {
                fontFile = Gdx.files.internal(ZHT_BOLD_FONT);
                break;
            }
            case EPO: {
                fontFile = Gdx.files.internal(EPO_BOLD_FONT);
                break;
            }
            case GRE: {
                fontFile = Gdx.files.internal(GRE_BOLD_FONT);
                break;
            }
            case JPN: {
                fontFile = Gdx.files.internal(JPN_BOLD_FONT);
                break;
            }
            case KOR: {
                fontFile = Gdx.files.internal("font/kor/GyeonggiCheonnyeonBatangBold.ttf");
                break;
            }
            case POL: 
            case RUS: 
            case UKR: {
                fontFile = Gdx.files.internal(RUS_BOLD_FONT);
                break;
            }
            case SRP: 
            case SRB: {
                fontFile = Gdx.files.internal(SRB_BOLD_FONT);
                break;
            }
            case THA: {
                fontScale = 0.95f;
                fontFile = Gdx.files.internal("font/tha/CSChatThaiUI.ttf");
                break;
            }
            case VIE: {
                fontFile = Gdx.files.internal(VIE_BOLD_FONT);
                break;
            }
            default: {
                fontFile = Gdx.files.internal(ENG_BOLD_FONT);
            }
        }
        FontHelper.param.gamma = 1.2f;
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowOffsetX = 0;
        FontHelper.param.shadowOffsetY = 0;
        if (Settings.WIDTH >= 1600) {
            FontHelper.param.spaceX = -1;
        }
        cardTypeFont = FontHelper.prepFont(17.0f, true);
        FontHelper.param.gamma = 1.2f;
        FontHelper.param.borderGamma = 1.2f;
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowColor = new Color(0.0f, 0.0f, 0.0f, 0.12f);
        FontHelper.param.shadowOffsetX = (int)(5.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = (int)(4.0f * Settings.scale);
        menuBannerFont = FontHelper.prepFont(38.0f, true);
        FontHelper.param.characters = "?";
        FontHelper.param.shadowOffsetX = (int)(15.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = (int)(12.0f * Settings.scale);
        largeCardFont = FontHelper.prepFont(120.0f, true);
        FontHelper.param.shadowOffsetX = 2;
        FontHelper.param.shadowOffsetY = 2;
        FontHelper.param.shadowColor = new Color(0.0f, 0.0f, 0.0f, 0.33f);
        FontHelper.param.gamma = 2.0f;
        FontHelper.param.borderGamma = 2.0f;
        FontHelper.param.borderStraight = true;
        FontHelper.param.borderColor = Color.DARK_GRAY;
        FontHelper.param.borderWidth = 2.0f * Settings.scale;
        FontHelper.param.shadowOffsetX = 1;
        FontHelper.param.shadowOffsetY = 1;
        tipHeaderFont = FontHelper.prepFont(23.0f, false);
        FontHelper.param.shadowOffsetX = 2;
        FontHelper.param.shadowOffsetY = 2;
        topPanelInfoFont = FontHelper.prepFont(26.0f, false);
        FontHelper.param.spaceX = 0;
        FontHelper.param.gamma = 0.9f;
        FontHelper.param.borderGamma = 0.9f;
        FontHelper.param.borderWidth = 0.0f;
        fontScale = 1.0f;
        fontFile = Gdx.files.internal(TINY_NUMBERS_FONT);
        FontHelper.param.borderWidth = 2.0f * Settings.scale;
        powerAmountFont = Settings.isMobile ? FontHelper.prepFont(20.0f, false) : FontHelper.prepFont(16.0f, false);
        switch (Settings.language) {
            case ZHS: {
                fontFile = Gdx.files.internal(ZHS_BOLD_FONT);
                break;
            }
            case ZHT: {
                fontFile = Gdx.files.internal(ZHT_BOLD_FONT);
                break;
            }
            case EPO: {
                fontFile = Gdx.files.internal(EPO_BOLD_FONT);
                break;
            }
            case GRE: {
                fontFile = Gdx.files.internal(GRE_BOLD_FONT);
                break;
            }
            case JPN: {
                fontFile = Gdx.files.internal(JPN_BOLD_FONT);
                break;
            }
            case KOR: {
                fontFile = Gdx.files.internal("font/kor/GyeonggiCheonnyeonBatangBold.ttf");
                break;
            }
            case POL: 
            case RUS: 
            case UKR: {
                fontFile = Gdx.files.internal(RUS_BOLD_FONT);
                break;
            }
            case SRP: 
            case SRB: {
                fontFile = Gdx.files.internal(SRB_BOLD_FONT);
                break;
            }
            case THA: {
                fontScale = 0.95f;
                fontFile = Gdx.files.internal("font/tha/CSChatThaiUI.ttf");
                break;
            }
            case VIE: {
                fontFile = Gdx.files.internal(VIE_DRAMATIC_FONT);
                break;
            }
            default: {
                fontFile = Gdx.files.internal(ENG_DRAMATIC_FONT);
            }
        }
        FontHelper.param.gamma = 0.5f;
        FontHelper.param.borderGamma = 0.5f;
        FontHelper.param.shadowOffsetX = 0;
        FontHelper.param.shadowOffsetY = 0;
        FontHelper.param.borderWidth = 6.0f * Settings.scale;
        FontHelper.param.borderColor = new Color(0.0f, 0.0f, 0.0f, 0.5f);
        FontHelper.param.spaceX = (int)(-5.0f * Settings.scale);
        dungeonTitleFont = FontHelper.prepFont(115.0f, true);
        dungeonTitleFont.getData().setScale(1.25f);
        FontHelper.param.borderWidth = 4.0f * Settings.scale;
        FontHelper.param.borderColor = new Color(0.0f, 0.0f, 0.0f, 0.33f);
        FontHelper.param.spaceX = (int)(-2.0f * Settings.scale);
        bannerNameFont = FontHelper.prepFont(72.0f, true);
        fontScale = 1.0f;
        switch (Settings.language) {
            case ZHS: {
                fontFile = Gdx.files.internal(ZHS_ITALIC_FONT);
                break;
            }
            case ZHT: {
                fontFile = Gdx.files.internal(ZHT_ITALIC_FONT);
                break;
            }
            case EPO: {
                fontFile = Gdx.files.internal(EPO_ITALIC_FONT);
                break;
            }
            case GRE: {
                fontFile = Gdx.files.internal(GRE_ITALIC_FONT);
                break;
            }
            case JPN: {
                fontFile = Gdx.files.internal(JPN_ITALIC_FONT);
                break;
            }
            case KOR: {
                fontFile = Gdx.files.internal("font/kor/GyeonggiCheonnyeonBatangBold.ttf");
                break;
            }
            case POL: 
            case RUS: 
            case UKR: {
                fontFile = Gdx.files.internal(RUS_ITALIC_FONT);
                break;
            }
            case SRP: 
            case SRB: {
                fontFile = Gdx.files.internal(SRB_ITALIC_FONT);
                break;
            }
            case THA: {
                fontScale = 0.95f;
                fontFile = Gdx.files.internal("font/tha/CSChatThaiUI.ttf");
                break;
            }
            case VIE: {
                fontFile = Gdx.files.internal(VIE_ITALIC_FONT);
                break;
            }
            default: {
                fontFile = Gdx.files.internal(ENG_ITALIC_FONT);
            }
        }
        FontHelper.param.shadowOffsetX = 0;
        FontHelper.param.shadowOffsetY = 0;
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowOffsetX = Math.round(2.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(2.0f * Settings.scale);
        FontHelper.param.spaceX = 0;
        SRV_quoteFont = FontHelper.prepFont(28.0f, false);
        fontScale = 1.0f;
        fontFile = Gdx.files.internal(ZHS_DEFAULT_FONT);
        leaderboardFont = FontHelper.prepFont(30.0f, false);
        logger.info("Font load time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public static void ClearSCPFontTextures() {
        System.out.println("Clearing SCP font textures...");
        SCP_cardDescFont.dispose();
        SCP_cardEnergyFont.dispose();
        SCP_cardTitleFont_small.dispose();
        fontScale = Settings.language == Settings.GameLanguage.THA ? 0.95f : 1.0f;
        fontFile = SCP_cardDescFont.getData().getFontFile();
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        FontHelper.param.shadowOffsetX = Math.round(4.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(3.0f * Settings.scale);
        SCP_cardDescFont = FontHelper.prepFont(48.0f, true);
        fontScale = 1.0f;
        FontHelper.param.shadowOffsetX = Math.round(6.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(6.0f * Settings.scale);
        FontHelper.param.borderColor = new Color(0.35f, 0.35f, 0.35f, 1.0f);
        FontHelper.param.borderWidth = 4.0f * Settings.scale;
        SCP_cardTitleFont_small = FontHelper.prepFont(46.0f, true);
        FontHelper.param.borderStraight = true;
        FontHelper.param.borderColor = new Color(0.3f, 0.3f, 0.3f, 1.0f);
        FontHelper.param.borderWidth = 8.0f * Settings.scale;
        SCP_cardEnergyFont = FontHelper.prepFont(76.0f, true);
    }

    public static void ClearSRVFontTextures() {
        System.out.println("Clearing SRV font textures...");
        SRV_quoteFont.dispose();
        SCP_cardDescFont.dispose();
        fontScale = Settings.language == Settings.GameLanguage.THA ? 0.95f : 1.0f;
        fontFile = SCP_cardDescFont.getData().getFontFile();
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.shadowColor = Settings.QUARTER_TRANSPARENT_BLACK_COLOR;
        FontHelper.param.shadowOffsetX = Math.round(4.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(3.0f * Settings.scale);
        SCP_cardDescFont = FontHelper.prepFont(48.0f, true);
        fontScale = 1.0f;
        fontFile = SRV_quoteFont.getData().getFontFile();
        FontHelper.param.shadowColor = new Color(0.0f, 0.0f, 0.0f, 0.33f);
        FontHelper.param.shadowOffsetX = Math.round(2.0f * Settings.scale);
        FontHelper.param.shadowOffsetY = Math.round(2.0f * Settings.scale);
        FontHelper.param.spaceX = 0;
        SRV_quoteFont = FontHelper.prepFont(28.0f, false);
    }

    public static void ClearLeaderboardFontTextures() {
        System.out.println("Clearing leaderboard font textures...");
        leaderboardFont.dispose();
        fontScale = 1.0f;
        FontHelper.param.shadowOffsetX = 0;
        FontHelper.param.shadowOffsetY = 0;
        FontHelper.param.borderWidth = 0.0f;
        FontHelper.param.spaceX = 0;
        fontFile = leaderboardFont.getData().getFontFile();
        leaderboardFont = FontHelper.prepFont(30.0f, false);
    }

    public static BitmapFont prepFont(float size, boolean isLinearFiltering) {
        FreeTypeFontGenerator g;
        if (generators.containsKey(fontFile.path())) {
            g = generators.get(fontFile.path());
        } else {
            g = new FreeTypeFontGenerator(fontFile);
            generators.put(fontFile.path(), g);
        }
        if (Settings.BIG_TEXT_MODE) {
            size *= 1.2f;
        }
        return FontHelper.prepFont(g, size, isLinearFiltering);
    }

    private static BitmapFont prepFont(FreeTypeFontGenerator g, float size, boolean isLinearFiltering) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.characters = "";
        p.incremental = true;
        p.size = Math.round(size * fontScale * Settings.scale);
        p.gamma = FontHelper.param.gamma;
        p.spaceX = FontHelper.param.spaceX;
        p.spaceY = FontHelper.param.spaceY;
        p.borderColor = FontHelper.param.borderColor;
        p.borderStraight = FontHelper.param.borderStraight;
        p.borderWidth = FontHelper.param.borderWidth;
        p.borderGamma = FontHelper.param.borderGamma;
        p.shadowColor = FontHelper.param.shadowColor;
        p.shadowOffsetX = FontHelper.param.shadowOffsetX;
        p.shadowOffsetY = FontHelper.param.shadowOffsetY;
        if (isLinearFiltering) {
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;
        } else {
            p.minFilter = Texture.TextureFilter.Nearest;
            p.magFilter = Texture.TextureFilter.MipMapLinearNearest;
        }
        g.scaleForPixelHeight(p.size);
        BitmapFont font = g.generateFont(p);
        font.setUseIntegerPositions(!isLinearFiltering);
        font.getData().markupEnabled = true;
        if (LocalizedStrings.break_chars != null) {
            font.getData().breakChars = LocalizedStrings.break_chars.toCharArray();
        }
        font.getData().fontFile = fontFile;
        return font;
    }

    public static void renderTipLeft(SpriteBatch sb, String msg) {
        layout.setText(cardDescFont_N, msg);
        sb.setColor(Color.BLACK);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, (float)InputHelper.mX - FontHelper.layout.width - 16.0f - 12.5f, (float)InputHelper.mY - FontHelper.layout.height, FontHelper.layout.width + 16.0f, FontHelper.layout.height + 16.0f);
        FontHelper.renderFont(sb, cardDescFont_N, msg, (float)InputHelper.mX - FontHelper.layout.width - 8.0f - 12.0f, (float)InputHelper.mY + 8.0f, Color.WHITE);
    }

    public static void renderFont(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        font.setColor(c);
        font.draw((Batch)sb, msg, x, y);
    }

    public static void renderRotatedText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float offsetX, float offsetY, float angle, boolean roundY, Color c) {
        if (roundY) {
            y = (float)Math.round(y) + 0.25f;
        }
        if (font.getData().scaleX == 1.0f) {
            x = MathUtils.round(x);
            y = MathUtils.round(y);
            offsetX = MathUtils.round(offsetX);
            offsetY = MathUtils.round(offsetY);
        }
        mx4.setToRotation(0.0f, 0.0f, 1.0f, angle);
        FontHelper.rotatedTextTmp.x = offsetX;
        FontHelper.rotatedTextTmp.y = offsetY;
        rotatedTextTmp.rotate(angle);
        mx4.trn(x + FontHelper.rotatedTextTmp.x, y + FontHelper.rotatedTextTmp.y, 0.0f);
        sb.end();
        sb.setTransformMatrix(mx4);
        sb.begin();
        font.setColor(c);
        layout.setText(font, msg);
        font.draw((Batch)sb, msg, -FontHelper.layout.width / 2.0f, FontHelper.layout.height / 2.0f);
        sb.end();
        sb.setTransformMatrix(rotatedTextMatrix);
        sb.begin();
    }

    public static void renderWrappedText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float width) {
        FontHelper.renderWrappedText(sb, font, msg, x, y, width, Color.WHITE, 1.0f);
    }

    public static void renderWrappedText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float width, float scale) {
        FontHelper.renderWrappedText(sb, font, msg, x, y, width, Color.WHITE, scale);
    }

    public static void renderWrappedText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float width, Color c, float scale) {
        font.getData().setScale(scale);
        font.setColor(c);
        layout.setText(font, msg, Color.WHITE, width, 1, true);
        font.draw(sb, msg, x - width / 2.0f, y + FontHelper.layout.height / 2.0f * scale, width, 1, true);
        font.getData().setScale(1.0f);
    }

    public static void renderFontLeftDownAligned(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x, y + FontHelper.layout.height, c);
    }

    public static void renderFontRightToLeft(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg, c, 1.0f, 18, false);
        font.setColor(c);
        font.draw((Batch)sb, msg, x - FontHelper.layout.width, y);
    }

    public static void renderFontRightTopAligned(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width, y, c);
    }

    public static void renderFontRightAligned(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width, y + FontHelper.layout.height / 2.0f, c);
    }

    public static void renderFontRightTopAligned(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float scale, Color c) {
        font.getData().setScale(1.0f);
        layout.setText(font, msg);
        float offsetX = FontHelper.layout.width / 2.0f;
        float offsetY = FontHelper.layout.height;
        font.getData().setScale(scale);
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width / 2.0f - offsetX, y + FontHelper.layout.height / 2.0f + offsetY, c);
    }

    public static void renderSmartText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color baseColor) {
        FontHelper.renderSmartText(sb, font, msg, x, y, Float.MAX_VALUE, font.getLineHeight(), baseColor);
    }

    public static void renderSmartText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float lineWidth, float lineSpacing, Color baseColor) {
        if (msg == null) {
            return;
        }
        if (Settings.lineBreakViaCharacter && font.getData().markupEnabled) {
            FontHelper.exampleNonWordWrappedText(sb, font, msg, x, y, baseColor, lineWidth, lineSpacing);
            return;
        }
        curWidth = 0.0f;
        curHeight = 0.0f;
        layout.setText(font, " ");
        spaceWidth = FontHelper.layout.width;
        for (String word : msg.split(" ")) {
            if (word.equals("NL")) {
                curWidth = 0.0f;
                curHeight -= lineSpacing;
                continue;
            }
            if (word.equals("TAB")) {
                curWidth += spaceWidth * 5.0f;
                continue;
            }
            orb = FontHelper.identifyOrb(word);
            if (orb == null) {
                color = FontHelper.identifyColor(word).cpy();
                if (!color.equals(Color.WHITE)) {
                    word = word.substring(2, word.length());
                    FontHelper.color.a = baseColor.a;
                    font.setColor(color);
                } else {
                    font.setColor(baseColor);
                }
                layout.setText(font, word);
                if (curWidth + FontHelper.layout.width > lineWidth) {
                    font.draw((Batch)sb, word, x, y + (curHeight -= lineSpacing));
                    curWidth = FontHelper.layout.width + spaceWidth;
                    continue;
                }
                font.draw((Batch)sb, word, x + curWidth, y + curHeight);
                curWidth += FontHelper.layout.width + spaceWidth;
                continue;
            }
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, baseColor.a));
            if (curWidth + CARD_ENERGY_IMG_WIDTH > lineWidth) {
                sb.draw(orb, x - (float)FontHelper.orb.packedWidth / 2.0f + 13.0f * Settings.scale, y + (curHeight -= lineSpacing) - (float)FontHelper.orb.packedHeight / 2.0f - 8.0f * Settings.scale, (float)FontHelper.orb.packedWidth / 2.0f, (float)FontHelper.orb.packedHeight / 2.0f, FontHelper.orb.packedWidth, FontHelper.orb.packedHeight, Settings.scale, Settings.scale, 0.0f);
                curWidth = CARD_ENERGY_IMG_WIDTH + spaceWidth;
                continue;
            }
            sb.draw(orb, x + curWidth - (float)FontHelper.orb.packedWidth / 2.0f + 13.0f * Settings.scale, y + curHeight - (float)FontHelper.orb.packedHeight / 2.0f - 8.0f * Settings.scale, (float)FontHelper.orb.packedWidth / 2.0f, (float)FontHelper.orb.packedHeight / 2.0f, FontHelper.orb.packedWidth, FontHelper.orb.packedHeight, Settings.scale, Settings.scale, 0.0f);
            curWidth += CARD_ENERGY_IMG_WIDTH + spaceWidth;
        }
        layout.setText(font, msg);
    }

    public static void renderSmartText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float lineWidth, float lineSpacing, Color baseColor, float scale) {
        BitmapFont.BitmapFontData data = font.getData();
        float prevScale = data.scaleX;
        data.setScale(scale);
        FontHelper.renderSmartText(sb, font, msg, x, y, lineWidth, lineSpacing, baseColor);
        data.setScale(prevScale);
    }

    public static float getSmartHeight(BitmapFont font, String msg, float lineWidth, float lineSpacing) {
        if (msg == null) {
            return 0.0f;
        }
        if (Settings.lineBreakViaCharacter) {
            return -FontHelper.getHeightForCharLineBreak(font, msg, lineWidth, lineSpacing);
        }
        curWidth = 0.0f;
        curHeight = 0.0f;
        layout.setText(font, " ");
        spaceWidth = FontHelper.layout.width;
        for (String word : msg.split(" ")) {
            if (word.equals("NL")) {
                curWidth = 0.0f;
                curHeight -= lineSpacing;
                continue;
            }
            if (word.equals("TAB")) {
                curWidth += spaceWidth * 5.0f;
                continue;
            }
            orb = FontHelper.identifyOrb(word);
            if (orb == null) {
                if (!FontHelper.identifyColor(word).equals(Color.WHITE)) {
                    word = word.substring(2, word.length());
                }
                layout.setText(font, word);
                if (curWidth + FontHelper.layout.width > lineWidth) {
                    curHeight -= lineSpacing;
                    curWidth = FontHelper.layout.width + spaceWidth;
                    continue;
                }
                curWidth += FontHelper.layout.width + spaceWidth;
                continue;
            }
            if (curWidth + CARD_ENERGY_IMG_WIDTH > lineWidth) {
                curHeight -= lineSpacing;
                curWidth = CARD_ENERGY_IMG_WIDTH + spaceWidth;
                continue;
            }
            curWidth += CARD_ENERGY_IMG_WIDTH + spaceWidth;
        }
        return curHeight;
    }

    private static float getHeightForCharLineBreak(BitmapFont font, String msg, float lineWidth, float lineSpacing) {
        newMsg.setLength(0);
        for (String word : msg.split(" ")) {
            if (word.equals("NL")) {
                newMsg.append("\n");
                continue;
            }
            if (word.length() > 0 && word.charAt(0) == '#') {
                newMsg.append(word.substring(2));
                continue;
            }
            newMsg.append(word);
        }
        msg = newMsg.toString();
        if (msg != null && msg.length() > 0) {
            layout.setText(font, msg, Color.WHITE, lineWidth, -1, true);
        }
        return FontHelper.layout.height - 16.0f * Settings.scale;
    }

    public static float getHeight(BitmapFont font) {
        layout.setText(font, "gl0!");
        return FontHelper.layout.height;
    }

    public static float getSmartWidth(BitmapFont font, String msg, float lineWidth, float lineSpacing) {
        curWidth = 0.0f;
        layout.setText(font, " ");
        spaceWidth = FontHelper.layout.width;
        for (String word : msg.split(" ")) {
            if (word.equals("NL")) {
                curWidth = 0.0f;
                continue;
            }
            if (word.equals("TAB")) {
                curWidth += spaceWidth * 5.0f;
                continue;
            }
            orb = FontHelper.identifyOrb(word);
            if (orb == null) {
                if (!FontHelper.identifyColor(word).equals(Color.WHITE)) {
                    word = word.substring(2, word.length());
                }
                layout.setText(font, word);
                if (curWidth + FontHelper.layout.width > lineWidth) {
                    curWidth = FontHelper.layout.width + spaceWidth;
                    continue;
                }
                curWidth += FontHelper.layout.width + spaceWidth;
                continue;
            }
            if (curWidth + CARD_ENERGY_IMG_WIDTH > lineWidth) {
                curWidth = CARD_ENERGY_IMG_WIDTH + spaceWidth;
                continue;
            }
            curWidth += CARD_ENERGY_IMG_WIDTH + spaceWidth;
        }
        return curWidth;
    }

    public static float getSmartWidth(BitmapFont font, String msg, float lineWidth, float lineSpacing, float scale) {
        BitmapFont.BitmapFontData data = font.getData();
        float prevScale = data.scaleX;
        data.setScale(scale);
        float retVal = FontHelper.getSmartWidth(font, msg, lineWidth, lineSpacing);
        data.setScale(prevScale);
        return retVal;
    }

    private static TextureAtlas.AtlasRegion identifyOrb(String word) {
        switch (word) {
            case "[E]": {
                return AbstractDungeon.player != null ? AbstractDungeon.player.getOrb() : AbstractCard.orb_red;
            }
            case "[R]": {
                return AbstractCard.orb_red;
            }
            case "[G]": {
                return AbstractCard.orb_green;
            }
            case "[B]": {
                return AbstractCard.orb_blue;
            }
            case "[W]": {
                return AbstractCard.orb_purple;
            }
            case "[C]": {
                return AbstractCard.orb_card;
            }
            case "[P]": {
                return AbstractCard.orb_potion;
            }
            case "[T]": {
                return AbstractCard.orb_relic;
            }
            case "[S]": {
                return AbstractCard.orb_special;
            }
        }
        return null;
    }

    private static Color identifyColor(String word) {
        if (word.length() > 0 && word.charAt(0) == '#') {
            switch (word.charAt(1)) {
                case 'r': {
                    return Settings.RED_TEXT_COLOR;
                }
                case 'g': {
                    return Settings.GREEN_TEXT_COLOR;
                }
                case 'b': {
                    return Settings.BLUE_TEXT_COLOR;
                }
                case 'y': {
                    return Settings.GOLD_COLOR;
                }
                case 'p': {
                    return Settings.PURPLE_COLOR;
                }
            }
            return Color.WHITE;
        }
        return Color.WHITE;
    }

    public static void renderDeckViewTip(SpriteBatch sb, String msg, float y, Color color) {
        layout.setText(cardDescFont_N, msg);
        sb.setColor(Settings.TWO_THIRDS_TRANSPARENT_BLACK_COLOR);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, (float)Settings.WIDTH / 2.0f - FontHelper.layout.width / 2.0f - 12.0f * Settings.scale, y - 24.0f * Settings.scale, FontHelper.layout.width + 24.0f * Settings.scale, 48.0f * Settings.scale);
        FontHelper.renderFontCentered(sb, cardDescFont_N, msg, (float)Settings.WIDTH / 2.0f, y, color);
    }

    public static void renderFontLeftTopAligned(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x, y, c);
    }

    public static void renderFontCentered(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width / 2.0f, y + FontHelper.layout.height / 2.0f, c);
    }

    public static void renderFontLeft(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x, y + FontHelper.layout.height / 2.0f, c);
    }

    public static void exampleNonWordWrappedText(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c, float widthMax, float lineSpacing) {
        layout.setText(font, msg, Color.WHITE, 0.0f, -1, false);
        currentLine = 0;
        curWidth = 0.0f;
        for (String word : msg.split(" ")) {
            if (word.length() == 0) continue;
            if (word.equals("NL")) {
                curWidth = 0.0f;
                ++currentLine;
                continue;
            }
            if (word.equals("TAB")) {
                layout.setText(font, word);
                curWidth += FontHelper.layout.width;
                continue;
            }
            if (word.charAt(0) == '[') {
                orb = FontHelper.identifyOrb(word);
                if (orb == null) continue;
                sb.setColor(new Color(1.0f, 1.0f, 1.0f, c.a));
                if (!(CARD_ENERGY_IMG_WIDTH > widthMax * 2.0f)) {
                    if (curWidth + CARD_ENERGY_IMG_WIDTH > widthMax) {
                        sb.draw(orb, x - (float)FontHelper.orb.packedWidth / 2.0f + 14.0f * Settings.scale, y - (float)currentLine * lineSpacing - (float)FontHelper.orb.packedHeight / 2.0f - 38.0f * Settings.scale, (float)FontHelper.orb.packedWidth / 2.0f, (float)FontHelper.orb.packedHeight / 2.0f, FontHelper.orb.packedWidth, FontHelper.orb.packedHeight, Settings.scale, Settings.scale, 0.0f);
                    } else {
                        sb.draw(orb, x + curWidth - (float)FontHelper.orb.packedWidth / 2.0f + 14.0f * Settings.scale, y - (float)currentLine * lineSpacing - (float)FontHelper.orb.packedHeight / 2.0f - 8.0f * Settings.scale, (float)FontHelper.orb.packedWidth / 2.0f, (float)FontHelper.orb.packedHeight / 2.0f, FontHelper.orb.packedWidth, FontHelper.orb.packedHeight, Settings.scale, Settings.scale, 0.0f);
                    }
                }
                if (!((curWidth += CARD_ENERGY_IMG_WIDTH) > widthMax)) continue;
                curWidth = CARD_ENERGY_IMG_WIDTH;
                ++currentLine;
                continue;
            }
            if (word.charAt(0) == '#') {
                layout.setText(font, word.substring(2));
                switch (word.charAt(1)) {
                    case 'r': {
                        word = "[#ff6563]" + word.substring(2) + "[]";
                        break;
                    }
                    case 'g': {
                        word = "[#7fff00]" + word.substring(2) + "[]";
                        break;
                    }
                    case 'b': {
                        word = "[#87ceeb]" + word.substring(2) + "[]";
                        break;
                    }
                    case 'y': {
                        word = "[#efc851]" + word.substring(2) + "[]";
                        break;
                    }
                    case 'p': {
                        word = "[#0e82ee]" + word.substring(2) + "[]";
                        break;
                    }
                }
                curWidth += FontHelper.layout.width;
                if (curWidth > widthMax) {
                    curWidth = 0.0f;
                    font.draw((Batch)sb, word, x + curWidth, y - lineSpacing * (float)(++currentLine));
                    curWidth = FontHelper.layout.width;
                    continue;
                }
                font.draw((Batch)sb, word, x + curWidth - FontHelper.layout.width, y - lineSpacing * (float)currentLine);
                continue;
            }
            font.setColor(c);
            for (int i = 0; i < word.length(); ++i) {
                String j = Character.toString(word.charAt(i));
                layout.setText(font, j);
                curWidth += FontHelper.layout.width;
                if (curWidth > widthMax && !j.equals(LocalizedStrings.PERIOD)) {
                    curWidth = FontHelper.layout.width;
                    ++currentLine;
                }
                font.draw((Batch)sb, j, x + curWidth - FontHelper.layout.width, y - lineSpacing * (float)currentLine);
            }
        }
    }

    public static void renderFontCenteredTopAligned(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, "lL");
        font.setColor(c);
        font.draw(sb, msg, x, y + FontHelper.layout.height / 2.0f, 0.0f, 1, false);
    }

    public static void renderFontCentered(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width / 2.0f, y + FontHelper.layout.height / 2.0f, c);
        font.getData().setScale(1.0f);
    }

    public static void renderFontCentered(SpriteBatch sb, BitmapFont font, String msg, float x, float y) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width / 2.0f, y + FontHelper.layout.height / 2.0f, Color.WHITE);
    }

    public static void renderFontCenteredWidth(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width / 2.0f, y, c);
    }

    public static void renderFontCenteredWidth(SpriteBatch sb, BitmapFont font, String msg, float x, float y) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x - FontHelper.layout.width / 2.0f, y, Color.WHITE);
    }

    public static void renderFontCenteredHeight(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float lineWidth, Color c) {
        layout.setText(font, msg, c, lineWidth, 1, true);
        font.setColor(c);
        font.draw(sb, msg, x, y + FontHelper.layout.height / 2.0f, lineWidth, 1, true);
    }

    public static void renderFontCenteredHeight(SpriteBatch sb, BitmapFont font, String msg, float x, float y, float lineWidth, Color c, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, msg, c, lineWidth, 1, true);
        font.setColor(c);
        font.draw(sb, msg, x, y + FontHelper.layout.height / 2.0f, lineWidth, 1, true);
        font.getData().setScale(1.0f);
    }

    public static void renderFontCenteredHeight(SpriteBatch sb, BitmapFont font, String msg, float x, float y, Color c) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x, y + FontHelper.layout.height / 2.0f, c);
    }

    public static void renderFontCenteredHeight(SpriteBatch sb, BitmapFont font, String msg, float x, float y) {
        layout.setText(font, msg);
        FontHelper.renderFont(sb, font, msg, x, y + FontHelper.layout.height / 2.0f, Color.WHITE);
    }

    public static String colorString(String input, String colorValue) {
        newMsg.setLength(0);
        for (String word : input.split(" ")) {
            newMsg.append("#").append(colorValue).append(word).append(' ');
        }
        return newMsg.toString().trim();
    }

    public static float getWidth(BitmapFont font, String text, float scale) {
        layout.setText(font, text);
        return FontHelper.layout.width * scale;
    }

    public static float getHeight(BitmapFont font, String text, float scale) {
        layout.setText(font, text);
        return FontHelper.layout.height * scale;
    }

    static {
        CARD_ENERGY_IMG_WIDTH = 26.0f * Settings.scale;
        layout = new GlyphLayout();
        mx4 = new Matrix4();
        newMsg = new StringBuilder("");
    }
}

