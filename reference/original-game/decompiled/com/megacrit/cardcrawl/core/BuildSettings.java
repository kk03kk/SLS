/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.core;

import com.megacrit.cardcrawl.core.BuildSettingsException;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

class BuildSettings {
    private final Properties prop = new Properties();
    public static final String defaultFilename = "build.properties";

    BuildSettings(Reader reader) throws IOException {
        this.prop.load(reader);
    }

    String getDistributor() throws BuildSettingsException {
        String distributor = this.prop.getProperty("distributor");
        if (distributor != null) {
            return distributor;
        }
        throw new BuildSettingsException("The key 'distributor' is null in file=build.properties");
    }
}

