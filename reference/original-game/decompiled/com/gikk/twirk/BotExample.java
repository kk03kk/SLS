/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.commands.PatternCommandExample;
import com.gikk.twirk.commands.PrefixCommandExample;
import com.gikk.twirk.events.TwirkListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class BotExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        String line;
        System.out.println("Welcome to this Bot example. In this example you will be able \nto send and receive messages from a Twitch chat channel. You will \nmake all input directly here in the command prompt. \n\nEnter channel to join (leave out the #):");
        Scanner scanner = new Scanner(new InputStreamReader(System.in, "UTF-8"));
        String channel = "#" + scanner.nextLine();
        Twirk twirk = new TwirkBuilder(channel, "USER_NAME", "oauth:XXXXXXX").setVerboseMode(true).build();
        twirk.addIrcListener(BotExample.getOnDisconnectListener(twirk));
        twirk.addIrcListener(new PatternCommandExample(twirk));
        twirk.addIrcListener(new PrefixCommandExample(twirk));
        twirk.connect();
        while (!(line = scanner.nextLine()).matches(".quit")) {
            twirk.channelMessage(line);
        }
        scanner.close();
        twirk.close();
    }

    private static TwirkListener getOnDisconnectListener(final Twirk twirk) {
        return new TwirkListener(){

            @Override
            public void onDisconnect() {
                try {
                    if (!twirk.connect()) {
                        twirk.close();
                    }
                }
                catch (IOException e) {
                    twirk.close();
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
            }
        };
    }
}

