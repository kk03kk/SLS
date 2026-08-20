/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.commands;

import com.gikk.twirk.enums.USER_TYPE;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public abstract class CommandExampleBase
implements TwirkListener {
    private Set<String> commandPattern;
    private CommandType type;
    private USER_TYPE minPrivilidge;

    protected CommandExampleBase(CommandType type) {
        this.type = type;
        this.commandPattern = this.compile();
        this.minPrivilidge = this.getMinUserPrevilidge();
    }

    @Override
    public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
        block4: {
            String content = message.getContent().toLowerCase(Locale.ENGLISH).trim();
            String[] split = content.split("\\s", 2);
            String command = split[0];
            if (sender.getUserType().value < this.minPrivilidge.value) break block4;
            if (this.type == CommandType.PREFIX_COMMAND) {
                for (String pattern : this.commandPattern) {
                    if (!command.startsWith(pattern)) continue;
                    this.performCommand(pattern, sender, message);
                    break;
                }
            } else {
                for (String pattern : this.commandPattern) {
                    if (!content.contains(pattern)) continue;
                    this.performCommand(pattern, sender, message);
                    break;
                }
            }
        }
    }

    protected abstract String getCommandWords();

    protected abstract USER_TYPE getMinUserPrevilidge();

    protected abstract void performCommand(String var1, TwitchUser var2, TwitchMessage var3);

    private Set<String> compile() {
        String[] patterns = this.getCommandWords().toLowerCase(Locale.ENGLISH).split("\\|");
        HashSet<String> out = new HashSet<String>();
        for (String pattern : patterns) {
            out.add(pattern);
        }
        return out;
    }

    public static enum CommandType {
        PREFIX_COMMAND,
        CONTENT_COMMAND;

    }
}

