/*
 * Decompiled with CFR 0.152.
 */
package de.robojumper.ststwitch;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.random.Random;
import de.robojumper.ststwitch.TwitchConfig;
import de.robojumper.ststwitch.TwitchConnection;
import de.robojumper.ststwitch.TwitchMessageListener;
import de.robojumper.ststwitch.TwitchPanel;
import de.robojumper.ststwitch.TwitchVoteListener;
import de.robojumper.ststwitch.TwitchVoteOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class TwitchVoter
implements TwitchMessageListener {
    private static List<TwitchVoteListener> listeners = new LinkedList<TwitchVoteListener>();
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TwitchVoter");
    public static final String[] TEXT = TwitchVoter.uiStrings.TEXT;
    private static boolean wasConnected = false;
    private boolean isVoting;
    private Consumer<Integer> voteCb;
    private float timer;
    private boolean triggered = false;
    private TwitchVoteOption[] options;
    private Set<String> votedUsernames = new HashSet<String>();
    private TwitchConfig twitchConfig;

    TwitchVoter(TwitchConfig twitchConfig) {
        this.twitchConfig = twitchConfig;
        this.isVoting = false;
        TwitchPanel.getTwitch().registerMessageListener(this);
    }

    public void update() {
        boolean newStatus;
        if (this.isCurrentlyVoting()) {
            this.timer -= Gdx.graphics.getDeltaTime();
            if (this.timer <= 0.0f && !this.triggered) {
                this.completeVoting();
            }
        }
        boolean bl = newStatus = TwitchPanel.getTwitch().getConnectionStatus() == TwitchConnection.ConnectionStatus.CONNECTED;
        if (wasConnected != newStatus) {
            if (newStatus) {
                this.notifyListenersConnected();
            } else {
                this.notifyListenersDisconnected();
            }
        }
        wasConnected = newStatus;
    }

    private void notifyComplete() {
        int maxIdx = 0;
        for (int i = 1; i < this.options.length; ++i) {
            if (this.options[i].voteCount != 0 && this.options[i].voteCount == this.options[maxIdx].voteCount) {
                if (!new Random().randomBoolean()) continue;
                maxIdx = i;
                continue;
            }
            if (this.options[i].voteCount <= this.options[maxIdx].voteCount) continue;
            maxIdx = i;
        }
        this.voteCb.accept(maxIdx);
    }

    private void notifyListenersConnected() {
        for (TwitchVoteListener l : listeners) {
            l.onTwitchAvailable();
        }
    }

    private void notifyListenersDisconnected() {
        for (TwitchVoteListener l : listeners) {
            l.onTwitchUnavailable();
        }
    }

    public static void registerListener(TwitchVoteListener L) {
        listeners.add(L);
    }

    private boolean isCurrentlyVoting() {
        return this.isVoting;
    }

    private boolean isVotingAvailable() {
        return !this.isVoting && TwitchPanel.getTwitch().getConnectionStatus() == TwitchConnection.ConnectionStatus.CONNECTED;
    }

    public boolean isVotingConnected() {
        return TwitchPanel.getTwitch().getConnectionStatus() == TwitchConnection.ConnectionStatus.CONNECTED;
    }

    public TwitchVoteOption[] getOptions() {
        return this.options;
    }

    public boolean initiateSimpleNumberVote(String[] strOptions, Consumer<Integer> voteCb) {
        TwitchVoteOption[] options = new TwitchVoteOption[strOptions.length];
        for (int i = 0; i < strOptions.length; ++i) {
            options[i] = new TwitchVoteOption(strOptions[i], "#" + i, "^#" + i + ".*$");
        }
        return this.initiateVote(options, voteCb);
    }

    private boolean initiateVote(TwitchVoteOption[] options, Consumer<Integer> voteCb) {
        if (this.isVotingAvailable()) {
            this.timer = this.twitchConfig.getTimer();
            this.triggered = false;
            this.isVoting = true;
            this.voteCb = voteCb;
            this.options = TwitchVoteOption.cloneArrayOfOptions(options);
            this.votedUsernames.clear();
            this.publishOptions();
            return true;
        }
        return false;
    }

    private void publishOptions() {
        StringBuilder sb = new StringBuilder();
        sb.append(TEXT[0]);
        for (int i = 0; i < this.options.length; ++i) {
            sb.append(this.options[i].commandHint);
            sb.append(": ");
            sb.append(this.options[i].displayName);
            if (i == this.options.length - 1) continue;
            sb.append("; ");
        }
        TwitchPanel.getTwitch().sendMessage(sb.toString());
    }

    @Override
    public void onMessage(String msg, String user) {
        for (TwitchVoteOption option : this.options) {
            if (!option.matchPattern.matcher(msg).matches() || this.votedUsernames.contains(user)) continue;
            ++option.voteCount;
            this.votedUsernames.add(user);
        }
    }

    public int getSecondsRemaining() {
        return (int)this.timer;
    }

    public void endVoting(boolean canceled) {
        if (this.isCurrentlyVoting()) {
            this.isVoting = false;
        }
    }

    private void completeVoting() {
        this.notifyComplete();
        this.endVoting(false);
        this.triggered = true;
    }
}

