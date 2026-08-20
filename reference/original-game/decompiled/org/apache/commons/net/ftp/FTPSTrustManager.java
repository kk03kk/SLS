/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

@Deprecated
public class FTPSTrustManager
implements X509TrustManager {
    private static final X509Certificate[] EMPTY_X509CERTIFICATE_ARRAY = new X509Certificate[0];

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        for (X509Certificate certificate : certificates) {
            certificate.checkValidity();
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return EMPTY_X509CERTIFICATE_ARRAY;
    }
}

