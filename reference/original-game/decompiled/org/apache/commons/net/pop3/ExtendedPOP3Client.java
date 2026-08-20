/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.pop3;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.net.pop3.POP3SClient;
import org.apache.commons.net.util.Base64;

public class ExtendedPOP3Client
extends POP3SClient {
    public boolean auth(AUTH_METHOD method, String username, String password) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        if (this.sendCommand(13, method.getAuthName()) != 2) {
            return false;
        }
        switch (method) {
            case PLAIN: {
                return this.sendCommand(new String(Base64.encodeBase64(("\u0000" + username + "\u0000" + password).getBytes(this.getCharset())), this.getCharset())) == 0;
            }
            case CRAM_MD5: {
                byte[] serverChallenge = Base64.decodeBase64(this.getReplyString().substring(2).trim());
                Mac hmac_md5 = Mac.getInstance("HmacMD5");
                hmac_md5.init(new SecretKeySpec(password.getBytes(this.getCharset()), "HmacMD5"));
                byte[] hmacResult = this._convertToHexString(hmac_md5.doFinal(serverChallenge)).getBytes(this.getCharset());
                byte[] usernameBytes = username.getBytes(this.getCharset());
                byte[] toEncode = new byte[usernameBytes.length + 1 + hmacResult.length];
                System.arraycopy(usernameBytes, 0, toEncode, 0, usernameBytes.length);
                toEncode[usernameBytes.length] = 32;
                System.arraycopy(hmacResult, 0, toEncode, usernameBytes.length + 1, hmacResult.length);
                return this.sendCommand(Base64.encodeBase64StringUnChunked(toEncode)) == 0;
            }
        }
        return false;
    }

    private String _convertToHexString(byte[] a) {
        StringBuilder result = new StringBuilder(a.length * 2);
        for (byte element : a) {
            if ((element & 0xFF) <= 15) {
                result.append("0");
            }
            result.append(Integer.toHexString(element & 0xFF));
        }
        return result.toString();
    }

    public static enum AUTH_METHOD {
        PLAIN("PLAIN"),
        CRAM_MD5("CRAM-MD5");

        private final String methodName;

        private AUTH_METHOD(String methodName) {
            this.methodName = methodName;
        }

        public final String getAuthName() {
            return this.methodName;
        }
    }
}

