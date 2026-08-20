/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.trials;

import com.megacrit.cardcrawl.trials.AbstractTrial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StarterDeckTrial
extends AbstractTrial {
    @Override
    public List<String> extraStartingRelicIDs() {
        return Collections.singletonList("Busted Crown");
    }

    @Override
    public ArrayList<String> dailyModIDs() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Binary");
        return retVal;
    }
}

