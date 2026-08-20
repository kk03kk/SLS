/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.imap;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import org.apache.commons.net.imap.IMAP;
import org.apache.commons.net.imap.IMAPCommand;
import org.apache.commons.net.imap.IMAPReply;
import org.apache.commons.net.imap.IMAPSClient;
import org.apache.commons.net.util.Base64;

public class AuthenticatingIMAPClient
extends IMAPSClient {
    public AuthenticatingIMAPClient() {
        this("TLS", false);
    }

    public AuthenticatingIMAPClient(boolean implicit) {
        this("TLS", implicit);
    }

    public AuthenticatingIMAPClient(String proto) {
        this(proto, false);
    }

    public AuthenticatingIMAPClient(String proto, boolean implicit) {
        this(proto, implicit, null);
    }

    public AuthenticatingIMAPClient(String proto, boolean implicit, SSLContext ctx) {
        super(proto, implicit, ctx);
    }

    public AuthenticatingIMAPClient(boolean implicit, SSLContext ctx) {
        this("TLS", implicit, ctx);
    }

    public AuthenticatingIMAPClient(SSLContext context) {
        this(false, context);
    }

    public boolean authenticate(AUTH_METHOD method, String username, String password) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        return this.auth(method, username, password);
    }

    public boolean auth(AUTH_METHOD method, String username, String password) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        if (!IMAPReply.isContinuation(this.sendCommand(IMAPCommand.AUTHENTICATE, method.getAuthName()))) {
            return false;
        }
        switch (method) {
            case PLAIN: {
                int result = this.sendData(Base64.encodeBase64StringUnChunked(("\u0000" + username + "\u0000" + password).getBytes(this.getCharset())));
                if (result == 0) {
                    this.setState(IMAP.IMAPState.AUTH_STATE);
                }
                return result == 0;
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
                int result = this.sendData(Base64.encodeBase64StringUnChunked(toEncode));
                if (result == 0) {
                    this.setState(IMAP.IMAPState.AUTH_STATE);
                }
                return result == 0;
            }
            case LOGIN: {
                if (this.sendData(Base64.encodeBase64StringUnChunked(username.getBytes(this.getCharset()))) != 3) {
                    return false;
                }
                int result = this.sendData(Base64.encodeBase64StringUnChunked(password.getBytes(this.getCharset())));
                if (result == 0) {
                    this.setState(IMAP.IMAPState.AUTH_STATE);
                }
                return result == 0;
            }
            case XOAUTH: {
                int result = this.sendData(username);
                if (result == 0) {
                    this.setState(IMAP.IMAPState.AUTH_STATE);
                }
                return result == 0;
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
        CRAM_MD5("CRAM-MD5"),
        LOGIN("LOGIN"),
        XOAUTH("XOAUTH");

        private final String authName;

        private AUTH_METHOD(String name) {
            this.authName = name;
        }

        public final String getAuthName() {
            return this.authName;
        }
    }
}

