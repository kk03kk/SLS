/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.characters;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.blue.Zap;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.events.beyond.SpireHeart;
import com.megacrit.cardcrawl.events.city.Vampires;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbBlue;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbInterface;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Defect
extends AbstractPlayer {
    private static final Logger logger = LogManager.getLogger(Defect.class.getName());
    private static final CharacterStrings characterStrings = CardCrawlGame.languagePack.getCharacterString("Defect");
    public static final String[] NAMES = Defect.characterStrings.NAMES;
    public static final String[] TEXT = Defect.characterStrings.TEXT;
    private EnergyOrbInterface energyOrb = new EnergyOrbBlue();
    private Prefs prefs;
    private CharStat charStat = new CharStat(this);

    Defect(String playerName) {
        super(playerName, AbstractPlayer.PlayerClass.DEFECT);
        this.drawX += 5.0f * Settings.scale;
        this.drawY += 7.0f * Settings.scale;
        this.dialogX = this.drawX + 0.0f * Settings.scale;
        this.dialogY = this.drawY + 170.0f * Settings.scale;
        this.initializeClass(null, "images/characters/defect/shoulder2.png", "images/characters/defect/shoulder.png", "images/characters/defect/corpse.png", this.getLoadout(), 0.0f, -5.0f, 240.0f, 244.0f, new EnergyManager(3));
        this.loadAnimation("images/characters/defect/idle/skeleton.atlas", "images/characters/defect/idle/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.9f);
    }

    @Override
    public String getPortraitImageName() {
        return "defectPortrait.jpg";
    }

    @Override
    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Cracked Core");
        UnlockTracker.markRelicAsSeen("Cracked Core");
        return retVal;
    }

    @Override
    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Strike_B");
        retVal.add("Strike_B");
        retVal.add("Strike_B");
        retVal.add("Strike_B");
        retVal.add("Defend_B");
        retVal.add("Defend_B");
        retVal.add("Defend_B");
        retVal.add("Defend_B");
        retVal.add("Zap");
        retVal.add("Dualcast");
        return retVal;
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        return new Zap();
    }

    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo(NAMES[0], TEXT[0], 75, 75, 3, 99, 5, this, this.getStartingRelics(), this.getStartingDeck(), false);
    }

    @Override
    public String getTitle(AbstractPlayer.PlayerClass plyrClass) {
        return AbstractPlayer.uiStrings.TEXT[3];
    }

    @Override
    public AbstractCard.CardColor getCardColor() {
        return AbstractCard.CardColor.BLUE;
    }

    @Override
    public Color getCardRenderColor() {
        return Color.SKY;
    }

    @Override
    public String getAchievementKey() {
        return "SAPPHIRE";
    }

    @Override
    public ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> tmpPool) {
        CardLibrary.addBlueCards(tmpPool);
        if (ModHelper.isModEnabled("Red Cards")) {
            CardLibrary.addRedCards(tmpPool);
        }
        if (ModHelper.isModEnabled("Green Cards")) {
            CardLibrary.addGreenCards(tmpPool);
        }
        if (ModHelper.isModEnabled("Purple Cards")) {
            CardLibrary.addPurpleCards(tmpPool);
        }
        return tmpPool;
    }

    @Override
    public Color getCardTrailColor() {
        return Color.SKY.cpy();
    }

    @Override
    public String getLeaderboardCharacterName() {
        return "DEFECT";
    }

    @Override
    public Texture getEnergyImage() {
        return ImageMaster.BLUE_ORB_FLASH_VFX;
    }

    @Override
    public int getAscensionMaxHPLoss() {
        return 4;
    }

    @Override
    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontBlue;
    }

    @Override
    public void renderOrb(SpriteBatch sb, boolean enabled, float current_x, float current_y) {
        this.energyOrb.renderOrb(sb, enabled, current_x, current_y);
    }

    @Override
    public void updateOrb(int orbCount) {
        this.energyOrb.updateOrb(orbCount);
    }

    @Override
    public Prefs getPrefs() {
        if (this.prefs == null) {
            logger.error("prefs need to be initialized first!");
        }
        return this.prefs;
    }

    @Override
    public void loadPrefs() {
        this.prefs = SaveHelper.getPrefs("STSDataDefect");
    }

    @Override
    public CharStat getCharStat() {
        return this.charStat;
    }

    @Override
    public int getUnlockedCardCount() {
        return UnlockTracker.unlockedBlueCardCount;
    }

    @Override
    public int getSeenCardCount() {
        return CardLibrary.seenBlueCards;
    }

    @Override
    public int getCardCount() {
        return CardLibrary.blueCards;
    }

    @Override
    public boolean saveFileExists() {
        return SaveAndContinue.saveExistsAndNotCorrupted(this);
    }

    @Override
    public String getWinStreakKey() {
        return "win_streak_defect";
    }

    @Override
    public String getLeaderboardWinStreakKey() {
        return "DEFECT_CONSECUTIVE_WINS";
    }

    @Override
    public void renderStatScreen(SpriteBatch sb, float screenX, float renderY) {
        if (!UnlockTracker.isCharacterLocked("Defect")) {
            if (CardCrawlGame.mainMenuScreen.statsScreen.defectHb == null) {
                CardCrawlGame.mainMenuScreen.statsScreen.defectHb = new Hitbox(150.0f * Settings.scale, 150.0f * Settings.scale);
            }
            StatsScreen.renderHeader(sb, StatsScreen.NAMES[4], screenX, renderY);
            this.charStat.render(sb, screenX, renderY);
        }
    }

    @Override
    public void doCharSelectScreenSelectEffect() {
        CardCrawlGame.sound.playA("ATTACK_MAGIC_BEAM_SHORT", MathUtils.random(-0.2f, 0.2f));
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    @Override
    public String getCustomModeCharacterButtonSoundKey() {
        return "ATTACK_MAGIC_BEAM_SHORT";
    }

    @Override
    public Texture getCustomModeCharacterButtonImage() {
        return ImageMaster.FILTER_DEFECT;
    }

    @Override
    public CharacterStrings getCharacterString() {
        return CardCrawlGame.languagePack.getCharacterString("Defect");
    }

    @Override
    public String getLocalizedCharacterName() {
        return NAMES[0];
    }

    @Override
    public void refreshCharStat() {
        this.charStat = new CharStat(this);
    }

    @Override
    public AbstractPlayer newInstance() {
        return new Defect(this.name);
    }

    @Override
    public TextureAtlas.AtlasRegion getOrb() {
        return AbstractCard.orb_blue;
    }

    @Override
    public void damage(DamageInfo info) {
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output - this.currentBlock > 0) {
            AnimationState.TrackEntry e = this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
            e.setTime(0.9f);
        }
        super.damage(info);
    }

    @Override
    public String getSpireHeartText() {
        return SpireHeart.DESCRIPTIONS[10];
    }

    @Override
    public Color getSlashAttackColor() {
        return Color.SKY;
    }

    @Override
    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[]{AbstractGameAction.AttackEffect.SLASH_HEAVY, AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.SLASH_DIAGONAL, AbstractGameAction.AttackEffect.SLASH_HEAVY, AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.SLASH_DIAGONAL};
    }

    @Override
    public String getVampireText() {
        return Vampires.DESCRIPTIONS[5];
    }
}

