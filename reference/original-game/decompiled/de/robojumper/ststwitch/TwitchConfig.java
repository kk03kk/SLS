/*
 * Decompiled with CFR 0.152.
 */
package de.robojumper.ststwitch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TwitchConfig {
    private static final Logger logger = LogManager.getLogger(TwitchConfig.class.getName());
    public static final String configFilename = "twitchconfig.txt";
    private static final float DEFAULT_VOTE_TIMER = 35.0f;
    private boolean enabled;
    private String username;
    private String token;
    private String channel;
    private float timer;

    private TwitchConfig(Boolean enabled, String username, String token, String channel, float timer) {
        this.enabled = enabled;
        this.username = username;
        this.token = token;
        this.channel = channel;
        this.timer = timer;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static Optional<TwitchConfig> readConfig() {
        String username = "";
        String token = "";
        String channel = "";
        String unparsedTimer = "";
        String unparsedEnabled = "";
        try (FileInputStream in = new FileInputStream(configFilename);){
            Properties prop = new Properties();
            prop.load(in);
            if (!TwitchConfig.validateProps(prop)) {
                Optional<TwitchConfig> optional = Optional.empty();
                return optional;
            }
            username = prop.getProperty("user").trim();
            token = prop.getProperty("token").trim();
            channel = prop.getProperty("channel").trim();
            unparsedTimer = prop.getProperty("timer").trim();
            unparsedEnabled = prop.getProperty("enabled").trim();
        }
        catch (IOException e) {
            logger.info("Did not connect to twitch chat.");
            logger.error(e);
        }
        boolean enabled = Boolean.parseBoolean(unparsedEnabled);
        float timer = 35.0f;
        if (!unparsedTimer.equals("")) {
            float parsed = 0.0f;
            try {
                parsed = Float.parseFloat(unparsedTimer);
            }
            catch (NumberFormatException e) {
                logger.info("Could not parse timer=" + unparsedTimer);
            }
            if (parsed > 0.0f && parsed < 604800.0f) {
                timer = parsed;
            } else {
                logger.info("Twitch config 'timer' not a valid int between 0 and 604800, using default: 35.0");
            }
        } else {
            logger.info("Twitch config 'timer' value empty, using default: 35.0");
        }
        if (channel.equals("")) {
            channel = "#" + username.toLowerCase();
        } else if (!channel.startsWith("#")) {
            channel = "#" + channel;
        }
        channel = channel.toLowerCase();
        if (token.startsWith("oauth:")) return Optional.of(new TwitchConfig(enabled, username, token, channel, timer));
        token = "oauth:" + token;
        return Optional.of(new TwitchConfig(enabled, username, token, channel, timer));
    }

    private static boolean validateProps(Properties prop) {
        Set<String> propNames = prop.stringPropertyNames();
        HashSet<String> requiredProps = new HashSet<String>(Arrays.asList("user", "token", "channel", "timer", "enabled"));
        for (String reqProp : requiredProps) {
            if (propNames.contains(reqProp)) continue;
            logger.info("WARNING: twitchconfig.txt does not contain the property '" + reqProp + "'");
            return false;
        }
        return true;
    }

    public static void createConfig() {
        FileHandle exampleConfig = Gdx.files.internal("twitchconfig.txt.example");
        if (exampleConfig.exists()) {
            FileHandle configTarget = Gdx.files.local(configFilename);
            if (!configTarget.exists()) {
                logger.info("The 'twitchconfig.txt' does not exist, creating it from this example : " + configTarget.path());
                exampleConfig.copyTo(configTarget);
            } else if (configTarget.length() == 0L) {
                logger.info("The 'twitchconfig.txt' is empty, creating it from this example : " + configTarget.path());
                exampleConfig.copyTo(configTarget);
            } else {
                logger.info("The 'twitchconfig.txt' already exists, not overwriting it.");
            }
        } else {
            logger.warn("The '" + exampleConfig.name() + "' does not appear to exist: " + exampleConfig.path());
        }
    }

    public static boolean configFileExists() {
        return Gdx.files.local(configFilename).exists();
    }

    String getUsername() {
        return this.username;
    }

    String getToken() {
        return this.token;
    }

    String getChannel() {
        return this.channel;
    }

    public float getTimer() {
        return this.timer;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}

