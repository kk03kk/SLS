/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.localization;

import com.megacrit.cardcrawl.localization.LocalizedStrings;

public class PowerStrings {
    public String NAME;
    public String[] DESCRIPTIONS;

    public static PowerStrings getMockPowerString() {
        PowerStrings retVal = new PowerStrings();
        retVal.NAME = "[MISSING_NAME]";
        retVal.DESCRIPTIONS = LocalizedStrings.createMockStringArray(6);
        return retVal;
    }
}

