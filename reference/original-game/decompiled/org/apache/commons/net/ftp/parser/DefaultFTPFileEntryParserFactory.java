/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.net.ftp.Configurable;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.apache.commons.net.ftp.parser.CompositeFileEntryParser;
import org.apache.commons.net.ftp.parser.FTPFileEntryParserFactory;
import org.apache.commons.net.ftp.parser.MVSFTPEntryParser;
import org.apache.commons.net.ftp.parser.MacOsPeterFTPEntryParser;
import org.apache.commons.net.ftp.parser.NTFTPEntryParser;
import org.apache.commons.net.ftp.parser.NetwareFTPEntryParser;
import org.apache.commons.net.ftp.parser.OS2FTPEntryParser;
import org.apache.commons.net.ftp.parser.OS400FTPEntryParser;
import org.apache.commons.net.ftp.parser.ParserInitializationException;
import org.apache.commons.net.ftp.parser.UnixFTPEntryParser;
import org.apache.commons.net.ftp.parser.VMSVersioningFTPEntryParser;

public class DefaultFTPFileEntryParserFactory
implements FTPFileEntryParserFactory {
    private static final String JAVA_IDENTIFIER = "\\p{javaJavaIdentifierStart}(\\p{javaJavaIdentifierPart})*";
    private static final String JAVA_QUALIFIED_NAME = "(\\p{javaJavaIdentifierStart}(\\p{javaJavaIdentifierPart})*\\.)+\\p{javaJavaIdentifierStart}(\\p{javaJavaIdentifierPart})*";
    private static final Pattern JAVA_QUALIFIED_NAME_PATTERN = Pattern.compile("(\\p{javaJavaIdentifierStart}(\\p{javaJavaIdentifierPart})*\\.)+\\p{javaJavaIdentifierStart}(\\p{javaJavaIdentifierPart})*");

    @Override
    public FTPFileEntryParser createFileEntryParser(String key) {
        if (key == null) {
            throw new ParserInitializationException("Parser key cannot be null");
        }
        return this.createFileEntryParser(key, null);
    }

    private FTPFileEntryParser createFileEntryParser(String key, FTPClientConfig config) {
        FTPFileEntryParser parser = null;
        if (JAVA_QUALIFIED_NAME_PATTERN.matcher(key).matches()) {
            try {
                Class<?> parserClass = Class.forName(key);
                try {
                    parser = (FTPFileEntryParser)parserClass.newInstance();
                }
                catch (ClassCastException e) {
                    throw new ParserInitializationException(parserClass.getName() + " does not implement the interface " + "org.apache.commons.net.ftp.FTPFileEntryParser.", e);
                }
                catch (Exception e) {
                    throw new ParserInitializationException("Error initializing parser", e);
                }
                catch (ExceptionInInitializerError e) {
                    throw new ParserInitializationException("Error initializing parser", e);
                }
            }
            catch (ClassNotFoundException e) {
                // empty catch block
            }
        }
        if (parser == null) {
            String ukey = key.toUpperCase(Locale.ENGLISH);
            if (ukey.indexOf("UNIX") >= 0) {
                parser = new UnixFTPEntryParser(config, false);
            } else if (ukey.indexOf("UNIX_LTRIM") >= 0) {
                parser = new UnixFTPEntryParser(config, true);
            } else if (ukey.indexOf("VMS") >= 0) {
                parser = new VMSVersioningFTPEntryParser(config);
            } else if (ukey.indexOf("WINDOWS") >= 0) {
                parser = this.createNTFTPEntryParser(config);
            } else if (ukey.indexOf("OS/2") >= 0) {
                parser = new OS2FTPEntryParser(config);
            } else if (ukey.indexOf("OS/400") >= 0 || ukey.indexOf("AS/400") >= 0) {
                parser = this.createOS400FTPEntryParser(config);
            } else if (ukey.indexOf("MVS") >= 0) {
                parser = new MVSFTPEntryParser();
            } else if (ukey.indexOf("NETWARE") >= 0) {
                parser = new NetwareFTPEntryParser(config);
            } else if (ukey.indexOf("MACOS PETER") >= 0) {
                parser = new MacOsPeterFTPEntryParser(config);
            } else if (ukey.indexOf("TYPE: L8") >= 0) {
                parser = new UnixFTPEntryParser(config);
            } else {
                throw new ParserInitializationException("Unknown parser type: " + key);
            }
        }
        if (parser instanceof Configurable) {
            ((Configurable)((Object)parser)).configure(config);
        }
        return parser;
    }

    @Override
    public FTPFileEntryParser createFileEntryParser(FTPClientConfig config) throws ParserInitializationException {
        String key = config.getServerSystemKey();
        return this.createFileEntryParser(key, config);
    }

    public FTPFileEntryParser createUnixFTPEntryParser() {
        return new UnixFTPEntryParser();
    }

    public FTPFileEntryParser createVMSVersioningFTPEntryParser() {
        return new VMSVersioningFTPEntryParser();
    }

    public FTPFileEntryParser createNetwareFTPEntryParser() {
        return new NetwareFTPEntryParser();
    }

    public FTPFileEntryParser createNTFTPEntryParser() {
        return this.createNTFTPEntryParser(null);
    }

    private FTPFileEntryParser createNTFTPEntryParser(FTPClientConfig config) {
        if (config != null && "WINDOWS".equals(config.getServerSystemKey())) {
            return new NTFTPEntryParser(config);
        }
        return new CompositeFileEntryParser(new FTPFileEntryParser[]{new NTFTPEntryParser(config), new UnixFTPEntryParser(config, config != null && "UNIX_LTRIM".equals(config.getServerSystemKey()))});
    }

    public FTPFileEntryParser createOS2FTPEntryParser() {
        return new OS2FTPEntryParser();
    }

    public FTPFileEntryParser createOS400FTPEntryParser() {
        return this.createOS400FTPEntryParser(null);
    }

    private FTPFileEntryParser createOS400FTPEntryParser(FTPClientConfig config) {
        if (config != null && "OS/400".equals(config.getServerSystemKey())) {
            return new OS400FTPEntryParser(config);
        }
        return new CompositeFileEntryParser(new FTPFileEntryParser[]{new OS400FTPEntryParser(config), new UnixFTPEntryParser(config, config != null && "UNIX_LTRIM".equals(config.getServerSystemKey()))});
    }

    public FTPFileEntryParser createMVSEntryParser() {
        return new MVSFTPEntryParser();
    }
}

