/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

public enum FTPCmd {
    ABOR,
    ACCT,
    ALLO,
    APPE,
    CDUP,
    CWD,
    DELE,
    EPRT,
    EPSV,
    FEAT,
    HELP,
    LIST,
    MDTM,
    MFMT,
    MKD,
    MLSD,
    MLST,
    MODE,
    NLST,
    NOOP,
    PASS,
    PASV,
    PORT,
    PWD,
    QUIT,
    REIN,
    REST,
    RETR,
    RMD,
    RNFR,
    RNTO,
    SITE,
    SMNT,
    STAT,
    STOR,
    STOU,
    STRU,
    SYST,
    TYPE,
    USER;

    public static final FTPCmd ABORT;
    public static final FTPCmd ACCOUNT;
    public static final FTPCmd ALLOCATE;
    public static final FTPCmd APPEND;
    public static final FTPCmd CHANGE_TO_PARENT_DIRECTORY;
    public static final FTPCmd CHANGE_WORKING_DIRECTORY;
    public static final FTPCmd DATA_PORT;
    public static final FTPCmd DELETE;
    public static final FTPCmd FEATURES;
    public static final FTPCmd FILE_STRUCTURE;
    public static final FTPCmd GET_MOD_TIME;
    public static final FTPCmd LOGOUT;
    public static final FTPCmd MAKE_DIRECTORY;
    public static final FTPCmd MOD_TIME;
    public static final FTPCmd NAME_LIST;
    public static final FTPCmd PASSIVE;
    public static final FTPCmd PASSWORD;
    public static final FTPCmd PRINT_WORKING_DIRECTORY;
    public static final FTPCmd REINITIALIZE;
    public static final FTPCmd REMOVE_DIRECTORY;
    public static final FTPCmd RENAME_FROM;
    public static final FTPCmd RENAME_TO;
    public static final FTPCmd REPRESENTATION_TYPE;
    public static final FTPCmd RESTART;
    public static final FTPCmd RETRIEVE;
    public static final FTPCmd SET_MOD_TIME;
    public static final FTPCmd SITE_PARAMETERS;
    public static final FTPCmd STATUS;
    public static final FTPCmd STORE;
    public static final FTPCmd STORE_UNIQUE;
    public static final FTPCmd STRUCTURE_MOUNT;
    public static final FTPCmd SYSTEM;
    public static final FTPCmd TRANSFER_MODE;
    public static final FTPCmd USERNAME;

    public final String getCommand() {
        return this.name();
    }

    static {
        ABORT = ABOR;
        ACCOUNT = ACCT;
        ALLOCATE = ALLO;
        APPEND = APPE;
        CHANGE_TO_PARENT_DIRECTORY = CDUP;
        CHANGE_WORKING_DIRECTORY = CWD;
        DATA_PORT = PORT;
        DELETE = DELE;
        FEATURES = FEAT;
        FILE_STRUCTURE = STRU;
        GET_MOD_TIME = MDTM;
        LOGOUT = QUIT;
        MAKE_DIRECTORY = MKD;
        MOD_TIME = MDTM;
        NAME_LIST = NLST;
        PASSIVE = PASV;
        PASSWORD = PASS;
        PRINT_WORKING_DIRECTORY = PWD;
        REINITIALIZE = REIN;
        REMOVE_DIRECTORY = RMD;
        RENAME_FROM = RNFR;
        RENAME_TO = RNTO;
        REPRESENTATION_TYPE = TYPE;
        RESTART = REST;
        RETRIEVE = RETR;
        SET_MOD_TIME = MFMT;
        SITE_PARAMETERS = SITE;
        STATUS = STAT;
        STORE = STOR;
        STORE_UNIQUE = STOU;
        STRUCTURE_MOUNT = SMNT;
        SYSTEM = SYST;
        TRANSFER_MODE = MODE;
        USERNAME = USER;
    }
}

