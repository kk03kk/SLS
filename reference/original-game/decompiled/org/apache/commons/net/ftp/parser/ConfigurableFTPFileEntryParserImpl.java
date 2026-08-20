/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.text.ParseException;
import java.util.Calendar;
import org.apache.commons.net.ftp.Configurable;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.parser.FTPTimestampParser;
import org.apache.commons.net.ftp.parser.FTPTimestampParserImpl;
import org.apache.commons.net.ftp.parser.RegexFTPFileEntryParserImpl;

public abstract class ConfigurableFTPFileEntryParserImpl
extends RegexFTPFileEntryParserImpl
implements Configurable {
    private final FTPTimestampParser timestampParser = new FTPTimestampParserImpl();

    public ConfigurableFTPFileEntryParserImpl(String regex) {
        super(regex);
    }

    public ConfigurableFTPFileEntryParserImpl(String regex, int flags) {
        super(regex, flags);
    }

    public Calendar parseTimestamp(String timestampStr) throws ParseException {
        return this.timestampParser.parseTimestamp(timestampStr);
    }

    @Override
    public void configure(FTPClientConfig config) {
        if (this.timestampParser instanceof Configurable) {
            FTPClientConfig defaultCfg = this.getDefaultConfiguration();
            if (config != null) {
                if (null == config.getDefaultDateFormatStr()) {
                    config.setDefaultDateFormatStr(defaultCfg.getDefaultDateFormatStr());
                }
                if (null == config.getRecentDateFormatStr()) {
                    config.setRecentDateFormatStr(defaultCfg.getRecentDateFormatStr());
                }
                ((Configurable)((Object)this.timestampParser)).configure(config);
            } else {
                ((Configurable)((Object)this.timestampParser)).configure(defaultCfg);
            }
        }
    }

    protected abstract FTPClientConfig getDefaultConfiguration();
}

