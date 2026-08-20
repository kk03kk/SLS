/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.localization;

public class CardStrings {
    public String NAME;
    public String DESCRIPTION;
    public String UPGRADE_DESCRIPTION;
    public String[] EXTENDED_DESCRIPTION;

    public static CardStrings getMockCardString() {
        CardStrings retVal = new CardStrings();
        retVal.NAME = "[MISSING_TITLE]";
        retVal.DESCRIPTION = "[MISSING_DESCRIPTION]";
        retVal.UPGRADE_DESCRIPTION = "[MISSING_DESCRIPTION+]";
        retVal.EXTENDED_DESCRIPTION = new String[]{"[MISSING_0]", "[MISSING_1]", "[MISSING_2]"};
        return retVal;
    }
}

