/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeHelper {
    private static final Logger logger = LogManager.getLogger(TimeHelper.class.getName());
    public static boolean isTimeSet = false;
    private static long daySince1970;
    private static long timeServerTime;
    private static long endTimeMs;
    private static final float UPDATE_FREQ = -1.0f;
    private static float updateTimer;
    private static boolean offlineMode;
    private static DateFormat format;

    public static void setTime(long unixTime, boolean isOfflineMode) {
        if (!isTimeSet) {
            offlineMode = isOfflineMode;
            timeServerTime = unixTime;
            daySince1970 = timeServerTime / 86400L;
            logger.info("Setting time to: " + timeServerTime);
            endTimeMs = (daySince1970 + 1L) * 86400L * 1000L;
            logger.info("Day was set!");
            isTimeSet = true;
            format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        }
    }

    public static boolean isOfflineMode() {
        return offlineMode;
    }

    public static long daySince1970() {
        return daySince1970;
    }

    public static void update() {
        if (isTimeSet && (updateTimer -= Gdx.graphics.getDeltaTime()) < 0.0f) {
            updateTimer = -1.0f;
        }
    }

    public static String getTodayDate() {
        return format.format(new Date(System.currentTimeMillis()));
    }

    public static String getDate(long numDaysSince1970) {
        long unixTimestampMs = (daySince1970 - numDaysSince1970) * 86400L * 1000L;
        return format.format(new Date(System.currentTimeMillis() - unixTimestampMs));
    }

    public static String getTimeLeft() {
        if (endTimeMs - System.currentTimeMillis() < 0L) {
            endTimeMs += 86400000L;
            ++daySince1970;
        }
        long remainingSec = (endTimeMs - System.currentTimeMillis()) / 1000L;
        long hours = remainingSec / 3600L;
        long minutes = (remainingSec %= 3600L) / 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSec % 60L);
    }

    static {
        updateTimer = -1.0f;
        offlineMode = false;
        format = new SimpleDateFormat(CardCrawlGame.languagePack.getUIString((String)"DailyScreen").TEXT[17]);
    }
}

