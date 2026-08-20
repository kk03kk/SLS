/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.TimeZone;

public class FTPFile
implements Serializable {
    private static final long serialVersionUID = 9010790363003271996L;
    public static final int FILE_TYPE = 0;
    public static final int DIRECTORY_TYPE = 1;
    public static final int SYMBOLIC_LINK_TYPE = 2;
    public static final int UNKNOWN_TYPE = 3;
    public static final int USER_ACCESS = 0;
    public static final int GROUP_ACCESS = 1;
    public static final int WORLD_ACCESS = 2;
    public static final int READ_PERMISSION = 0;
    public static final int WRITE_PERMISSION = 1;
    public static final int EXECUTE_PERMISSION = 2;
    private int _type;
    private int _hardLinkCount;
    private long _size;
    private String _rawListing;
    private String _user;
    private String _group;
    private String _name;
    private String _link;
    private Calendar _date;
    private final boolean[][] _permissions;

    public FTPFile() {
        this._permissions = new boolean[3][3];
        this._type = 3;
        this._hardLinkCount = 0;
        this._size = -1L;
        this._user = "";
        this._group = "";
        this._date = null;
        this._name = null;
    }

    FTPFile(String rawListing) {
        this._permissions = null;
        this._rawListing = rawListing;
        this._type = 3;
        this._hardLinkCount = 0;
        this._size = -1L;
        this._user = "";
        this._group = "";
        this._date = null;
        this._name = null;
    }

    public void setRawListing(String rawListing) {
        this._rawListing = rawListing;
    }

    public String getRawListing() {
        return this._rawListing;
    }

    public boolean isDirectory() {
        return this._type == 1;
    }

    public boolean isFile() {
        return this._type == 0;
    }

    public boolean isSymbolicLink() {
        return this._type == 2;
    }

    public boolean isUnknown() {
        return this._type == 3;
    }

    public boolean isValid() {
        return this._permissions != null;
    }

    public void setType(int type) {
        this._type = type;
    }

    public int getType() {
        return this._type;
    }

    public void setName(String name) {
        this._name = name;
    }

    public String getName() {
        return this._name;
    }

    public void setSize(long size) {
        this._size = size;
    }

    public long getSize() {
        return this._size;
    }

    public void setHardLinkCount(int links) {
        this._hardLinkCount = links;
    }

    public int getHardLinkCount() {
        return this._hardLinkCount;
    }

    public void setGroup(String group) {
        this._group = group;
    }

    public String getGroup() {
        return this._group;
    }

    public void setUser(String user) {
        this._user = user;
    }

    public String getUser() {
        return this._user;
    }

    public void setLink(String link) {
        this._link = link;
    }

    public String getLink() {
        return this._link;
    }

    public void setTimestamp(Calendar date) {
        this._date = date;
    }

    public Calendar getTimestamp() {
        return this._date;
    }

    public void setPermission(int access, int permission, boolean value) {
        this._permissions[access][permission] = value;
    }

    public boolean hasPermission(int access, int permission) {
        if (this._permissions == null) {
            return false;
        }
        return this._permissions[access][permission];
    }

    public String toString() {
        return this.getRawListing();
    }

    public String toFormattedString() {
        return this.toFormattedString(null);
    }

    public String toFormattedString(String timezone) {
        if (!this.isValid()) {
            return "[Invalid: could not parse file entry]";
        }
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        sb.append(this.formatType());
        sb.append(this.permissionToString(0));
        sb.append(this.permissionToString(1));
        sb.append(this.permissionToString(2));
        fmt.format(" %4d", this.getHardLinkCount());
        fmt.format(" %-8s %-8s", this.getUser(), this.getGroup());
        fmt.format(" %8d", this.getSize());
        Calendar timestamp = this.getTimestamp();
        if (timestamp != null) {
            TimeZone newZone;
            if (timezone != null && !(newZone = TimeZone.getTimeZone(timezone)).equals(timestamp.getTimeZone())) {
                Date original = timestamp.getTime();
                Calendar newStamp = Calendar.getInstance(newZone);
                newStamp.setTime(original);
                timestamp = newStamp;
            }
            fmt.format(" %1$tY-%1$tm-%1$td", timestamp);
            if (timestamp.isSet(11)) {
                fmt.format(" %1$tH", timestamp);
                if (timestamp.isSet(12)) {
                    fmt.format(":%1$tM", timestamp);
                    if (timestamp.isSet(13)) {
                        fmt.format(":%1$tS", timestamp);
                        if (timestamp.isSet(14)) {
                            fmt.format(".%1$tL", timestamp);
                        }
                    }
                }
                fmt.format(" %1$tZ", timestamp);
            }
        }
        sb.append(' ');
        sb.append(this.getName());
        fmt.close();
        return sb.toString();
    }

    private char formatType() {
        switch (this._type) {
            case 0: {
                return '-';
            }
            case 1: {
                return 'd';
            }
            case 2: {
                return 'l';
            }
        }
        return '?';
    }

    private String permissionToString(int access) {
        StringBuilder sb = new StringBuilder();
        if (this.hasPermission(access, 0)) {
            sb.append('r');
        } else {
            sb.append('-');
        }
        if (this.hasPermission(access, 1)) {
            sb.append('w');
        } else {
            sb.append('-');
        }
        if (this.hasPermission(access, 2)) {
            sb.append('x');
        } else {
            sb.append('-');
        }
        return sb.toString();
    }
}

