package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardColor;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.powers.BufferPower;
import com.megacrit.cardcrawl.powers.EquilibriumPower;
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.powers.watcher.EstablishmentPower;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Whitelist-only setup boundaries for original-game differential tests. */
public final class OracleScenarioPatch {
    public static final String COMMAND = "parity_scenario";
    public static final String CARD_PROBE_COMMAND = "parity_card";
    private static final Set<String> SCENARIOS = new HashSet<String>(Arrays.asList(
        "damage_buffer_intangible",
        "duration_weak",
        "retain_ethereal"
    ));
    public static String activeScenario = null;

    private OracleScenarioPatch() {}

    private static AbstractCard card(String id) {
        AbstractCard prototype = CardLibrary.getCard(id);
        if (prototype == null) {
            throw new IllegalStateException("Unknown oracle scenario card: " + id);
        }
        return prototype.makeCopy();
    }

    private static void clearCombatState(AbstractPlayer player) {
        player.hand.clear();
        player.drawPile.clear();
        player.discardPile.clear();
        player.exhaustPile.clear();
        player.limbo.clear();
        player.powers.clear();
        player.currentBlock = 0;
        player.energy.energy = 3;
        AbstractDungeon.actionManager.actions.clear();
        AbstractDungeon.actionManager.cardQueue.clear();
    }

    private static void apply(String id) {
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);

        if ("retain_ethereal".equals(id)) {
            AbstractCard retainedStrike = card("Strike_R");
            retainedStrike.retain = true;
            player.hand.addToTop(retainedStrike);
            player.hand.addToTop(card("Ghostly Armor"));
            player.hand.addToTop(card("Dazed"));
            player.hand.addToTop(card("Defend_R"));
            player.powers.add(new EstablishmentPower(player, 1));
            player.powers.add(new EquilibriumPower(player, 1));
        } else if ("duration_weak".equals(id)) {
            player.hand.addToTop(card("Defend_R"));
            player.drawPile.addToTop(card("Strike_R"));
            player.powers.add(new WeakPower(player, 2, true));
        } else if ("damage_buffer_intangible".equals(id)) {
            player.currentBlock = 3;
            player.hand.addToTop(card("Defend_R"));
            player.powers.add(new IntangiblePlayerPower(player, 1));
            player.powers.add(new BufferPower(player, 1));
            player.relics.add(RelicLibrary.getRelic("Torii").makeCopy());
            player.relics.add(RelicLibrary.getRelic("TungstenRod").makeCopy());
        } else {
            throw new IllegalArgumentException("Unknown oracle scenario: " + id);
        }
        activeScenario = id;
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /**
     * Stable, deliberately narrow boundary for single-Ironclad-card traces.
     * It accepts an actual CardLibrary red card only, never a class name or a
     * reflection path, and only exercises base/+1 variants.
     */
    private static void applyCardProbe(String cardId, int upgrades) {
        AbstractCard probe = CardLibrary.getCard(cardId);
        if (probe == null || probe.color != CardColor.RED) {
            throw new IllegalArgumentException("parity_card requires an Ironclad card id");
        }
        if (upgrades < 0 || upgrades > 1) {
            throw new IllegalArgumentException("parity_card upgrades must be 0 or 1");
        }
        if (upgrades == 1) {
            probe.upgrade();
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        // Four energy covers Blood for Blood while retaining a deterministic
        // baseline for X-cost cards.  The support cards make hand/discard/
        // exhaust selection effects observable without changing card identity.
        player.energy.energy = 4;
        player.hand.addToBottom(probe);
        player.hand.addToBottom(card("Strike_R"));
        player.drawPile.addToBottom(card("Defend_R"));
        player.discardPile.addToBottom(card("Defend_R"));
        player.exhaustPile.addToBottom(card("Defend_R"));
        activeScenario = "card_probe:" + probe.cardID + ":" + upgrades;
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    @SpirePatch(clz = CommandExecutor.class, method = "getAvailableCommands")
    public static class Advertise {
        @SpirePostfixPatch
        public static ArrayList<String> Postfix(ArrayList<String> commands) {
            if (CommandExecutor.isEndCommandAvailable()) {
                if (!commands.contains(COMMAND)) commands.add(COMMAND);
                if (!commands.contains(CARD_PROBE_COMMAND)) commands.add(CARD_PROBE_COMMAND);
            }
            return commands;
        }
    }

    @SpirePatch(clz = CommandExecutor.class, method = "executeCommand")
    public static class Execute {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> Prefix(String command) {
            String normalized = command.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("start ")) {
                activeScenario = null;
                return SpireReturn.Continue();
            }
            if (!normalized.startsWith(COMMAND + " ")) {
                if (!normalized.startsWith(CARD_PROBE_COMMAND + " ")) {
                    return SpireReturn.Continue();
                }
                String[] cardParts = command.trim().split("\\s+");
                if (cardParts.length != 3) {
                    throw new IllegalArgumentException("parity_card requires CARD_ID and UPGRADES");
                }
                int upgrades;
                try {
                    upgrades = Integer.parseInt(cardParts[2]);
                } catch (NumberFormatException error) {
                    throw new IllegalArgumentException("parity_card upgrades must be an integer", error);
                }
                applyCardProbe(cardParts[1], upgrades);
                return SpireReturn.Return(Boolean.TRUE);
            }
            String id = normalized.substring(COMMAND.length()).trim();
            if (!SCENARIOS.contains(id)) {
                throw new IllegalArgumentException("Unknown oracle scenario: " + id);
            }
            apply(id);
            return SpireReturn.Return(Boolean.TRUE);
        }
    }
}
