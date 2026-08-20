/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class SSLContextUtils {
    private SSLContextUtils() {
    }

    public static SSLContext createSSLContext(String protocol, KeyManager keyManager, TrustManager trustManager) throws IOException {
        TrustManager[] trustManagerArray;
        KeyManager[] keyManagerArray;
        if (keyManager == null) {
            keyManagerArray = null;
        } else {
            KeyManager[] keyManagerArray2 = new KeyManager[1];
            keyManagerArray = keyManagerArray2;
            keyManagerArray2[0] = keyManager;
        }
        if (trustManager == null) {
            trustManagerArray = null;
        } else {
            TrustManager[] trustManagerArray2 = new TrustManager[1];
            trustManagerArray = trustManagerArray2;
            trustManagerArray2[0] = trustManager;
        }
        return SSLContextUtils.createSSLContext(protocol, keyManagerArray, trustManagerArray);
    }

    public static SSLContext createSSLContext(String protocol, KeyManager[] keyManagers, TrustManager[] trustManagers) throws IOException {
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance(protocol);
            ctx.init(keyManagers, trustManagers, null);
        }
        catch (GeneralSecurityException e) {
            IOException ioe = new IOException("Could not initialize SSL context");
            ioe.initCause(e);
            throw ioe;
        }
        return ctx;
    }
}

