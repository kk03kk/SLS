/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.megacrit.cardcrawl.daily.TimeHelper;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeLookup {
    private static final Logger logger = LogManager.getLogger(TimeLookup.class.getName());
    public static volatile boolean isDone = false;
    private static final String timeServer = "https://hyi3lwrhf5.execute-api.us-east-1.amazonaws.com/prod/time";
    private static volatile AtomicInteger retryCount = new AtomicInteger(1);
    private static final int MAX_RETRY = 2;
    private static final int WAIT_TIME_CAP = 2;

    private static void makeHTTPReq(String url) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest().method("GET").url(url).header("Content-Type", "application/json").header("Accept", "application/json").header("User-Agent", "curl/7.43.0").timeout(5000).build();
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener(){

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String status = String.valueOf(httpResponse.getStatus().getStatusCode());
                String result = httpResponse.getResultAsString();
                if (!status.startsWith("2")) {
                    logger.info("Query to sts-time-server failed: status_code=" + status + " result=" + result);
                }
                logger.info("Time server response: " + result);
                long serverTime = Long.parseLong(result);
                isDone = true;
                TimeHelper.setTime(serverTime, false);
            }

            @Override
            public void failed(Throwable t) {
                logger.info("http request failed: " + t.toString());
                logger.info("retry count: " + retryCount);
                if (retryCount.get() > 2) {
                    logger.info("Failed to lookup time. Switching to OFFLINE MODE!");
                    long localTime = System.currentTimeMillis() / 1000L;
                    TimeHelper.setTime(localTime, true);
                    return;
                }
                long waitTime = (long)Math.pow(2.0, retryCount.get());
                logger.info("wait time: " + waitTime);
                if (waitTime > 2L) {
                    waitTime = 2L;
                }
                logger.info("Retry " + retryCount.get() + ": waiting " + waitTime + " seconds for time lookup");
                retryCount.getAndIncrement();
                try {
                    Thread.sleep(waitTime * 1000L);
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.info("Thread interrupted!");
                }
                TimeLookup.makeHTTPReq(TimeLookup.timeServer);
            }

            @Override
            public void cancelled() {
                logger.info("http request cancelled.");
            }
        });
    }

    public static void fetchDailyTimeAsync() {
        TimeLookup.makeHTTPReq(timeServer);
    }
}

