/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.audio;

import com.megacrit.cardcrawl.audio.MainMusic;
import com.megacrit.cardcrawl.audio.TempMusic;
import com.megacrit.cardcrawl.core.Settings;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MusicMaster {
    private static final Logger logger = LogManager.getLogger(MusicMaster.class.getName());
    private ArrayList<MainMusic> mainTrack = new ArrayList();
    private ArrayList<TempMusic> tempTrack = new ArrayList();

    public MusicMaster() {
        Settings.MASTER_VOLUME = Settings.soundPref.getFloat("Master Volume", 0.5f);
        Settings.MUSIC_VOLUME = Settings.soundPref.getFloat("Music Volume", 0.5f);
        logger.info("Music Volume: " + Settings.MUSIC_VOLUME);
    }

    public void update() {
        this.updateBGM();
        this.updateTempBGM();
    }

    public void updateVolume() {
        for (MainMusic mainMusic : this.mainTrack) {
            mainMusic.updateVolume();
        }
        for (TempMusic tempMusic : this.tempTrack) {
            tempMusic.updateVolume();
        }
    }

    private void updateBGM() {
        Iterator<MainMusic> i = this.mainTrack.iterator();
        while (i.hasNext()) {
            MainMusic e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
    }

    private void updateTempBGM() {
        Iterator<TempMusic> i = this.tempTrack.iterator();
        while (i.hasNext()) {
            TempMusic e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
    }

    public void fadeOutTempBGM() {
        for (TempMusic tempMusic : this.tempTrack) {
            if (tempMusic.isFadingOut) continue;
            tempMusic.fadeOut();
        }
        for (MainMusic mainMusic : this.mainTrack) {
            mainMusic.unsilence();
        }
    }

    public void justFadeOutTempBGM() {
        for (TempMusic m : this.tempTrack) {
            if (m.isFadingOut) continue;
            m.fadeOut();
        }
    }

    public void playTempBGM(String key) {
        if (key != null) {
            logger.info("Playing " + key);
            this.tempTrack.add(new TempMusic(key, false));
            for (MainMusic m : this.mainTrack) {
                m.silence();
            }
        }
    }

    public void playTempBgmInstantly(String key) {
        if (key != null) {
            logger.info("Playing " + key);
            this.tempTrack.add(new TempMusic(key, true));
            for (MainMusic m : this.mainTrack) {
                m.silenceInstantly();
            }
        }
    }

    public void precacheTempBgm(String key) {
        if (key != null) {
            logger.info("Pre-caching " + key);
            this.tempTrack.add(new TempMusic(key, true, true, true));
        }
    }

    public void playPrecachedTempBgm() {
        if (!this.tempTrack.isEmpty()) {
            this.tempTrack.get(0).playPrecached();
            for (MainMusic m : this.mainTrack) {
                m.silenceInstantly();
            }
        }
    }

    public void playTempBgmInstantly(String key, boolean loop) {
        if (key != null) {
            logger.info("Playing " + key);
            this.tempTrack.add(new TempMusic(key, true, loop));
            for (MainMusic m : this.mainTrack) {
                m.silenceInstantly();
            }
        }
    }

    public void changeBGM(String key) {
        this.mainTrack.add(new MainMusic(key));
    }

    public void fadeOutBGM() {
        for (MainMusic m : this.mainTrack) {
            if (m.isFadingOut) continue;
            m.fadeOut();
        }
    }

    public void silenceBGM() {
        for (MainMusic m : this.mainTrack) {
            m.silence();
        }
    }

    public void silenceBGMInstantly() {
        for (MainMusic m : this.mainTrack) {
            m.silenceInstantly();
        }
    }

    public void unsilenceBGM() {
        for (MainMusic m : this.mainTrack) {
            m.unsilence();
        }
    }

    public void silenceTempBgmInstantly() {
        for (TempMusic m : this.tempTrack) {
            m.silenceInstantly();
            m.isFadingOut = true;
        }
    }

    public void dispose() {
        for (MainMusic mainMusic : this.mainTrack) {
            mainMusic.kill();
        }
        for (TempMusic tempMusic : this.tempTrack) {
            tempMusic.kill();
        }
    }

    public void fadeAll() {
        for (MainMusic mainMusic : this.mainTrack) {
            mainMusic.fadeOut();
        }
        for (TempMusic tempMusic : this.tempTrack) {
            tempMusic.fadeOut();
        }
    }
}

