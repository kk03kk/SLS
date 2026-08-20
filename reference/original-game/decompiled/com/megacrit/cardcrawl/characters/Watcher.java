/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.characters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.purple.Eruption;
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
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbInterface;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbPurple;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Watcher
extends AbstractPlayer {
    private static final Logger logger = LogManager.getLogger(Watcher.class.getName());
    private static final CharacterStrings characterStrings = CardCrawlGame.languagePack.getCharacterString("Watcher");
    public static final String[] NAMES = Watcher.characterStrings.NAMES;
    public static final String[] TEXT = Watcher.characterStrings.TEXT;
    private EnergyOrbInterface energyOrb = new EnergyOrbPurple();
    private Prefs prefs;
    private CharStat charStat = new CharStat(this);
    private Bone eyeBone;
    protected TextureAtlas eyeAtlas = null;
    protected Skeleton eyeSkeleton;
    public AnimationState eyeState;
    protected AnimationStateData eyeStateData;

    Watcher(String playerName) {
        super(playerName, AbstractPlayer.PlayerClass.WATCHER);
        this.drawY += 7.0f * Settings.scale;
        this.dialogX = this.drawX + 0.0f * Settings.scale;
        this.dialogY = this.drawY + 170.0f * Settings.scale;
        this.initializeClass(null, "images/characters/watcher/shoulder2.png", "images/characters/watcher/shoulder.png", "images/characters/watcher/corpse.png", this.getLoadout(), 0.0f, -5.0f, 240.0f, 270.0f, new EnergyManager(3));
        this.loadAnimation("images/characters/watcher/idle/skeleton.atlas", "images/characters/watcher/idle/skeleton.json", 1.0f);
        this.loadEyeAnimation();
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.7f);
        this.eyeBone = this.skeleton.findBone("eye_anchor");
        if (ModHelper.enabledMods.size() > 0 && (ModHelper.isModEnabled("Diverse") || ModHelper.isModEnabled("Chimera") || ModHelper.isModEnabled("Blue Cards"))) {
            this.masterMaxOrbs = 1;
        }
    }

    private void loadEyeAnimation() {
        this.eyeAtlas = new TextureAtlas(Gdx.files.internal("images/characters/watcher/eye_anim/skeleton.atlas"));
        SkeletonJson json = new SkeletonJson(this.eyeAtlas);
        json.setScale(Settings.scale / 1.0f);
        SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal("images/characters/watcher/eye_anim/skeleton.json"));
        this.eyeSkeleton = new Skeleton(skeletonData);
        this.eyeSkeleton.setColor(Color.WHITE);
        this.eyeStateData = new AnimationStateData(skeletonData);
        this.eyeState = new AnimationState(this.eyeStateData);
        this.eyeStateData.setDefaultMix(0.2f);
        this.eyeState.setAnimation(0, "None", true);
    }

    @Override
    public String getPortraitImageName() {
        return "watcherPortrait.jpg";
    }

    @Override
    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("PureWater");
        return retVal;
    }

    @Override
    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Strike_P");
        retVal.add("Strike_P");
        retVal.add("Strike_P");
        retVal.add("Strike_P");
        retVal.add("Defend_P");
        retVal.add("Defend_P");
        retVal.add("Defend_P");
        retVal.add("Defend_P");
        retVal.add("Eruption");
        retVal.add("Vigilance");
        return retVal;
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        return new Eruption();
    }

    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo(NAMES[0], TEXT[0], 72, 72, 0, 99, 5, this, this.getStartingRelics(), this.getStartingDeck(), false);
    }

    @Override
    public String getTitle(AbstractPlayer.PlayerClass plyrClass) {
        return AbstractPlayer.uiStrings.TEXT[4];
    }

    @Override
    public AbstractCard.CardColor getCardColor() {
        return AbstractCard.CardColor.PURPLE;
    }

    @Override
    public Color getCardRenderColor() {
        return Settings.PURPLE_COLOR;
    }

    @Override
    public CharacterOption getCharacterSelectOption() {
        return new CharacterOption(CharacterSelectScreen.TEXT[14], (AbstractPlayer)this, ImageMaster.CHAR_SELECT_WATCHER, ImageMaster.CHAR_SELECT_BG_WATCHER);
    }

    @Override
    public String getAchievementKey() {
        return "AMETHYST";
    }

    @Override
    public ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> tmpPool) {
        CardLibrary.addPurpleCards(tmpPool);
        if (ModHelper.isModEnabled("Red Cards")) {
            CardLibrary.addRedCards(tmpPool);
        }
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
        return Color.PURPLE.cpy();
    }

    @Override
    public String getLeaderboardCharacterName() {
        return "WATCHER";
    }

    @Override
    public Texture getEnergyImage() {
        return ImageMaster.PURPLE_ORB_FLASH_VFX;
    }

    @Override
    public int getAscensionMaxHPLoss() {
        return 4;
    }

    @Override
    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontPurple;
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
        this.prefs = SaveHelper.getPrefs("STSDataWatcher");
    }

    @Override
    public CharStat getCharStat() {
        return this.charStat;
    }

    @Override
    public int getUnlockedCardCount() {
        return UnlockTracker.unlockedPurpleCardCount;
    }

    @Override
    public int getSeenCardCount() {
        return CardLibrary.seenPurpleCards;
    }

    @Override
    public int getCardCount() {
        return CardLibrary.purpleCards;
    }

    @Override
    public boolean saveFileExists() {
        return SaveAndContinue.saveExistsAndNotCorrupted(this);
    }

    @Override
    public String getWinStreakKey() {
        return "win_streak_watcher";
    }

    @Override
    public String getLeaderboardWinStreakKey() {
        return "WATCHER_CONSECUTIVE_WINS";
    }

    @Override
    public void renderStatScreen(SpriteBatch sb, float screenX, float renderY) {
        if (!UnlockTracker.isCharacterLocked("Watcher")) {
            if (CardCrawlGame.mainMenuScreen.statsScreen.watcherHb == null) {
                CardCrawlGame.mainMenuScreen.statsScreen.watcherHb = new Hitbox(150.0f * Settings.scale, 150.0f * Settings.scale);
            }
            StatsScreen.renderHeader(sb, StatsScreen.NAMES[5], screenX, renderY);
            this.charStat.render(sb, screenX, renderY);
        }
    }

    @Override
    public void doCharSelectScreenSelectEffect() {
        CardCrawlGame.sound.playA("SELECT_WATCHER", MathUtils.random(-0.15f, 0.15f));
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    @Override
    public String getCustomModeCharacterButtonSoundKey() {
        return "SELECT_WATCHER";
    }

    @Override
    public Texture getCustomModeCharacterButtonImage() {
        return ImageMaster.FILTER_WATCHER;
    }

    @Override
    public CharacterStrings getCharacterString() {
        return CardCrawlGame.languagePack.getCharacterString("Watcher");
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
        return new Watcher(this.name);
    }

    @Override
    public TextureAtlas.AtlasRegion getOrb() {
        return AbstractCard.orb_purple;
    }

    @Override
    public void damage(DamageInfo info) {
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output - this.currentBlock > 0 && this.atlas != null) {
            AnimationState.TrackEntry e = this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
            e.setTime(0.9f);
        }
        super.damage(info);
    }

    @Override
    public String getSpireHeartText() {
        return SpireHeart.DESCRIPTIONS[15];
    }

    @Override
    public Color getSlashAttackColor() {
        return Color.MAGENTA;
    }

    @Override
    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[]{AbstractGameAction.AttackEffect.BLUNT_LIGHT, AbstractGameAction.AttackEffect.BLUNT_HEAVY, AbstractGameAction.AttackEffect.BLUNT_LIGHT, AbstractGameAction.AttackEffect.BLUNT_HEAVY, AbstractGameAction.AttackEffect.BLUNT_HEAVY, AbstractGameAction.AttackEffect.BLUNT_LIGHT};
    }

    @Override
    public String getVampireText() {
        return Vampires.DESCRIPTIONS[1];
    }

    @Override
    public void onStanceChange(String newStance) {
        if (newStance.equals("Calm")) {
            this.eyeState.setAnimation(0, "Calm", true);
        } else if (newStance.equals("Wrath")) {
            this.eyeState.setAnimation(0, "Wrath", true);
        } else if (newStance.equals("Divinity")) {
            this.eyeState.setAnimation(0, "Divinity", true);
        } else if (newStance.equals("Neutral")) {
            this.eyeState.setAnimation(0, "None", true);
        } else {
            this.eyeState.setAnimation(0, "None", true);
        }
    }

    @Override
    public void renderPlayerImage(SpriteBatch sb) {
        this.state.update(Gdx.graphics.getDeltaTime());
        this.state.apply(this.skeleton);
        this.skeleton.updateWorldTransform();
        this.skeleton.setPosition(this.drawX + this.animX, this.drawY + this.animY);
        this.skeleton.setColor(this.tint.color);
        this.skeleton.setFlip(this.flipHorizontal, this.flipVertical);
        this.eyeState.update(Gdx.graphics.getDeltaTime());
        this.eyeState.apply(this.eyeSkeleton);
        this.eyeSkeleton.updateWorldTransform();
        this.eyeSkeleton.setPosition(this.skeleton.getX() + this.eyeBone.getWorldX(), this.skeleton.getY() + this.eyeBone.getWorldY());
        this.eyeSkeleton.setColor(this.tint.color);
        this.eyeSkeleton.setFlip(this.flipHorizontal, this.flipVertical);
        sb.end();
        CardCrawlGame.psb.begin();
        sr.draw(CardCrawlGame.psb, this.skeleton);
        sr.draw(CardCrawlGame.psb, this.eyeSkeleton);
        CardCrawlGame.psb.end();
        sb.begin();
    }
}

