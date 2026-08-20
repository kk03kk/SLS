/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.events;

import com.gikk.twirk.types.clearChat.ClearChat;
import com.gikk.twirk.types.hostTarget.HostTarget;
import com.gikk.twirk.types.mode.Mode;
import com.gikk.twirk.types.notice.Notice;
import com.gikk.twirk.types.roomstate.Roomstate;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.usernotice.Usernotice;
import com.gikk.twirk.types.users.TwitchUser;
import com.gikk.twirk.types.users.Userstate;
import java.util.Collection;

public interface TwirkListener {
    default public void onAnything(String unformatedMessage) {
    }

    default public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
    }

    default public void onWhisper(TwitchUser sender, TwitchMessage message) {
    }

    default public void onJoin(String joinedNick) {
    }

    default public void onPart(String partedNick) {
    }

    default public void onConnect() {
    }

    default public void onReconnect() {
    }

    default public void onDisconnect() {
    }

    default public void onNotice(Notice notice) {
    }

    default public void onHost(HostTarget hostNotice) {
    }

    default public void onMode(Mode mode) {
    }

    default public void onUserstate(Userstate userstate) {
    }

    default public void onRoomstate(Roomstate roomstate) {
    }

    default public void onClearChat(ClearChat clearChat) {
    }

    default public void onNamesList(Collection<String> namesList) {
    }

    default public void onUsernotice(TwitchUser user, Usernotice usernotice) {
    }

    default public void onUnknown(String unformatedMessage) {
    }
}

