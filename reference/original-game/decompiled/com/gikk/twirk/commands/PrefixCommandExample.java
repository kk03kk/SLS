/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.commands;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.commands.CommandExampleBase;
import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import java.util.Calendar;
import java.util.Date;

public class PrefixCommandExample
extends CommandExampleBase {
    private static final String patternA = "!timezone";
    private static final String patternB = "!time";
    private final Twirk twirk;

    public PrefixCommandExample(Twirk twirk) {
        super(CommandExampleBase.CommandType.PREFIX_COMMAND);
        this.twirk = twirk;
    }

    @Override
    protected String getCommandWords() {
        return "!timezone|!time";
    }

    @Override
    protected USER_TYPE getMinUserPrevilidge() {
        return USER_TYPE.DEFAULT;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        if (command.equals(patternA)) {
            this.twirk.channelMessage(sender.getDisplayName() + ": Local time zone is " + Calendar.getInstance().getTimeZone().getDisplayName());
        } else if (command.equals(patternB)) {
            this.twirk.channelMessage(sender.getDisplayName() + ": Local time is " + new Date().toString());
        }
    }
}

