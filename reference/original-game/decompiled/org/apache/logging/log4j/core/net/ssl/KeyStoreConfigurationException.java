/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.net.ssl;

import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;

public class KeyStoreConfigurationException
extends StoreConfigurationException {
    private static final long serialVersionUID = 1L;

    public KeyStoreConfigurationException(Exception e) {
        super(e);
    }
}

