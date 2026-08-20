/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.core.util.Clock;

/*
 * Multiple versions of this class in jar - see https://www.benf.org/other/cfr/multi-version-jar.html
 */
public final class SystemClock
implements Clock {
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}

