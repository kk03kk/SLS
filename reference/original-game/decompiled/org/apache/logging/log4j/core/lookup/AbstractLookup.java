/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.lookup.StrLookup;

public abstract class AbstractLookup
implements StrLookup {
    @Override
    public String lookup(String key) {
        return this.lookup(null, key);
    }
}

