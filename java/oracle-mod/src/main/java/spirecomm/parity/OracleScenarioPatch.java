package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.powers.BufferPower;
import com.megacrit.cardcrawl.powers.EquilibriumPower;
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.powers.watcher.EstablishmentPower;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Whitelist-only setup boundaries for original-game differential tests. */
public final class OracleScenarioPatch {
    public static final String COMMAND = "parity_scenario";
    public static final String CARD_PROBE_COMMAND = "parity_card";
    private static final Set<String> SCENARIOS = new HashSet<String>(Arrays.asList(
        "damage_buffer_intangible",
        "duration_weak",
        "retain_ethereal"
    ));
    private static final Map<String, String> CARD_ALLOWLIST = loadCardAllowlist();
    public static Map<String, String> activeScenario = null;

    private OracleScenarioPatch() {}

    private static Map<String, String> loadCardAllowlist() {
        Map<String, String> result = new LinkedHashMap<String, String>();
        try {
            InputStream stream = OracleScenarioPatch.class.getResourceAsStream(
                "/spirecomm/parity/scenario-card-allowlist.tsv"
            );
            if (stream == null) throw new IllegalStateException("missing scenario card allowlist");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\t", 2);
                if (parts.length == 2) result.put(parts[0], parts[1]);
            }
            reader.close();
        } catch (Exception error) {
            throw new ExceptionInInitializerError(error);
        }
        return Collections.unmodifiableMap(result);
    }

    private static String setupDigest(AbstractPlayer player) {
        StringBuilder value = new StringBuilder();
        value.append("hp=").append(player.currentHealth).append('/').append(player.maxHealth)
            .append(";energy=").append(player.energy.energy)
            .append(";block=").append(player.currentBlock);
        for (AbstractCard card : player.hand.group) value.append(";hand=").append(card.cardID).append('+').append(card.timesUpgraded);
        for (AbstractCard card : player.drawPile.group) value.append(";draw=").append(card.cardID).append('+').append(card.timesUpgraded);
        for (AbstractCard card : player.discardPile.group) value.append(";discard=").append(card.cardID).append('+').append(card.timesUpgraded);
        for (AbstractCard card : player.exhaustPile.group) value.append(";exhaust=").append(card.cardID).append('+').append(card.timesUpgraded);
        for (com.megacrit.cardcrawl.powers.AbstractPower power : player.powers) {
            value.append(";power=").append(power.ID).append(':').append(power.amount);
        }
        for (com.megacrit.cardcrawl.relics.AbstractRelic relic : player.relics) {
            value.append(";relic=").append(relic.relicId).append(':').append(relic.counter);
        }
        for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
            value.append(";monster=").append(monster.id)
                .append(':').append(monster.currentHealth).append('/').append(monster.maxHealth)
                .append(':').append(monster.currentBlock).append(':').append(monster.intent);
            for (com.megacrit.cardcrawl.powers.AbstractPower power : monster.powers) {
                value.append(":power=").append(power.ID).append(':').append(power.amount);
            }
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                value.toString().getBytes(StandardCharsets.UTF_8)
            );
            StringBuilder hex = new StringBuilder();
            for (byte item : digest) hex.append(String.format("%02x", item & 0xff));
            return hex.toString();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static void activate(String id, String source, AbstractPlayer player) {
        Map<String, String> evidence = new LinkedHashMap<String, String>();
        evidence.put("scenario_id", id);
        evidence.put("source", source);
        evidence.put("setup_digest", setupDigest(player));
        activeScenario = evidence;
    }

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

    private static void installProbeRelics(AbstractPlayer player, CardType type) {
        player.relics.clear();
        player.relics.add(RelicLibrary.getRelic("Burning Blood").makeCopy());
        if (type == CardType.STATUS) {
            player.relics.add(RelicLibrary.getRelic("Medical Kit").makeCopy());
        } else if (type == CardType.CURSE) {
            player.relics.add(RelicLibrary.getRelic("Blue Candle").makeCopy());
        }
    }

    private static void normalizeProbeTarget() {
        if (AbstractDungeon.getMonsters() == null ||
                AbstractDungeon.getMonsters().monsters.size() != 1) {
            throw new IllegalStateException("parity_card requires a one-monster combat");
        }
        AbstractMonster monster = AbstractDungeon.getMonsters().monsters.get(0);
        monster.currentHealth = 999;
        monster.maxHealth = 999;
        monster.currentBlock = 0;
        monster.powers.clear();
        monster.isDying = false;
        monster.isEscaping = false;
        monster.intent = AbstractMonster.Intent.ATTACK;
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
        activate(id, "RULE_TEST", player);
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /**
     * Stable, deliberately narrow boundary for single-card traces in the
     * packaged Ironclad A0 reachable-content closure.  It accepts a registry
     * id only, never a class name or a reflection path, and only exercises
     * base/+1 variants.
     */
    private static void applyCardProbe(String cardId, int upgrades) {
        String gameId = CARD_ALLOWLIST.get(cardId.toUpperCase(Locale.ROOT));
        if (gameId == null) {
            throw new IllegalArgumentException("parity_card is not in the packaged Ironclad allowlist");
        }
        AbstractCard prototype = CardLibrary.getCard(gameId);
        if (prototype == null) {
            throw new IllegalArgumentException("parity_card requires a packaged card id");
        }
        // Never upgrade CardLibrary's shared prototype: doing so contaminates
        // every later reward/deck copy in the same original-game process.
        AbstractCard probe = prototype.makeCopy();
        if (upgrades < 0 || upgrades > 1) {
            throw new IllegalArgumentException("parity_card upgrades must be 0 or 1");
        }
        if (upgrades == 1) {
            probe.upgrade();
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        installProbeRelics(player, probe.type);
        player.currentHealth = 80;
        player.maxHealth = 80;
        normalizeProbeTarget();
        // Four energy covers Blood for Blood while retaining a deterministic
        // baseline for X-cost cards.  The support cards make hand/discard/
        // exhaust selection effects observable without changing card identity.
        player.energy.energy = 4;
        player.hand.addToBottom(probe);
        player.hand.addToBottom(card("Strike_R"));
        player.drawPile.addToBottom(card("Defend_R"));
        player.discardPile.addToBottom(card("Defend_R"));
        player.exhaustPile.addToBottom(card("Defend_R"));
        activate("card_probe:" + cardId.toUpperCase(Locale.ROOT) + ":" + upgrades,
            "RULE_TEST:IRONCLAD_CARD_ALLOWLIST", player);
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
