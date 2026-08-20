/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FTPFileFilters {
    public static final FTPFileFilter ALL = new FTPFileFilter(){

        @Override
        public boolean accept(FTPFile file) {
            return true;
        }
    };
    public static final FTPFileFilter NON_NULL = new FTPFileFilter(){

        @Override
        public boolean accept(FTPFile file) {
            return file != null;
        }
    };
    public static final FTPFileFilter DIRECTORIES = new FTPFileFilter(){

        @Override
        public boolean accept(FTPFile file) {
            return file != null && file.isDirectory();
        }
    };
}

