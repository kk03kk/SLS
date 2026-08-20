/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.audio.MockMusic;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainMusic {
    private static final Logger logger = LogManager.getLogger(MainMusic.class.getName());
    private Music music;
    public String key;
    private static final String DIR = "audio/music/";
    private static final String TITLE_BGM = "STS_MenuTheme_NewMix_v1.ogg";
    private static final String LEVEL_1_1_BGM = "STS_Level1_NewMix_v1.ogg";
    private static final String LEVEL_1_2_BGM = "STS_Level1-2_v2.ogg";
    private static final String LEVEL_2_1_BGM = "STS_Level2_NewMix_v1.ogg";
    private static final String LEVEL_2_2_BGM = "STS_Level2-2_v2.ogg";
    private static final String LEVEL_3_1_BGM = "STS_Level3_v2.ogg";
    private static final String LEVEL_3_2_BGM = "STS_Level3-2_v2.ogg";
    private static final String LEVEL_4_1_BGM = "STS_Act4_BGM_v2.ogg";
    public boolean isSilenced = false;
    private float silenceTimer = 0.0f;
    private float silenceTime = 0.0f;
    private static final float SILENCE_TIME = 4.0f;
    private static final float FAST_SILENCE_TIME = 0.25f;
    private float silenceStartVolume;
    private static final float FADE_IN_TIME = 4.0f;
    private static final float FADE_OUT_TIME = 4.0f;
    private float fadeTimer = 0.0f;
    public boolean isFadingOut = false;
    private float fadeOutStartVolume;
    public boolean isDone = false;

    public MainMusic(String key) {
        this.key = key;
        this.music = this.getSong(key);
        this.fadeTimer = 4.0f;
        this.music.setLooping(true);
        this.music.play();
        this.music.setVolume(0.0f);
    }

    private Music getSong(String key) {
        switch (key) {
            case "Exordium": {
                switch (AbstractDungeon.miscRng.random(1)) {
                    case 0: {
                        return MainMusic.newMusic("audio/music/STS_Level1_NewMix_v1.ogg");
                    }
                }
                return MainMusic.newMusic("audio/music/STS_Level1-2_v2.ogg");
            }
            case "TheCity": {
                switch (AbstractDungeon.miscRng.random(1)) {
                    case 0: {
                        return MainMusic.newMusic("audio/music/STS_Level2_NewMix_v1.ogg");
                    }
                }
                return MainMusic.newMusic("audio/music/STS_Level2-2_v2.ogg");
            }
            case "TheBeyond": {
                switch (AbstractDungeon.miscRng.random(1)) {
                    case 0: {
                        return MainMusic.newMusic("audio/music/STS_Level3_v2.ogg");
                    }
                }
                return MainMusic.newMusic("audio/music/STS_Level3-2_v2.ogg");
            }
            case "TheEnding": {
                return MainMusic.newMusic("audio/music/STS_Act4_BGM_v2.ogg");
            }
            case "MENU": {
                return MainMusic.newMusic("audio/music/STS_MenuTheme_NewMix_v1.ogg");
            }
        }
        logger.info("NO SUCH MAIN BGM (playing level_1 instead): " + key);
        return MainMusic.newMusic("audio/music/STS_Level1_NewMix_v1.ogg");
    }

    public static Music newMusic(String path) {
        if (Gdx.audio == null) {
            logger.info("WARNING: Gdx.audio is null so no Music instance can be initialized.");
            return new MockMusic();
        }
        return Gdx.audio.newMusic(Gdx.files.internal(path));
    }

    public void updateVolume() {
        if (!this.isFadingOut && !this.isSilenced) {
            this.music.setVolume(Settings.MUSIC_VOLUME * Settings.MASTER_VOLUME);
        }
    }

    public void fadeOut() {
        this.isFadingOut = true;
        this.fadeOutStartVolume = this.music.getVolume();
        this.fadeTimer = 4.0f;
    }

    public void silence() {
        this.isSilenced = true;
        this.silenceTimer = 4.0f;
        this.silenceTime = 4.0f;
        this.silenceStartVolume = this.music.getVolume();
    }

    public void silenceInstantly() {
        this.isSilenced = true;
        this.silenceTimer = 0.25f;
        this.silenceTime = 0.25f;
        this.silenceStartVolume = this.music.getVolume();
    }

    public void unsilence() {
        if (this.isSilenced) {
            logger.info("Unsilencing " + this.key);
            this.isSilenced = false;
            this.fadeTimer = 4.0f;
        }
    }

    public void kill() {
        this.music.dispose();
        this.isDone = true;
    }

    public void update() {
        if (!this.isFadingOut) {
            this.updateFadeIn();
        } else {
            this.updateFadeOut();
        }
    }

    private void updateFadeIn() {
        if (!this.isSilenced) {
            this.fadeTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeTimer < 0.0f) {
                this.fadeTimer = 0.0f;
            }
            if (!Settings.isBackgrounded) {
                this.music.setVolume(Interpolation.fade.apply(0.0f, 1.0f, 1.0f - this.fadeTimer / 4.0f) * (Settings.MUSIC_VOLUME * Settings.MASTER_VOLUME));
            } else {
                this.music.setVolume(MathHelper.slowColorLerpSnap(this.music.getVolume(), 0.0f));
            }
        } else {
            this.silenceTimer -= Gdx.graphics.getDeltaTime();
            if (this.silenceTimer < 0.0f) {
                this.silenceTimer = 0.0f;
            }
            if (!Settings.isBackgrounded) {
                this.music.setVolume(Interpolation.fade.apply(this.silenceStartVolume, 0.0f, 1.0f - this.silenceTimer / this.silenceTime));
            } else {
                this.music.setVolume(MathHelper.slowColorLerpSnap(this.music.getVolume(), 0.0f));
            }
        }
    }

    private void updateFadeOut() {
        if (!this.isSilenced) {
            this.fadeTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeTimer < 0.0f) {
                this.fadeTimer = 0.0f;
                this.isDone = true;
                logger.info("Disposing MainMusic: " + this.key);
                this.music.dispose();
            } else {
                this.music.setVolume(Interpolation.fade.apply(this.fadeOutStartVolume, 0.0f, 1.0f - this.fadeTimer / 4.0f));
            }
        } else {
            this.silenceTimer -= Gdx.graphics.getDeltaTime();
            if (this.silenceTimer < 0.0f) {
                this.silenceTimer = 0.0f;
            }
            this.music.setVolume(Interpolation.fade.apply(this.silenceStartVolume, 0.0f, 1.0f - this.silenceTimer / this.silenceTime));
        }
    }
}

