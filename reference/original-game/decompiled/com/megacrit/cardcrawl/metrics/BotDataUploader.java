/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.metrics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.ExceptionHandler;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BotDataUploader
implements Runnable {
    private static final Logger logger = LogManager.getLogger(BotDataUploader.class.getName());
    private Gson gson = new Gson();
    private String url = System.getenv("STS_DATA_UPLOAD_URL");
    private GameDataType type;
    private String header;
    private ArrayList<String> data;

    public void setValues(GameDataType type, String header, ArrayList<String> data) {
        this.type = type;
        this.header = header;
        this.data = data;
    }

    public void sendPost(HashMap<String, Serializable> eventData) {
        if (this.url == null) {
            return;
        }
        HashMap<String, Serializable> event = new HashMap<String, Serializable>();
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(hostname.getBytes());
        }
        catch (UnknownHostException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            ExceptionHandler.handleException(e, logger);
        }
        eventData.put("STS_DATA_UPLOAD_KEY", (Serializable)((Object)System.getenv("STS_DATA_UPLOAD_KEY")));
        event.putAll(eventData);
        String data = this.gson.toJson(event);
        logger.info("UPLOADING TO LEADER BOARD: " + data);
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest().method("POST").url(this.url).header("Content-Type", "application/json").header("Accept", "application/json").build();
        httpRequest.setContent(data);
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener(){

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                if (Settings.isDev) {
                    logger.info("Bot Data Upload: http request response: " + httpResponse.getStatus().getStatusCode() + " " + httpResponse.getResultAsString());
                }
            }

            @Override
            public void failed(Throwable t) {
                if (Settings.isDev) {
                    logger.info("Bot Data Upload: http request failed: " + t.toString());
                }
            }

            @Override
            public void cancelled() {
                if (Settings.isDev) {
                    logger.info("Bot Data Upload: http request cancelled.");
                }
            }
        });
    }

    private void sendData(String table, String header, ArrayList<String> entries) {
        HashMap<String, Serializable> data = new HashMap<String, Serializable>(entries.size() + 1);
        data.put("event type", (Serializable)((Object)(table + " data update")));
        data.put("header", (Serializable)((Object)header));
        data.put("data", entries);
        this.sendPost(data);
    }

    public static void uploadDataAsync(GameDataType type, String header, ArrayList<String> data) {
        BotDataUploader poster = new BotDataUploader();
        poster.setValues(type, header, data);
        Thread t = new Thread(poster);
        t.setName("LeaderboardPoster");
        t.start();
    }

    @Override
    public void run() {
        switch (this.type) {
            case CARD_DATA: {
                this.sendData("card", this.header, this.data);
                break;
            }
            case RELIC_DATA: {
                this.sendData("relic", this.header, this.data);
                break;
            }
            case ENEMY_DATA: {
                this.sendData("enemy", this.header, this.data);
                break;
            }
            case POTION_DATA: {
                this.sendData("potion", this.header, this.data);
                break;
            }
            case DAILY_MOD_DATA: {
                this.sendData("daily mod", this.header, this.data);
                break;
            }
            case BLIGHT_DATA: {
                this.sendData("blight", this.header, this.data);
                break;
            }
            case KEYWORD_DATA: {
                this.sendData("keywords", this.header, this.data);
                break;
            }
            default: {
                logger.info("Unspecified/deprecated LeaderboardPosterType: " + this.type.name() + " in run()");
            }
        }
    }

    public static void uploadKeywordData() {
        TreeMap<String, String> keywords = GameDictionary.keywords;
        if (keywords.isEmpty()) {
            GameDictionary.initialize();
        }
        ArrayList<String> data = new ArrayList<String>();
        for (String name : keywords.keySet()) {
            data.add(String.format("%s\t%s", name, keywords.get(name)));
        }
        BotDataUploader.uploadDataAsync(GameDataType.KEYWORD_DATA, "name\ttext", data);
    }

    public static enum GameDataType {
        BANDITS,
        CRASH_DATA,
        DAILY_DATA,
        DEMO_EMBARK,
        VICTORY_DATA,
        CARD_DATA,
        ENEMY_DATA,
        RELIC_DATA,
        POTION_DATA,
        DAILY_MOD_DATA,
        BLIGHT_DATA,
        KEYWORD_DATA;

    }
}

