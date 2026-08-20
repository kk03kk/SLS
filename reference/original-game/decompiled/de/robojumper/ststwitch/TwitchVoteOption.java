/*
 * Decompiled with CFR 0.152.
 */
package de.robojumper.ststwitch;

import java.util.regex.Pattern;

public class TwitchVoteOption {
    public final String displayName;
    final String commandHint;
    final Pattern matchPattern;
    public int voteCount;

    TwitchVoteOption(String name, String hint, String pattern) {
        this.displayName = name;
        this.commandHint = hint;
        this.matchPattern = Pattern.compile(pattern);
    }

    private TwitchVoteOption(TwitchVoteOption other) {
        this.displayName = other.displayName;
        this.commandHint = other.commandHint;
        this.matchPattern = other.matchPattern;
        this.voteCount = other.voteCount;
    }

    static TwitchVoteOption[] cloneArrayOfOptions(TwitchVoteOption[] options) {
        TwitchVoteOption[] newArr = new TwitchVoteOption[options.length];
        for (int i = 0; i < newArr.length; ++i) {
            newArr[i] = new TwitchVoteOption(options[i]);
        }
        return newArr;
    }
}

