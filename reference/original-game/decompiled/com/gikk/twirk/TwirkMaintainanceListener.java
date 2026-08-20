/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.mode.Mode;
import com.gikk.twirk.types.users.Userstate;

class TwirkMaintainanceListener
implements TwirkListener {
    private final Twirk instance;

    TwirkMaintainanceListener(Twirk twirk) {
        this.instance = twirk;
    }

    @Override
    public void onAnything(String line) {
        if (this.instance.verboseMode) {
            System.out.println("IN  " + line);
        }
    }

    @Override
    public void onJoin(String joinedNick) {
        if (!this.instance.online.add(joinedNick)) {
            System.out.println(" was already listed as online....\tUser " + joinedNick);
        }
    }

    @Override
    public void onPart(String partedNick) {
        if (!this.instance.online.remove(partedNick)) {
            System.out.println("\tUser " + partedNick + " was not listed as online....");
        }
    }

    @Override
    public void onMode(Mode mode) {
        if (mode.getEvent() == Mode.MODE_EVENT.GAINED_MOD) {
            this.instance.moderators.add(mode.getUser());
        } else {
            this.instance.moderators.remove(mode.getUser());
        }
    }

    @Override
    public void onUserstate(Userstate userstate) {
        if (userstate.isMod()) {
            this.instance.setOutputMessageDelay(300);
        } else {
            this.instance.setOutputMessageDelay(1500);
        }
    }

    @Override
    public void onDisconnect() {
        this.instance.online.clear();
        this.instance.moderators.clear();
    }
}

