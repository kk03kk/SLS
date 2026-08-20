/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.map;

import com.megacrit.cardcrawl.map.MapEdge;
import java.util.Comparator;

class EdgeComparator
implements Comparator<MapEdge> {
    EdgeComparator() {
    }

    @Override
    public int compare(MapEdge e1, MapEdge e2) {
        return e1.compareTo(e2);
    }
}

