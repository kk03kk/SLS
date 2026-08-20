/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.integrations.DistributorFactoryException;
import com.megacrit.cardcrawl.integrations.PublisherIntegration;
import com.megacrit.cardcrawl.integrations.discord.DiscordIntegration;
import com.megacrit.cardcrawl.integrations.ea.EaIntegration;
import com.megacrit.cardcrawl.integrations.gog.GogIntegration;
import com.megacrit.cardcrawl.integrations.microsoft.MicrosoftIntegration;
import com.megacrit.cardcrawl.integrations.steam.SteamIntegration;
import com.megacrit.cardcrawl.integrations.wegame.WeGameIntegration;

public class DistributorFactory {
    public static PublisherIntegration getEnabledDistributor(String distributor) throws DistributorFactoryException {
        switch (distributor) {
            case "steam": {
                return new SteamIntegration();
            }
            case "discord": {
                return new DiscordIntegration();
            }
            case "wegame": {
                return new WeGameIntegration();
            }
            case "gog": {
                return new GogIntegration();
            }
            case "ea": {
                return new EaIntegration();
            }
            case "microsoft": {
                return new MicrosoftIntegration();
            }
        }
        throw new DistributorFactoryException("Unrecognized distributor=" + distributor);
    }

    public static boolean isLeaderboardEnabled() {
        return CardCrawlGame.publisherIntegration.getType() == Distributor.STEAM;
    }

    public static enum Distributor {
        STEAM,
        DISCORD,
        WEGAME,
        GOG,
        EA,
        MICROSOFT;

    }
}

