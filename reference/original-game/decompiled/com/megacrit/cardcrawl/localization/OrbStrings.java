/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.localization;

import com.megacrit.cardcrawl.localization.LocalizedStrings;

public class OrbStrings {
    public String NAME;
    public String[] DESCRIPTION;

    public static OrbStrings getMockOrbString() {
        OrbStrings retVal = new OrbStrings();
        retVal.NAME = "[MISSING_NAME]";
        retVal.DESCRIPTION = LocalizedStrings.createMockStringArray(5);
        return retVal;
    }
}

