/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.parser.VMSFTPEntryParser;

public class VMSVersioningFTPEntryParser
extends VMSFTPEntryParser {
    private final Pattern _preparse_pattern_;
    private static final String PRE_PARSE_REGEX = "(.*?);([0-9]+)\\s*.*";

    public VMSVersioningFTPEntryParser() {
        this((FTPClientConfig)null);
    }

    public VMSVersioningFTPEntryParser(FTPClientConfig config) {
        this.configure(config);
        try {
            this._preparse_pattern_ = Pattern.compile(PRE_PARSE_REGEX);
        }
        catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException("Unparseable regex supplied:  (.*?);([0-9]+)\\s*.*");
        }
    }

    @Override
    public List<String> preParse(List<String> original) {
        Integer existing;
        Integer nv;
        String version;
        String name;
        Matcher _preparse_matcher_;
        MatchResult result;
        String entry;
        HashMap<String, Integer> existingEntries = new HashMap<String, Integer>();
        ListIterator<String> iter = original.listIterator();
        while (iter.hasNext()) {
            entry = iter.next().trim();
            result = null;
            _preparse_matcher_ = this._preparse_pattern_.matcher(entry);
            if (!_preparse_matcher_.matches()) continue;
            result = _preparse_matcher_.toMatchResult();
            name = result.group(1);
            version = result.group(2);
            nv = Integer.valueOf(version);
            existing = (Integer)existingEntries.get(name);
            if (null != existing && nv < existing) {
                iter.remove();
                continue;
            }
            existingEntries.put(name, nv);
        }
        while (iter.hasPrevious()) {
            entry = iter.previous().trim();
            result = null;
            _preparse_matcher_ = this._preparse_pattern_.matcher(entry);
            if (!_preparse_matcher_.matches()) continue;
            result = _preparse_matcher_.toMatchResult();
            name = result.group(1);
            version = result.group(2);
            nv = Integer.valueOf(version);
            existing = (Integer)existingEntries.get(name);
            if (null == existing || nv >= existing) continue;
            iter.remove();
        }
        return original;
    }

    @Override
    protected boolean isVersioning() {
        return true;
    }
}

