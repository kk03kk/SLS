/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.localization;

import com.megacrit.cardcrawl.localization.LocalizedStrings;

public class BlightStrings {
    public String NAME;
    public String[] DESCRIPTION;

    public static BlightStrings getBlightOrbString() {
        BlightStrings retVal = new BlightStrings();
        retVal.NAME = "[MISSING_NAME]";
        retVal.DESCRIPTION = LocalizedStrings.createMockStringArray(5);
        return retVal;
    }
}

