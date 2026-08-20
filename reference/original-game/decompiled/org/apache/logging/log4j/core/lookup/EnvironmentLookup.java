/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;

@Plugin(name="env", category="Lookup")
public class EnvironmentLookup
extends AbstractLookup {
    @Override
    public String lookup(LogEvent event, String key) {
        return System.getenv(key);
    }
}

