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
import com.megacrit.cardcrawl.cards.red.Bash;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.events.beyond.SpireHeart;
import com.megacrit.cardcrawl.events.city.Vampires;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
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
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbInterface;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbRed;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ironclad
extends AbstractPlayer {
    private static final Logger logger = LogManager.getLogger(Ironclad.class.getName());
    private static final CharacterStrings characterStrings = CardCrawlGame.languagePack.getCharacterString("Ironclad");
    public static final String[] NAMES = Ironclad.characterStrings.NAMES;
    public static final String[] TEXT = Ironclad.characterStrings.TEXT;
    private EnergyOrbInterface energyOrb = new EnergyOrbRed();
    private Prefs prefs;
    private CharStat charStat = new CharStat(this);

    Ironclad(String playerName) {
        super(playerName, AbstractPlayer.PlayerClass.IRONCLAD);
        this.dialogX = this.drawX + 0.0f * Settings.scale;
        this.dialogY = this.drawY + 220.0f * Settings.scale;
        this.initializeClass(null, "images/characters/ironclad/shoulder2.png", "images/characters/ironclad/shoulder.png", "images/characters/ironclad/corpse.png", this.getLoadout(), -4.0f, -16.0f, 220.0f, 290.0f, new EnergyManager(3));
        this.loadAnimation("images/characters/ironclad/idle/skeleton.atlas", "images/characters/ironclad/idle/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.6f);
        if (ModHelper.enabledMods.size() > 0 && (ModHelper.isModEnabled("Diverse") || ModHelper.isModEnabled("Chimera") || ModHelper.isModEnabled("Blue Cards"))) {
            this.masterMaxOrbs = 1;
        }
    }

    @Override
    public String getPortraitImageName() {
        return "ironcladPortrait.jpg";
    }

    @Override
    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Burning Blood");
        UnlockTracker.markRelicAsSeen("Burning Blood");
        return retVal;
    }

    @Override
    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Strike_R");
        retVal.add("Strike_R");
        retVal.add("Strike_R");
        retVal.add("Strike_R");
        retVal.add("Strike_R");
        retVal.add("Defend_R");
        retVal.add("Defend_R");
        retVal.add("Defend_R");
        retVal.add("Defend_R");
        retVal.add("Bash");
        return retVal;
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        return new Bash();
    }

    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo(NAMES[0], TEXT[0], 80, 80, 0, 99, 5, this, this.getStartingRelics(), this.getStartingDeck(), false);
    }

    @Override
    public String getTitle(AbstractPlayer.PlayerClass plyrClass) {
        return AbstractPlayer.uiStrings.TEXT[1];
    }

    @Override
    public AbstractCard.CardColor getCardColor() {
        return AbstractCard.CardColor.RED;
    }

    @Override
    public Color getCardRenderColor() {
        return Color.SCARLET;
    }

    @Override
    public String getAchievementKey() {
        return "RUBY";
    }

    @Override
    public ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> tmpPool) {
        CardLibrary.addRedCards(tmpPool);
        if (ModHelper.isModEnabled("Green Cards")) {
            CardLibrary.addGreenCards(tmpPool);
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
        return new Color(1.0f, 0.4f, 0.1f, 1.0f);
    }

    @Override
    public String getLeaderboardCharacterName() {
        return "IRONCLAD";
    }

    @Override
    public Texture getEnergyImage() {
        return ImageMaster.RED_ORB_FLASH_VFX;
    }

    @Override
    public int getAscensionMaxHPLoss() {
        return 5;
    }

    @Override
    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontRed;
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
        this.prefs = SaveHelper.getPrefs("STSDataVagabond");
    }

    @Override
    public CharStat getCharStat() {
        return this.charStat;
    }

    @Override
    public int getUnlockedCardCount() {
        return UnlockTracker.unlockedRedCardCount;
    }

    @Override
    public int getSeenCardCount() {
        return CardLibrary.seenRedCards;
    }

    @Override
    public int getCardCount() {
        return CardLibrary.redCards;
    }

    @Override
    public boolean saveFileExists() {
        return SaveAndContinue.saveExistsAndNotCorrupted(this);
    }

    @Override
    public String getWinStreakKey() {
        return "win_streak_ironclad";
    }

    @Override
    public String getLeaderboardWinStreakKey() {
        return "IRONCLAD_CONSECUTIVE_WINS";
    }

    @Override
    public void renderStatScreen(SpriteBatch sb, float screenX, float renderY) {
        StatsScreen.renderHeader(sb, StatsScreen.NAMES[2], screenX, renderY);
        this.charStat.render(sb, screenX, renderY);
    }

    @Override
    public void doCharSelectScreenSelectEffect() {
        CardCrawlGame.sound.playA("ATTACK_HEAVY", MathUtils.random(-0.2f, 0.2f));
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, true);
    }

    @Override
    public String getCustomModeCharacterButtonSoundKey() {
        return "ATTACK_HEAVY";
    }

    @Override
    public Texture getCustomModeCharacterButtonImage() {
        return ImageMaster.FILTER_IRONCLAD;
    }

    @Override
    public CharacterStrings getCharacterString() {
        return CardCrawlGame.languagePack.getCharacterString("Ironclad");
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
        return new Ironclad(this.name);
    }

    @Override
    public TextureAtlas.AtlasRegion getOrb() {
        return AbstractCard.orb_red;
    }

    @Override
    public void damage(DamageInfo info) {
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output - this.currentBlock > 0) {
            AnimationState.TrackEntry e = this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
            e.setTimeScale(0.6f);
        }
        super.damage(info);
    }

    @Override
    public String getSpireHeartText() {
        return SpireHeart.DESCRIPTIONS[8];
    }

    @Override
    public Color getSlashAttackColor() {
        return Color.RED;
    }

    @Override
    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[]{AbstractGameAction.AttackEffect.SLASH_HEAVY, AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.BLUNT_HEAVY, AbstractGameAction.AttackEffect.SLASH_HEAVY, AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.BLUNT_HEAVY};
    }

    @Override
    public String getVampireText() {
        return Vampires.DESCRIPTIONS[0];
    }
}

