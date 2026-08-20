/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.util;

import java.net.URLConnection;

public interface AuthorizationProvider {
    public void addAuthorization(URLConnection var1);
}

