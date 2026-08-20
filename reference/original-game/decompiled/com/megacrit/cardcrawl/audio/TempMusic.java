/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.audio.MainMusic;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TempMusic {
    private static final Logger logger = LogManager.getLogger(TempMusic.class.getName());
    private Music music;
    public String key;
    private static final String DIR = "audio/music/";
    private static final String SHOP_BGM = "STS_Merchant_NewMix_v1.ogg";
    private static final String SHRINE_BGM = "STS_Shrine_NewMix_v1.ogg";
    private static final String MINDBLOOM_BGM = "STS_Boss1MindBloom_v1.ogg";
    private static final String LEVEL_1_BOSS_BGM = "STS_Boss1_NewMix_v1.ogg";
    private static final String LEVEL_2_BOSS_BGM = "STS_Boss2_NewMix_v1.ogg";
    private static final String LEVEL_3_BOSS_BGM = "STS_Boss3_NewMix_v1.ogg";
    private static final String LEVEL_4_BOSS_BGM = "STS_Boss4_v6.ogg";
    private static final String ELITE_BGM = "STS_EliteBoss_NewMix_v1.ogg";
    private static final String CREDITS = "STS_Credits_v5.ogg";
    public boolean isSilenced = false;
    private float silenceTimer = 0.0f;
    private float silenceTime = 0.0f;
    private static final float FAST_SILENCE_TIME = 0.25f;
    private float silenceStartVolume;
    private static final float FADE_IN_TIME = 4.0f;
    private static final float FAST_FADE_IN_TIME = 0.25f;
    private static final float FADE_OUT_TIME = 4.0f;
    private float fadeTimer;
    private float fadeTime;
    public boolean isFadingOut = false;
    private float fadeOutStartVolume;
    public boolean isDone = false;

    public TempMusic(String key, boolean isFast) {
        this(key, isFast, true);
    }

    public TempMusic(String key, boolean isFast, boolean loop) {
        this.key = key;
        this.music = this.getSong(key);
        if (isFast) {
            this.fadeTimer = 0.25f;
            this.fadeTime = 0.25f;
        } else {
            this.fadeTimer = 4.0f;
            this.fadeTime = 4.0f;
        }
        this.music.setLooping(loop);
        this.music.play();
        this.music.setVolume(0.0f);
    }

    public TempMusic(String key, boolean isFast, boolean loop, boolean precache) {
        this.key = key;
        this.music = this.getSong(key);
        if (isFast) {
            this.fadeTimer = 0.25f;
            this.fadeTime = 0.25f;
        } else {
            this.fadeTimer = 4.0f;
            this.fadeTime = 4.0f;
        }
        this.music.setLooping(loop);
        this.music.setVolume(0.0f);
    }

    public void playPrecached() {
        if (!this.music.isPlaying()) {
            this.music.play();
        } else {
            logger.info("[WARNING] Attempted to play music that is already playing.");
        }
    }

    private Music getSong(String key) {
        switch (key) {
            case "SHOP": {
                return MainMusic.newMusic("audio/music/STS_Merchant_NewMix_v1.ogg");
            }
            case "SHRINE": {
                return MainMusic.newMusic("audio/music/STS_Shrine_NewMix_v1.ogg");
            }
            case "MINDBLOOM": {
                return MainMusic.newMusic("audio/music/STS_Boss1MindBloom_v1.ogg");
            }
            case "BOSS_BOTTOM": {
                return MainMusic.newMusic("audio/music/STS_Boss1_NewMix_v1.ogg");
            }
            case "BOSS_CITY": {
                return MainMusic.newMusic("audio/music/STS_Boss2_NewMix_v1.ogg");
            }
            case "BOSS_BEYOND": {
                return MainMusic.newMusic("audio/music/STS_Boss3_NewMix_v1.ogg");
            }
            case "BOSS_ENDING": {
                return MainMusic.newMusic("audio/music/STS_Boss4_v6.ogg");
            }
            case "ELITE": {
                return MainMusic.newMusic("audio/music/STS_EliteBoss_NewMix_v1.ogg");
            }
            case "CREDITS": {
                return MainMusic.newMusic("audio/music/STS_Credits_v5.ogg");
            }
        }
        return MainMusic.newMusic(DIR + key);
    }

    public void fadeOut() {
        this.isFadingOut = true;
        this.fadeOutStartVolume = this.music.getVolume();
        this.fadeTimer = 4.0f;
    }

    public void silenceInstantly() {
        this.isSilenced = true;
        this.silenceTimer = 0.25f;
        this.silenceTime = 0.25f;
        this.silenceStartVolume = this.music.getVolume();
    }

    public void kill() {
        logger.info("Disposing TempMusic: " + this.key);
        this.music.dispose();
        this.isDone = true;
    }

    public void update() {
        if (this.music.isPlaying()) {
            if (!this.isFadingOut) {
                this.updateFadeIn();
            } else {
                this.updateFadeOut();
            }
        } else if (this.isFadingOut) {
            this.kill();
        }
    }

    private void updateFadeIn() {
        if (!this.isSilenced) {
            this.fadeTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeTimer < 0.0f) {
                this.fadeTimer = 0.0f;
                if (!Settings.isBackgrounded) {
                    this.music.setVolume(Interpolation.fade.apply(0.0f, 1.0f, 1.0f - this.fadeTimer / this.fadeTime) * (Settings.MUSIC_VOLUME * Settings.MASTER_VOLUME));
                } else {
                    this.music.setVolume(MathHelper.slowColorLerpSnap(this.music.getVolume(), 0.0f));
                }
            } else if (!Settings.isBackgrounded) {
                this.music.setVolume(Interpolation.fade.apply(0.0f, 1.0f, 1.0f - this.fadeTimer / this.fadeTime) * (Settings.MUSIC_VOLUME * Settings.MASTER_VOLUME));
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
        this.fadeTimer -= Gdx.graphics.getDeltaTime();
        if (this.fadeTimer < 0.0f) {
            this.fadeTimer = 0.0f;
            this.isDone = true;
            logger.info("Disposing TempMusic: " + this.key);
            this.music.dispose();
        } else {
            this.music.setVolume(Interpolation.fade.apply(this.fadeOutStartVolume, 0.0f, 1.0f - this.fadeTimer / 4.0f));
        }
    }

    public void updateVolume() {
        if (!this.isFadingOut && !this.isSilenced) {
            this.music.setVolume(Settings.MUSIC_VOLUME * Settings.MASTER_VOLUME);
        }
    }
}

