/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.net.ssl.SSLSocket;

public class SSLSocketUtils {
    private SSLSocketUtils() {
    }

    public static boolean enableEndpointNameVerification(SSLSocket socket) {
        try {
            Object sslParams;
            Class<?> cls = Class.forName("javax.net.ssl.SSLParameters");
            Method setEndpointIdentificationAlgorithm = cls.getDeclaredMethod("setEndpointIdentificationAlgorithm", String.class);
            Method getSSLParameters = SSLSocket.class.getDeclaredMethod("getSSLParameters", new Class[0]);
            Method setSSLParameters = SSLSocket.class.getDeclaredMethod("setSSLParameters", cls);
            if (setEndpointIdentificationAlgorithm != null && getSSLParameters != null && setSSLParameters != null && (sslParams = getSSLParameters.invoke((Object)socket, new Object[0])) != null) {
                setEndpointIdentificationAlgorithm.invoke(sslParams, "HTTPS");
                setSSLParameters.invoke((Object)socket, sslParams);
                return true;
            }
        }
        catch (SecurityException e) {
        }
        catch (ClassNotFoundException e) {
        }
        catch (NoSuchMethodException e) {
        }
        catch (IllegalArgumentException e) {
        }
        catch (IllegalAccessException e) {
        }
        catch (InvocationTargetException invocationTargetException) {
            // empty catch block
        }
        return false;
    }
}

