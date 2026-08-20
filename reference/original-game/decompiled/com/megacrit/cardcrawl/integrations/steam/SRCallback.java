/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations.steam;

import com.codedisaster.steamworks.SteamAPICall;
import com.codedisaster.steamworks.SteamPublishedFileID;
import com.codedisaster.steamworks.SteamRemoteStorageCallback;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUGCHandle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SRCallback
implements SteamRemoteStorageCallback {
    private static final Logger logger = LogManager.getLogger(SRCallback.class.getName());

    @Override
    public void onFileShareResult(SteamUGCHandle fileHandle, String fileName, SteamResult result) {
        logger.info("The 'onFileShareResult' callback was called and returns: fileHandle=" + fileHandle.toString() + ", fileName=" + fileName + ", result=" + result.toString());
    }

    @Override
    public void onDownloadUGCResult(SteamUGCHandle fileHandle, SteamResult result) {
        logger.info("The 'onDownloadUGCResult' callback was called and returns: fileHandle=" + fileHandle.toString() + ", result=" + result.toString());
    }

    @Override
    public void onPublishFileResult(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result) {
        logger.info("The 'onPublishFileResult' callback was called and returns: publishedFileID=" + publishedFileID.toString() + ", needsToAcceptWLA=" + needsToAcceptWLA + ", result=" + result.toString());
    }

    @Override
    public void onUpdatePublishedFileResult(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result) {
        logger.info("The 'onUpdatePublishedFileResult' callback was called and returns: publishedFileID=" + publishedFileID.toString() + ", needsToAcceptWLA=" + needsToAcceptWLA + ", result=" + result.toString());
    }

    @Override
    public void onPublishedFileSubscribed(SteamPublishedFileID publishedFileID, int appID) {
    }

    @Override
    public void onPublishedFileUnsubscribed(SteamPublishedFileID publishedFileID, int appID) {
    }

    @Override
    public void onPublishedFileDeleted(SteamPublishedFileID publishedFileID, int appID) {
    }

    @Override
    public void onFileWriteAsyncComplete(SteamResult result) {
    }

    @Override
    public void onFileReadAsyncComplete(SteamAPICall fileReadAsync, SteamResult result, int offset, int read) {
    }
}

