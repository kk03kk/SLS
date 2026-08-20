/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.trials;

import com.megacrit.cardcrawl.trials.AbstractTrial;
import java.util.ArrayList;

public class CursedTrial
extends AbstractTrial {
    @Override
    public ArrayList<String> dailyModIDs() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Cursed Run");
        return retVal;
    }
}

