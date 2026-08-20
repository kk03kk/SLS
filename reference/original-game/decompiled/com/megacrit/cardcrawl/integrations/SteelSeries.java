/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import java.io.BufferedReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SteelSeries {
    private final Logger logger = LogManager.getLogger(SteelSeries.class.getName());
    private final String gameName = "SLAY_THE_SPIRE";
    public Boolean isEnabled;
    private String url;
    private long timeAtLastHealthcheck = 0L;

    public SteelSeries() {
        String _url;
        String program_data = System.getenv("PROGRAMDATA");
        Path winPath = Paths.get(program_data + "/SteelSeries/SteelSeries Engine 3/coreProps.json", new String[0]);
        Path macPath = Paths.get("/Library/Application Support/SteelSeries Engine 3/coreProps.json", new String[0]);
        Boolean winExists = Files.exists(winPath, new LinkOption[0]);
        Boolean macExists = Files.exists(macPath, new LinkOption[0]);
        this.isEnabled = winExists != false || macExists != false;
        this.logger.info("enabled=" + this.isEnabled);
        if (!this.isEnabled.booleanValue()) {
            return;
        }
        String string = _url = winExists != false ? this.getUrl(winPath) : this.getUrl(macPath);
        if (_url != null) {
            this.url = "http://" + _url;
        } else {
            this.logger.info("ERROR: url is null!");
        }
        this.register();
    }

    private String getUrl(Path path) {
        Gson gson = new Gson();
        try {
            BufferedReader reader = Files.newBufferedReader(path);
            Map map = gson.fromJson((Reader)reader, Map.class);
            ((Reader)reader).close();
            return (String)map.get("address");
        }
        catch (Exception e) {
            e.printStackTrace();
            this.isEnabled = false;
            return null;
        }
    }

    public void update() {
        if (System.currentTimeMillis() - this.timeAtLastHealthcheck > 14000L) {
            this.doHealthCheck();
            this.timeAtLastHealthcheck = System.currentTimeMillis();
        }
    }

    private void doHealthCheck() {
        if (!this.isEnabled.booleanValue()) {
            return;
        }
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("game", "SLAY_THE_SPIRE");
        this.sendPost(this.url + "/game_heartbeat", data, new Net.HttpResponseListener(){

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
            }

            @Override
            public void failed(Throwable t) {
                SteelSeries.this.logger.info("Healthcheck failed.");
                SteelSeries.this.isEnabled = false;
            }

            @Override
            public void cancelled() {
            }
        });
    }

    private void register() {
        if (!this.isEnabled.booleanValue()) {
            return;
        }
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("game", "SLAY_THE_SPIRE");
        data.put("game_display_name", "Slay the Spire");
        data.put("developer", "MEGACRIT");
        this.sendPost(this.url + "/game_metadata", data, new Net.HttpResponseListener(){

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                SteelSeries.this.create_event_handler();
            }

            @Override
            public void failed(Throwable t) {
                SteelSeries.this.logger.info("Steel Series service not running.");
                SteelSeries.this.isEnabled = false;
            }

            @Override
            public void cancelled() {
                SteelSeries.this.logger.info("http request cancelled.");
            }
        });
    }

    private Map<String, Object> create_event_map(AbstractPlayer.PlayerClass character, Map<String, Integer> color) {
        HashMap<String, Integer> c1 = new HashMap<String, Integer>();
        c1.put("red", 0);
        c1.put("green", 0);
        c1.put("blue", 0);
        HashMap<String, Map<String, Integer>> gradient = new HashMap<String, Map<String, Integer>>();
        gradient.put("zero", c1);
        gradient.put("hundred", color);
        HashMap<String, HashMap<String, Map<String, Integer>>> colorConfig = new HashMap<String, HashMap<String, Map<String, Integer>>>();
        colorConfig.put("gradient", gradient);
        HashMap<String, Object> keyboardHandler = new HashMap<String, Object>();
        keyboardHandler.put("device-type", "keyboard");
        keyboardHandler.put("zone", "all");
        keyboardHandler.put("color", colorConfig);
        keyboardHandler.put("mode", "percent");
        HashMap<String, Object> mouseHandler = new HashMap<String, Object>();
        mouseHandler.put("device-type", "mouse");
        mouseHandler.put("zone", "all");
        mouseHandler.put("color", colorConfig);
        mouseHandler.put("mode", "percent");
        ArrayList<HashMap<String, Object>> handlers = new ArrayList<HashMap<String, Object>>();
        handlers.add(keyboardHandler);
        handlers.add(mouseHandler);
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("game", "SLAY_THE_SPIRE");
        data.put("event", character.toString());
        data.put("min_value", 0);
        data.put("max_value", 100);
        data.put("icon_id", 0);
        data.put("handlers", handlers);
        return data;
    }

    private Net.HttpResponseListener newEventHandlerListener() {
        return new Net.HttpResponseListener(){

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
            }

            @Override
            public void failed(Throwable t) {
                SteelSeries.this.logger.info("Steel Series service not running.");
                SteelSeries.this.isEnabled = false;
            }

            @Override
            public void cancelled() {
                SteelSeries.this.logger.info("http request cancelled.");
            }
        };
    }

    private void create_event_handler() {
        if (!this.isEnabled.booleanValue()) {
            return;
        }
        HashMap<String, Integer> ironclad_color = new HashMap<String, Integer>();
        ironclad_color.put("red", 255);
        ironclad_color.put("green", 0);
        ironclad_color.put("blue", 0);
        Map<String, Object> eventMap = this.create_event_map(AbstractPlayer.PlayerClass.IRONCLAD, ironclad_color);
        this.sendPost(this.url + "/bind_game_event", eventMap, this.newEventHandlerListener());
        HashMap<String, Integer> silent_color = new HashMap<String, Integer>();
        silent_color.put("red", 0);
        silent_color.put("green", 255);
        silent_color.put("blue", 0);
        eventMap = this.create_event_map(AbstractPlayer.PlayerClass.THE_SILENT, silent_color);
        this.sendPost(this.url + "/bind_game_event", eventMap, this.newEventHandlerListener());
        HashMap<String, Integer> defect_color = new HashMap<String, Integer>();
        defect_color.put("red", 0);
        defect_color.put("green", 0);
        defect_color.put("blue", 255);
        eventMap = this.create_event_map(AbstractPlayer.PlayerClass.DEFECT, defect_color);
        this.sendPost(this.url + "/bind_game_event", eventMap, this.newEventHandlerListener());
        HashMap<String, Integer> watcher_color = new HashMap<String, Integer>();
        watcher_color.put("red", 148);
        watcher_color.put("green", 0);
        watcher_color.put("blue", 211);
        eventMap = this.create_event_map(AbstractPlayer.PlayerClass.WATCHER, watcher_color);
        this.sendPost(this.url + "/bind_game_event", eventMap, this.newEventHandlerListener());
    }

    public void event_character_chosen(AbstractPlayer.PlayerClass character) {
        if (!this.isEnabled.booleanValue()) {
            return;
        }
        HashMap<String, Integer> value = new HashMap<String, Integer>();
        value.put("value", 100);
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("game", "SLAY_THE_SPIRE");
        data.put("event", character.toString());
        data.put("data", value);
        this.sendPost(this.url + "/game_event", data, new Net.HttpResponseListener(){

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
            }

            @Override
            public void failed(Throwable t) {
                SteelSeries.this.logger.info("Steel Series service not running.");
                SteelSeries.this.isEnabled = false;
            }

            @Override
            public void cancelled() {
                SteelSeries.this.logger.info("http request cancelled.");
            }
        });
    }

    private void sendPost(String url, Map<String, Object> data, Net.HttpResponseListener listener) {
        Gson gson = new Gson();
        String content = gson.toJson(data);
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest().method("POST").url(url).header("Content-Type", "application/json").header("Accept", "application/json").header("User-Agent", "sts/" + CardCrawlGame.TRUE_VERSION_NUM).timeout(1).build();
        httpRequest.setContent(content);
        Gdx.net.sendHttpRequest(httpRequest, listener);
    }
}

