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
import com.megacrit.cardcrawl.cards.green.Neutralize;
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
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbGreen;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbInterface;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheSilent
extends AbstractPlayer {
    private static final Logger logger = LogManager.getLogger(TheSilent.class.getName());
    private static final CharacterStrings characterStrings = CardCrawlGame.languagePack.getCharacterString("Silent");
    public static final String[] NAMES = TheSilent.characterStrings.NAMES;
    public static final String[] TEXT = TheSilent.characterStrings.TEXT;
    private EnergyOrbInterface energyOrb = new EnergyOrbGreen();
    private Prefs prefs;
    private CharStat charStat = new CharStat(this);

    TheSilent(String playerName) {
        super(playerName, AbstractPlayer.PlayerClass.THE_SILENT);
        this.dialogX = this.drawX + 0.0f * Settings.scale;
        this.dialogY = this.drawY + 170.0f * Settings.scale;
        this.initializeClass(null, "images/characters/theSilent/shoulder2.png", "images/characters/theSilent/shoulder.png", "images/characters/theSilent/corpse.png", this.getLoadout(), -20.0f, -24.0f, 240.0f, 240.0f, new EnergyManager(3));
        this.loadAnimation("images/characters/theSilent/idle/skeleton.atlas", "images/characters/theSilent/idle/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.9f);
        if (ModHelper.enabledMods.size() > 0 && (ModHelper.isModEnabled("Diverse") || ModHelper.isModEnabled("Chimera") || ModHelper.isModEnabled("Blue Cards"))) {
            this.masterMaxOrbs = 1;
        }
    }

    @Override
    public String getPortraitImageName() {
        return "silentPortrait.jpg";
    }

    @Override
    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Strike_G");
        retVal.add("Strike_G");
        retVal.add("Strike_G");
        retVal.add("Strike_G");
        retVal.add("Strike_G");
        retVal.add("Defend_G");
        retVal.add("Defend_G");
        retVal.add("Defend_G");
        retVal.add("Defend_G");
        retVal.add("Defend_G");
        retVal.add("Survivor");
        retVal.add("Neutralize");
        return retVal;
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        return new Neutralize();
    }

    @Override
    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Ring of the Snake");
        UnlockTracker.markRelicAsSeen("Ring of the Snake");
        return retVal;
    }

    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo(NAMES[0], TEXT[0], 70, 70, 0, 99, 5, this, this.getStartingRelics(), this.getStartingDeck(), false);
    }

    @Override
    public String getTitle(AbstractPlayer.PlayerClass plyrClass) {
        return AbstractPlayer.uiStrings.TEXT[2];
    }

    @Override
    public AbstractCard.CardColor getCardColor() {
        return AbstractCard.CardColor.GREEN;
    }

    @Override
    public Color getCardRenderColor() {
        return Color.CHARTREUSE;
    }

    @Override
    public String getAchievementKey() {
        return "EMERALD";
    }

    @Override
    public ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> tmpPool) {
        CardLibrary.addGreenCards(tmpPool);
        if (ModHelper.isModEnabled("Red Cards")) {
            CardLibrary.addRedCards(tmpPool);
        }
        if (ModHelper.isModEnabled("Blue Cards")) {
            CardLibrary.addBlueCards(tmpPool);
        }
        if (ModHelper.isModEnabled("Purple Cards")) {
            CardLibrary.addPurpleCards(tmpPool);
        }
        return tmpPool;
    }

    @Override
    public Color getCardTrailColor() {
        return Color.CHARTREUSE.cpy();
    }

    @Override
    public String getLeaderboardCharacterName() {
        return "SILENT";
    }

    @Override
    public Texture getEnergyImage() {
        return ImageMaster.GREEN_ORB_FLASH_VFX;
    }

    @Override
    public int getAscensionMaxHPLoss() {
        return 4;
    }

    @Override
    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontGreen;
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
        this.prefs = SaveHelper.getPrefs("STSDataTheSilent");
    }

    @Override
    public CharStat getCharStat() {
        return this.charStat;
    }

    @Override
    public int getUnlockedCardCount() {
        return UnlockTracker.unlockedGreenCardCount;
    }

    @Override
    public int getSeenCardCount() {
        return CardLibrary.seenGreenCards;
    }

    @Override
    public int getCardCount() {
        return CardLibrary.greenCards;
    }

    @Override
    public boolean saveFileExists() {
        return SaveAndContinue.saveExistsAndNotCorrupted(this);
    }

    @Override
    public String getWinStreakKey() {
        return "win_streak_silent";
    }

    @Override
    public String getLeaderboardWinStreakKey() {
        return "SILENT_CONSECUTIVE_WINS";
    }

    @Override
    public void renderStatScreen(SpriteBatch sb, float screenX, float renderY) {
        if (!UnlockTracker.isCharacterLocked("The Silent")) {
            if (CardCrawlGame.mainMenuScreen.statsScreen.silentHb == null) {
                CardCrawlGame.mainMenuScreen.statsScreen.silentHb = new Hitbox(150.0f * Settings.scale, 150.0f * Settings.scale);
            }
            StatsScreen.renderHeader(sb, StatsScreen.NAMES[3], screenX, renderY);
            this.charStat.render(sb, screenX, renderY);
        }
    }

    @Override
    public void doCharSelectScreenSelectEffect() {
        CardCrawlGame.sound.playA("ATTACK_DAGGER_2", MathUtils.random(-0.2f, 0.2f));
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    @Override
    public String getCustomModeCharacterButtonSoundKey() {
        return "ATTACK_DAGGER_2";
    }

    @Override
    public Texture getCustomModeCharacterButtonImage() {
        return ImageMaster.FILTER_SILENT;
    }

    @Override
    public CharacterStrings getCharacterString() {
        return CardCrawlGame.languagePack.getCharacterString("Silent");
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
        return new TheSilent(this.name);
    }

    @Override
    public TextureAtlas.AtlasRegion getOrb() {
        return AbstractCard.orb_green;
    }

    @Override
    public void damage(DamageInfo info) {
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output - this.currentBlock > 0) {
            AnimationState.TrackEntry e = this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
            e.setTimeScale(0.9f);
        }
        super.damage(info);
    }

    @Override
    public String getSpireHeartText() {
        return SpireHeart.DESCRIPTIONS[9];
    }

    @Override
    public Color getSlashAttackColor() {
        return Color.GREEN;
    }

    @Override
    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[]{AbstractGameAction.AttackEffect.SLASH_HEAVY, AbstractGameAction.AttackEffect.POISON, AbstractGameAction.AttackEffect.SLASH_DIAGONAL, AbstractGameAction.AttackEffect.SLASH_HEAVY, AbstractGameAction.AttackEffect.POISON, AbstractGameAction.AttackEffect.SLASH_DIAGONAL};
    }

    @Override
    public String getVampireText() {
        return Vampires.DESCRIPTIONS[1];
    }
}

