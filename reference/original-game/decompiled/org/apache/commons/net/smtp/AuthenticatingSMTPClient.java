/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.smtp;

import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SMTPSClient;
import org.apache.commons.net.util.Base64;

public class AuthenticatingSMTPClient
extends SMTPSClient {
    public AuthenticatingSMTPClient() {
    }

    public AuthenticatingSMTPClient(String protocol) {
        super(protocol);
    }

    public AuthenticatingSMTPClient(String proto, boolean implicit) {
        super(proto, implicit);
    }

    public AuthenticatingSMTPClient(String proto, boolean implicit, String encoding) {
        super(proto, implicit, encoding);
    }

    public AuthenticatingSMTPClient(boolean implicit, SSLContext ctx) {
        super(implicit, ctx);
    }

    public AuthenticatingSMTPClient(String protocol, String encoding) {
        super(protocol, false, encoding);
    }

    public int ehlo(String hostname) throws IOException {
        return this.sendCommand(15, hostname);
    }

    public boolean elogin(String hostname) throws IOException {
        return SMTPReply.isPositiveCompletion(this.ehlo(hostname));
    }

    public boolean elogin() throws IOException {
        InetAddress host = this.getLocalAddress();
        String name = host.getHostName();
        if (name == null) {
            return false;
        }
        return SMTPReply.isPositiveCompletion(this.ehlo(name));
    }

    public int[] getEnhancedReplyCode() {
        String reply = this.getReplyString().substring(4);
        String[] parts = reply.substring(0, reply.indexOf(32)).split("\\.");
        int[] res = new int[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            res[i] = Integer.parseInt(parts[i]);
        }
        return res;
    }

    public boolean auth(AUTH_METHOD method, String username, String password) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        if (!SMTPReply.isPositiveIntermediate(this.sendCommand(14, AUTH_METHOD.getAuthName(method)))) {
            return false;
        }
        if (method.equals((Object)AUTH_METHOD.PLAIN)) {
            return SMTPReply.isPositiveCompletion(this.sendCommand(Base64.encodeBase64StringUnChunked(("\u0000" + username + "\u0000" + password).getBytes(this.getCharset()))));
        }
        if (method.equals((Object)AUTH_METHOD.CRAM_MD5)) {
            byte[] serverChallenge = Base64.decodeBase64(this.getReplyString().substring(4).trim());
            Mac hmac_md5 = Mac.getInstance("HmacMD5");
            hmac_md5.init(new SecretKeySpec(password.getBytes(this.getCharset()), "HmacMD5"));
            byte[] hmacResult = this._convertToHexString(hmac_md5.doFinal(serverChallenge)).getBytes(this.getCharset());
            byte[] usernameBytes = username.getBytes(this.getCharset());
            byte[] toEncode = new byte[usernameBytes.length + 1 + hmacResult.length];
            System.arraycopy(usernameBytes, 0, toEncode, 0, usernameBytes.length);
            toEncode[usernameBytes.length] = 32;
            System.arraycopy(hmacResult, 0, toEncode, usernameBytes.length + 1, hmacResult.length);
            return SMTPReply.isPositiveCompletion(this.sendCommand(Base64.encodeBase64StringUnChunked(toEncode)));
        }
        if (method.equals((Object)AUTH_METHOD.LOGIN)) {
            if (!SMTPReply.isPositiveIntermediate(this.sendCommand(Base64.encodeBase64StringUnChunked(username.getBytes(this.getCharset()))))) {
                return false;
            }
            return SMTPReply.isPositiveCompletion(this.sendCommand(Base64.encodeBase64StringUnChunked(password.getBytes(this.getCharset()))));
        }
        if (method.equals((Object)AUTH_METHOD.XOAUTH)) {
            return SMTPReply.isPositiveIntermediate(this.sendCommand(Base64.encodeBase64StringUnChunked(username.getBytes(this.getCharset()))));
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
        PLAIN,
        CRAM_MD5,
        LOGIN,
        XOAUTH;


        public static final String getAuthName(AUTH_METHOD method) {
            if (method.equals((Object)PLAIN)) {
                return "PLAIN";
            }
            if (method.equals((Object)CRAM_MD5)) {
                return "CRAM-MD5";
            }
            if (method.equals((Object)LOGIN)) {
                return "LOGIN";
            }
            if (method.equals((Object)XOAUTH)) {
                return "XOAUTH";
            }
            return null;
        }
    }
}

