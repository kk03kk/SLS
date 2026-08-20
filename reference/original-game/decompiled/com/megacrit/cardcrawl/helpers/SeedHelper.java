/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.BadWordChecker;
import com.megacrit.cardcrawl.helpers.TrialHelper;
import com.megacrit.cardcrawl.random.Random;
import java.math.BigInteger;

public class SeedHelper {
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
    public static String cachedSeed = null;
    public static final int SEED_DEFAULT_LENGTH = SeedHelper.getString(Long.MIN_VALUE).length();

    public static void setSeed(String seedStr) {
        if (seedStr.isEmpty()) {
            Settings.seedSet = false;
            Settings.seed = null;
            Settings.specialSeed = null;
        } else {
            long seed = SeedHelper.getLong(seedStr);
            Settings.seedSet = true;
            Settings.seed = seed;
            Settings.specialSeed = null;
            Settings.isDailyRun = false;
            cachedSeed = null;
        }
    }

    public static String getUserFacingSeedString() {
        if (Settings.seed != null) {
            if (cachedSeed == null) {
                cachedSeed = SeedHelper.getString(Settings.seed);
            }
            return cachedSeed;
        }
        return "";
    }

    public static String getValidCharacter(String character, String textSoFar) {
        boolean isValid;
        if ((character = character.toUpperCase()).equals("O")) {
            character = "0";
        }
        if (isValid = CHARACTERS.contains(character)) {
            return character;
        }
        return null;
    }

    public static String sterilizeString(String raw) {
        raw = raw.trim().toUpperCase();
        String pattern = "([A-Z]*[0-9]*)*";
        if (raw.matches("([A-Z]*[0-9]*)*")) {
            return raw.replace("O", "0");
        }
        return "";
    }

    public static String getString(long seed) {
        StringBuilder bldr = new StringBuilder();
        BigInteger leftover = new BigInteger(Long.toUnsignedString(seed));
        BigInteger charCount = BigInteger.valueOf(CHARACTERS.length());
        while (!leftover.equals(BigInteger.ZERO)) {
            BigInteger remainder = leftover.remainder(charCount);
            leftover = leftover.divide(charCount);
            int charIndex = remainder.intValue();
            char c = CHARACTERS.charAt(charIndex);
            bldr.insert(0, c);
        }
        return bldr.toString();
    }

    public static long getLong(String seedStr) {
        long total = 0L;
        seedStr = seedStr.toUpperCase().replaceAll("O", "0");
        for (int i = 0; i < seedStr.length(); ++i) {
            char toFind = seedStr.charAt(i);
            int remainder = CHARACTERS.indexOf(toFind);
            if (remainder == -1) {
                System.out.println("Character in seed is invalid: " + toFind);
            }
            total *= (long)CHARACTERS.length();
            total += (long)remainder;
        }
        return total;
    }

    public static long generateUnoffensiveSeed(Random rng) {
        String safeString = "fuck";
        while (BadWordChecker.containsBadWord(safeString) || TrialHelper.isTrialSeed(safeString)) {
            long possible = rng.randomLong();
            safeString = SeedHelper.getString(possible);
        }
        return SeedHelper.getLong(safeString);
    }
}

