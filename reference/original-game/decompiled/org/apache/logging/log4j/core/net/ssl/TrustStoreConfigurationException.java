/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.net.ssl;

import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;

public class TrustStoreConfigurationException
extends StoreConfigurationException {
    private static final long serialVersionUID = 1L;

    public TrustStoreConfigurationException(Exception e) {
        super(e);
    }
}

