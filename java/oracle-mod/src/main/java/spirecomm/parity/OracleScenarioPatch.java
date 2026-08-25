package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.powers.BufferPower;
import com.megacrit.cardcrawl.powers.EquilibriumPower;
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.powers.FocusPower;
import com.megacrit.cardcrawl.powers.watcher.EstablishmentPower;
import com.megacrit.cardcrawl.stances.CalmStance;
import com.megacrit.cardcrawl.stances.NeutralStance;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.orbs.Frost;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.ui.campfire.RestOption;
import com.megacrit.cardcrawl.ui.campfire.SmithOption;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.RedCirclet;
import com.megacrit.cardcrawl.relics.SlaversCollar;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.helpers.EventHelper;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.potions.SmokeBomb;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.core.AbstractCreature;
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
    public static final String POTION_PROBE_COMMAND = "parity_potion";
    public static final String RELIC_PROBE_COMMAND = "parity_relic";
    public static final String ENCOUNTER_PROBE_COMMAND = "parity_encounter";
    public static final String ENGINE_PROBE_COMMAND = "parity_engine";
    public static final String EVENT_PROBE_COMMAND = "parity_event";
    public static final String RELIC_SPAWN_PROBE_COMMAND = "parity_relic_spawn";
    public static final String RELIC_UNEQUIP_PROBE_COMMAND = "parity_relic_unequip";
    public static final String RELIC_COUNTER_PROBE_COMMAND = "parity_relic_counter";
    public static final String RELIC_REWARD_PROBE_COMMAND = "parity_relic_reward";
    public static final String RELIC_HEAL_PROBE_COMMAND = "parity_relic_heal";
    public static final String RELIC_NEUTRAL_PROBE_COMMAND = "parity_relic_neutral";
    public static final String RELIC_VICTORY_PROBE_COMMAND = "parity_relic_victory";
    public static final String RELIC_CAMPFIRE_PROBE_COMMAND = "parity_relic_campfire";
    public static final String RELIC_RESOURCE_PROBE_COMMAND = "parity_relic_resource";
    public static final String RELIC_CARD_USE_PROBE_COMMAND = "parity_relic_card_use";
    public static final String RELIC_OBTAIN_CARD_PROBE_COMMAND = "parity_relic_obtain_card";
    public static final String RELIC_HP_LOSS_PROBE_COMMAND = "parity_relic_hp_loss";
    public static final String RELIC_VICTORY_RESOURCE_PROBE_COMMAND = "parity_relic_victory_resource";
    public static final String RELIC_DAMAGE_PROBE_COMMAND = "parity_relic_damage";
    public static final String RELIC_SHUFFLE_PROBE_COMMAND = "parity_relic_shuffle";
    public static final String RELIC_SPECIAL_RESOURCE_PROBE_COMMAND = "parity_relic_special_resource";
    public static final String RELIC_TURN_STATE_PROBE_COMMAND = "parity_relic_turn_state";
    public static final String RELIC_END_TURN_PROBE_COMMAND = "parity_relic_end_turn";
    public static final String RELIC_TRIGGER_PROBE_COMMAND = "parity_relic_trigger";
    public static final String RELIC_WORLD_PROBE_COMMAND = "parity_relic_world";
    public static final String RELIC_EQUIP_PROBE_COMMAND = "parity_relic_equip";
    private static final Set<String> SCENARIOS = new HashSet<String>(Arrays.asList(
        "damage_buffer_intangible",
        "duration_weak",
        "retain_ethereal"
    ));
    private static final Set<String> INTERACTIVE_EQUIP_RELICS = new HashSet<String>(Arrays.asList(
        "ASTROLABE", "BOTTLED_FLAME", "BOTTLED_LIGHTNING", "BOTTLED_TORNADO",
        "CALLING_BELL", "CAULDRON", "DOLLYS_MIRROR", "EMPTY_CAGE",
        "ORRERY", "PANDORAS_BOX", "TINY_HOUSE"
    ));
    private static final Set<String> POLICY_NEUTRAL_RELIC_CALLBACKS =
        new HashSet<String>(Arrays.asList(
            "ANCHOR:JUSTENTEREDROOM",
            "FOSSILIZED_HELIX:JUSTENTEREDROOM",
            "MEMBERSHIP_CARD:ONENTERROOM",
            "SMILING_MASK:ONENTERROOM",
            "THE_COURIER:ONENTERROOM",
            "BLACK_STAR:ONENTERROOM",
            "BLACK_STAR:ONVICTORY",
            "CENTENNIAL_PUZZLE:JUSTENTEREDROOM",
            "CENTENNIAL_PUZZLE:ONVICTORY",
            "CURSED_KEY:JUSTENTEREDROOM",
            "STONE_CALENDAR:JUSTENTEREDROOM"
        ));
    private static final Set<String> VICTORY_COUNTER_RELICS =
        new HashSet<String>(Arrays.asList(
            "CAPTAINS_WHEEL", "HORN_CLEAT", "KUNAI", "LETTER_OPENER",
            "ORNAMENTAL_FAN", "POCKETWATCH", "SHURIKEN", "STONE_CALENDAR",
            "VELVET_CHOKER"
        ));
    private static final Set<String> CAMPFIRE_RELICS =
        new HashSet<String>(Arrays.asList(
            "COFFEE_DRIPPER", "FUSION_HAMMER", "GIRYA", "PEACE_PIPE", "SHOVEL"
        ));
    private static final Set<String> RESOURCE_RELICS =
        new HashSet<String>(Arrays.asList(
            "BLOODY_IDOL", "ETERNAL_FEATHER", "SSSERPENT_HEAD"
        ));
    private static final Set<String> CARD_USE_RELICS =
        new HashSet<String>(Arrays.asList(
            "BIRD_FACED_URN", "BLUE_CANDLE", "INK_BOTTLE", "KUNAI",
            "LETTER_OPENER", "MEDICAL_KIT", "NUNCHAKU", "ORNAMENTAL_FAN",
            "PEN_NIB", "SHURIKEN", "MUMMIFIED_HAND", "ORANGE_PELLETS",
            "NECRONOMICON"
        ));
    private static final Set<String> OBTAIN_CARD_RELICS =
        new HashSet<String>(Arrays.asList(
            "CERAMIC_FISH", "DARKSTONE_PERIAPT", "FROZEN_EGG",
            "MOLTEN_EGG", "TOXIC_EGG"
        ));
    private static final Set<String> HP_LOSS_RELICS =
        new HashSet<String>(Arrays.asList(
            "CENTENNIAL_PUZZLE", "RUNIC_CUBE", "SELF_FORMING_CLAY"
        ));
    private static final Set<String> VICTORY_RESOURCE_RELICS =
        new HashSet<String>(Arrays.asList(
            "BLACK_BLOOD", "BURNING_BLOOD", "FACE_OF_CLERIC", "MEAT_ON_THE_BONE"
        ));
    private static final Map<String, String> CARD_ALLOWLIST = loadCardAllowlist();
    private static final Map<String, String> POTION_ALLOWLIST = loadAllowlist(
        "/spirecomm/parity/scenario-potion-allowlist.tsv"
    );
    private static final Map<String, String> RELIC_ALLOWLIST = loadAllowlist(
        "/spirecomm/parity/scenario-relic-allowlist.tsv"
    );
    private static final Map<String, String> ENCOUNTER_ALLOWLIST = loadAllowlist(
        "/spirecomm/parity/scenario-encounter-allowlist.tsv"
    );
    private static final Map<String, String> EVENT_ALLOWLIST = loadAllowlist(
        "/spirecomm/parity/scenario-event-allowlist.tsv"
    );
    public static Map<String, String> activeScenario = null;

    private OracleScenarioPatch() {}

    private static Map<String, String> loadCardAllowlist() {
        return loadAllowlist("/spirecomm/parity/scenario-card-allowlist.tsv");
    }

    private static Map<String, String> loadAllowlist(String resource) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        try {
            InputStream stream = OracleScenarioPatch.class.getResourceAsStream(resource);
            if (stream == null) throw new IllegalStateException("missing scenario allowlist: " + resource);
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
        if (AbstractDungeon.getMonsters() != null) {
            for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
                value.append(";monster=").append(monster.id)
                    .append(':').append(monster.currentHealth).append('/').append(monster.maxHealth)
                    .append(':').append(monster.currentBlock).append(':').append(monster.intent);
                for (com.megacrit.cardcrawl.powers.AbstractPower power : monster.powers) {
                    value.append(":power=").append(power.ID).append(':').append(power.amount);
                }
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
        player.damagedThisCombat = 0;
        player.cardsPlayedThisTurn = 0;
        player.currentBlock = 0;
        player.energy.energy = 3;
        AbstractDungeon.actionManager.actions.clear();
        AbstractDungeon.actionManager.currentAction = null;
        AbstractDungeon.actionManager.cardQueue.clear();
        AbstractDungeon.actionManager.cardsPlayedThisTurn.clear();
        AbstractDungeon.actionManager.cardsPlayedThisCombat.clear();
        AbstractDungeon.effectList.clear();
        AbstractDungeon.effectsQueue.clear();
        AbstractDungeon.topLevelEffects.clear();
        AbstractDungeon.topLevelEffectsQueue.clear();
        GameActionManager.totalDiscardedThisTurn = 0;
        GameActionManager.damageReceivedThisTurn = 0;
        GameActionManager.hpLossThisCombat = 0;
        GameActionManager.turn = 1;
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
        monster.setMove((byte)1, AbstractMonster.Intent.ATTACK, 6);
        monster.createIntent();
    }

    private static void apply(String id) {
        AbstractPlayer player = AbstractDungeon.player;
        if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
        clearCombatState(player);
        // Each scenario is an independent experiment.  A preceding rule probe
        // may have installed relics or advanced the dummy monster's move; do
        // not let that state contaminate the next setup in the same JVM.
        installProbeRelics(player, CardType.SKILL);
        normalizeProbeTarget();
        player.currentHealth = 80;
        player.maxHealth = 80;

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
        activate(id, "RULE_TEST:ISOLATED_V2", player);
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
        if (upgrades < 0 || upgrades > 1) {
            throw new IllegalArgumentException("parity_card upgrades must be 0 or 1");
        }
        AbstractPlayer player = AbstractDungeon.player;
        // Reset counters before makeCopy(): Blood for Blood derives its
        // initial cost from the player's damagedThisCombat value.
        clearCombatState(player);
        // Never upgrade CardLibrary's shared prototype: doing so contaminates
        // every later reward/deck copy in the same original-game process.
        AbstractCard probe = prototype.makeCopy();
        if (upgrades == 1) {
            probe.upgrade();
        }
        installProbeRelics(player, probe.type);
        player.currentHealth = 80;
        player.maxHealth = 80;
        normalizeProbeTarget();
        // Four energy covers Blood for Blood while retaining a deterministic
        // baseline for X-cost cards. The support cards make hand/discard/
        // exhaust selection effects observable without changing card identity.
        player.energy.energy = 4;
        EnergyPanel.setEnergy(4);
        player.hand.addToBottom(card("Strike_R"));
        player.hand.addToBottom(probe);
        player.drawPile.addToBottom(card("Defend_R"));
        player.drawPile.addToTop(card("Strike_R"));
        player.discardPile.addToBottom(card("Defend_R"));
        player.exhaustPile.addToBottom(card("Defend_R"));
        // Stock hand insertions recalculate dynamic card values before the
        // player can act (Mind Blast, Body Slam, Perfected Strike, etc.).
        player.hand.applyPowers();
        activate("card_probe:" + cardId.toUpperCase(Locale.ROOT) + ":" + upgrades,
            "RULE_TEST:IRONCLAD_CARD_ALLOWLIST", player);
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyPotionProbe(String potionId, boolean sacredBark) {
        String gameId = POTION_ALLOWLIST.get(potionId.toUpperCase(Locale.ROOT));
        if (gameId == null) {
            throw new IllegalArgumentException("parity_potion is not in the packaged Ironclad allowlist");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        installProbeRelics(player, CardType.SKILL);
        if (sacredBark) player.relics.add(RelicLibrary.getRelic("SacredBark").makeCopy());
        // Potion constructors snapshot getPotency(), which consults Sacred
        // Bark. Construct only after the probe relic set is final, matching
        // SacredBark.onEquip's initializeData refresh in a stock run.
        AbstractPotion potion = PotionHelper.getPotion(gameId);
        if (potion == null) {
            throw new IllegalArgumentException("parity_potion requires a packaged potion id");
        }
        player.currentHealth = "FAIRY_POTION".equalsIgnoreCase(potionId) ? 1 : 40;
        player.maxHealth = 80;
        normalizeProbeTarget();
        player.energy.energy = 3;
        EnergyPanel.setEnergy(3);
        player.hand.addToTop(card("Strike_R"));
        player.hand.addToTop(card("Defend_R"));
        player.hand.addToTop(card("Dazed"));
        player.drawPile.addToBottom(card("Defend_R"));
        player.drawPile.addToTop(card("Strike_R"));
        player.discardPile.addToBottom(card("Defend_R"));
        player.exhaustPile.addToBottom(card("Defend_R"));
        player.potions.clear();
        for (int slot = 0; slot < player.potionSlots; ++slot) {
            player.potions.add(new PotionSlot(slot));
        }
        player.obtainPotion(0, potion);
        player.hand.applyPowers();
        activate("potion_probe:" + potionId.toUpperCase(Locale.ROOT) + ":" + sacredBark,
            "RULE_TEST:IRONCLAD_POTION_ALLOWLIST", player);
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Controlled first-turn lifecycle probe for a packaged reachable relic. */
    private static void applyRelicProbe(String relicId) {
        ParityRng.resetRelicProbeStreams();
        String gameId = RELIC_ALLOWLIST.get(relicId.toUpperCase(Locale.ROOT));
        if (gameId == null) {
            throw new IllegalArgumentException("parity_relic is not in the packaged Ironclad allowlist");
        }
        AbstractRelic prototype = "RED_CIRCLET".equalsIgnoreCase(relicId)
            ? new RedCirclet() : RelicLibrary.getRelic(gameId);
        if (prototype == null) {
            throw new IllegalArgumentException("parity_relic requires a packaged relic id");
        }
        AbstractPlayer player = AbstractDungeon.player;
        if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
        clearCombatState(player);
        player.energy.energyMaster = 3;
        player.masterHandSize = 5;
        EnergyPanel.setEnergy(3);
        player.currentHealth = 80;
        player.maxHealth = 80;
        normalizeProbeTarget();
        player.relics.clear();
        AbstractRelic relic = prototype.makeCopy();
        player.relics.add(relic);
        // This probe models a relic already present when combat begins. Stock
        // does not re-run acquisition-time onEquip hooks at that boundary;
        // doing so would incorrectly open boss-relic selection screens such
        // as Astrolabe during combat setup.
        if (!INTERACTIVE_EQUIP_RELICS.contains(relicId.toUpperCase(Locale.ROOT))) {
            relic.onEquip();
            player.energy.energy = player.energy.energyMaster;
            EnergyPanel.setEnergy(player.energy.energyMaster);
        } else if ("TINY_HOUSE".equalsIgnoreCase(relicId)) {
            // Preserve Tiny House's immediate max-HP acquisition effect while
            // omitting its reward overlays from this combat-entry boundary.
            player.increaseMaxHp(5, true);
        }
        // Reconstruct the stock pre-draw combat boundary.  Several relics
        // depend on a non-empty draw pile (notably Mark of Pain's random
        // insertion), and pre-draw actions must run before the ordinary five
        // card opening draw and the queued atBattleStart actions.
        for (int index = 0; index < 5; ++index) {
            player.drawPile.addToBottom(card("Strike_R"));
        }
        for (int index = 0; index < 4; ++index) {
            player.drawPile.addToBottom(card("Defend_R"));
        }
        player.drawPile.addToBottom(card("Bash"));
        relic.atPreBattle();
        relic.atBattleStartPreDraw();
        AbstractDungeon.actionManager.addToBottom(
            new DrawCardAction(player, player.masterHandSize)
        );
        relic.atBattleStart();
        relic.atTurnStart();
        relic.atTurnStartPostDraw();
        drainActions();
        // Stabilize the actionable cards after lifecycle hooks. Generated-card
        // and extra-draw relics receive dedicated trigger scenarios later.
        player.hand.clear();
        player.drawPile.clear();
        player.discardPile.clear();
        player.exhaustPile.clear();
        player.hand.addToBottom(card("Inflame"));
        player.hand.addToBottom(card("Defend_R"));
        player.hand.addToBottom(card("Strike_R"));
        player.drawPile.addToBottom(card("Defend_R"));
        player.drawPile.addToTop(card("Strike_R"));
        player.discardPile.addToBottom(card("Defend_R"));
        player.exhaustPile.addToBottom(card("Defend_R"));
        player.hand.applyPowers();
        activate("relic_probe:" + relicId.toUpperCase(Locale.ROOT) + ":FIRST_TURN",
            "RULE_TEST:IRONCLAD_RELIC_ALLOWLIST", player);
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Act-1 canSpawn probe with explicit deck/relic/shop preconditions. */
    private static void applyRelicSpawnProbe(
            String relicId, int floor, boolean shopRoom, String preset) {
        String gameId = RELIC_ALLOWLIST.get(relicId.toUpperCase(Locale.ROOT));
        AbstractRelic prototype = gameId == null ? null : RelicLibrary.getRelic(gameId);
        if (prototype == null || floor < 0 || floor > 17) {
            throw new IllegalArgumentException("invalid Act-1 relic spawn probe");
        }
        AbstractPlayer player = AbstractDungeon.player;
        player.masterDeck.clear();
        player.masterDeck.addToBottom(card("Strike_R"));
        player.masterDeck.addToBottom(card("Defend_R"));
        player.masterDeck.addToBottom(card("Bash"));
        if ("VALID_ATTACK".equalsIgnoreCase(preset)) {
            player.masterDeck.addToBottom(card("Wild Strike"));
        } else if ("VALID_SKILL".equalsIgnoreCase(preset)) {
            player.masterDeck.addToBottom(card("Shrug It Off"));
        } else if ("VALID_POWER".equalsIgnoreCase(preset)) {
            player.masterDeck.addToBottom(card("Inflame"));
        }
        player.relics.clear();
        if ("BURNING_BLOOD".equalsIgnoreCase(preset)) {
            player.relics.add(RelicLibrary.getRelic("Burning Blood").makeCopy());
        } else if ("CAMPFIRE_TWO".equalsIgnoreCase(preset)) {
            player.relics.add(RelicLibrary.getRelic("Peace Pipe").makeCopy());
            player.relics.add(RelicLibrary.getRelic("Shovel").makeCopy());
        }
        int previousFloor = AbstractDungeon.floorNum;
        int previousAct = AbstractDungeon.actNum;
        AbstractRoom previousRoom = AbstractDungeon.getCurrRoom();
        AbstractDungeon.floorNum = floor;
        AbstractDungeon.actNum = 1;
        if (shopRoom) AbstractDungeon.currMapNode.room = new ShopRoom();
        boolean result;
        try {
            result = prototype.canSpawn();
        } finally {
            AbstractDungeon.floorNum = previousFloor;
            AbstractDungeon.actNum = previousAct;
            AbstractDungeon.currMapNode.room = previousRoom;
        }
        activate("relic_spawn_probe:" + relicId.toUpperCase(Locale.ROOT) + ":"
            + floor + ":" + shopRoom + ":" + preset.toUpperCase(Locale.ROOT),
            "STOCK_RELIC_CAN_SPAWN:ACT1", player);
        activeScenario.put("spawn_result", Boolean.toString(result));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Compare onUnequip by its persistent next-combat contract. */
    private static void applyRelicUnequipProbe(String relicId) {
        String gameId = RELIC_ALLOWLIST.get(relicId.toUpperCase(Locale.ROOT));
        AbstractRelic prototype = gameId == null ? null : RelicLibrary.getRelic(gameId);
        if (prototype == null) throw new IllegalArgumentException("invalid relic unequip probe");
        AbstractPlayer player = AbstractDungeon.player;
        player.energy.energyMaster = 4;
        player.masterHandSize = 7;
        player.masterDeck.clear();
        player.masterDeck.addToBottom(card("Strike_R"));
        player.masterDeck.addToBottom(card("Defend_R"));
        player.masterDeck.addToBottom(card("Necronomicurse"));
        player.relics.clear();
        AbstractRelic relic = prototype.makeCopy();
        player.relics.add(relic);
        int energyBefore = player.energy.energyMaster;
        int handBefore = player.masterHandSize;
        relic.onUnequip();
        player.relics.remove(relic);
        int curses = 0;
        for (AbstractCard item : player.masterDeck.group) {
            if ("Necronomicurse".equals(item.cardID)) ++curses;
        }
        activate("relic_unequip_probe:" + relicId.toUpperCase(Locale.ROOT),
            "STOCK_RELIC_ON_UNEQUIP", player);
        activeScenario.put("energy_delta", Integer.toString(
            player.energy.energyMaster - energyBefore
        ));
        activeScenario.put("hand_delta", Integer.toString(player.masterHandSize - handBefore));
        activeScenario.put("necronomicurse_count", Integer.toString(curses));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicCounterProbe(String relicId, int value) {
        String gameId = RELIC_ALLOWLIST.get(relicId.toUpperCase(Locale.ROOT));
        AbstractRelic prototype = gameId == null ? null : RelicLibrary.getRelic(gameId);
        if (prototype == null) throw new IllegalArgumentException("invalid relic counter probe");
        AbstractRelic relic = prototype.makeCopy();
        relic.setCounter(value);
        activate("relic_counter_probe:" + relicId.toUpperCase(Locale.ROOT) + ":" + value,
            "STOCK_RELIC_SET_COUNTER", AbstractDungeon.player);
        activeScenario.put("counter", Integer.toString(relic.counter));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicRewardProbe(String relicId, int value) {
        String gameId = RELIC_ALLOWLIST.get(relicId.toUpperCase(Locale.ROOT));
        AbstractRelic prototype = gameId == null ? null : RelicLibrary.getRelic(gameId);
        if (prototype == null) throw new IllegalArgumentException("invalid relic reward probe");
        AbstractRelic relic = prototype.makeCopy();
        int result = "NLOTHS_GIFT".equalsIgnoreCase(relicId)
            ? relic.changeRareCardRewardChance(value)
            : relic.changeNumberOfCardsInReward(value);
        activate("relic_reward_probe:" + relicId.toUpperCase(Locale.ROOT) + ":" + value,
            "STOCK_RELIC_REWARD_MODIFIER", AbstractDungeon.player);
        activeScenario.put("result", Integer.toString(result));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicHealProbe(String relicId, int value) {
        String gameId = RELIC_ALLOWLIST.get(relicId.toUpperCase(Locale.ROOT));
        AbstractRelic prototype = gameId == null ? null : RelicLibrary.getRelic(gameId);
        if (prototype == null) throw new IllegalArgumentException("invalid relic heal probe");
        int result = prototype.makeCopy().onPlayerHeal(value);
        activate("relic_heal_probe:" + relicId.toUpperCase(Locale.ROOT) + ":" + value,
            "STOCK_RELIC_ON_PLAYER_HEAL", AbstractDungeon.player);
        activeScenario.put("result", Integer.toString(result));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Invoke callbacks whose stock implementation changes presentation state only. */
    private static void applyRelicNeutralProbe(String relicId, String callback) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        String normalizedCallback = callback.toUpperCase(Locale.ROOT);
        if (!POLICY_NEUTRAL_RELIC_CALLBACKS.contains(
                normalizedRelic + ":" + normalizedCallback)) {
            throw new IllegalArgumentException("callback is not policy-neutral allowlisted");
        }
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic prototype = gameId == null ? null : RelicLibrary.getRelic(gameId);
        if (prototype == null) throw new IllegalArgumentException("invalid relic neutral probe");
        AbstractPlayer player = AbstractDungeon.player;
        AbstractRelic relic = prototype.makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        String before = setupDigest(player);
        AbstractRoom previousRoom = AbstractDungeon.getCurrRoom();
        AbstractRoom probeRoom = previousRoom;
        if ("BLACK_STAR".equals(normalizedRelic)) probeRoom = new MonsterRoomElite();
        else if ("CURSED_KEY".equals(normalizedRelic)) probeRoom = new TreasureRoom();
        else if ("ONENTERROOM".equals(normalizedCallback)) probeRoom = new ShopRoom();
        try {
            if ("JUSTENTEREDROOM".equals(normalizedCallback)) {
                relic.justEnteredRoom(probeRoom);
            } else if ("ONENTERROOM".equals(normalizedCallback)) {
                relic.onEnterRoom(probeRoom);
            } else if ("ONVICTORY".equals(normalizedCallback)) {
                AbstractDungeon.currMapNode.room = probeRoom;
                relic.onVictory();
            } else {
                throw new IllegalArgumentException("unsupported policy-neutral callback");
            }
        } finally {
            AbstractDungeon.currMapNode.room = previousRoom;
        }
        String after = setupDigest(player);
        activate("relic_neutral_probe:" + normalizedRelic + ":" + normalizedCallback,
            "STOCK_RELIC_POLICY_NEUTRAL_CALLBACK", player);
        activeScenario.put("policy_state_unchanged", Boolean.toString(before.equals(after)));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicVictoryProbe(String relicId, int initialCounter) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!VICTORY_COUNTER_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic victory probe is not allowlisted");
        }
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        relic.counter = initialCounter;
        relic.onVictory();
        activate("relic_victory_probe:" + normalizedRelic + ":" + initialCounter,
            "STOCK_RELIC_ON_VICTORY_COUNTER_RESET", AbstractDungeon.player);
        activeScenario.put("counter", Integer.toString(relic.counter));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicCampfireProbe(String relicId, String preset) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        String normalizedPreset = preset.toUpperCase(Locale.ROOT);
        if (!CAMPFIRE_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic campfire probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.masterDeck.clear();
        if (!"EMPTY".equals(normalizedPreset)) {
            player.masterDeck.addToBottom(card("Strike_R"));
        }
        activate("relic_campfire_probe:" + normalizedRelic + ":" + normalizedPreset,
            "STOCK_RELIC_CAMPFIRE_CALLBACK", player);
        if ("COFFEE_DRIPPER".equals(normalizedRelic)
                || "FUSION_HAMMER".equals(normalizedRelic)) {
            AbstractCampfireOption option = "REST".equals(normalizedPreset)
                ? new RestOption(true) : new SmithOption(true);
            boolean result = relic.canUseCampfireOption(option);
            activeScenario.put("result", Boolean.toString(result));
            activeScenario.put("usable", Boolean.toString(option.usable));
            activeScenario.put("option_type", option.getClass().getSimpleName());
        } else {
            if ("GIRYA".equals(normalizedRelic)) {
                relic.counter = "MAX".equals(normalizedPreset) ? 3 : 0;
            }
            ArrayList<AbstractCampfireOption> options =
                new ArrayList<AbstractCampfireOption>();
            relic.addCampfireOption(options);
            if (options.size() != 1) {
                throw new IllegalStateException("campfire relic did not add exactly one option");
            }
            AbstractCampfireOption option = options.get(0);
            activeScenario.put("result", "true");
            activeScenario.put("usable", Boolean.toString(option.usable));
            activeScenario.put("option_type", option.getClass().getSimpleName());
        }
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicResourceProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!RESOURCE_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic resource probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.currentHealth = 40;
        player.maxHealth = 80;
        player.gold = 100;
        player.masterDeck.clear();
        for (int index = 0; index < 10; ++index) {
            player.masterDeck.addToBottom(card("Strike_R"));
        }
        int hpBefore = player.currentHealth;
        int goldBefore = player.gold;
        if ("BLOODY_IDOL".equals(normalizedRelic)) {
            player.gainGold(10);
        } else if ("ETERNAL_FEATHER".equals(normalizedRelic)) {
            relic.onEnterRoom(new RestRoom());
        } else {
            relic.onEnterRoom(new EventRoom());
        }
        activate("relic_resource_probe:" + normalizedRelic,
            "STOCK_RELIC_RESOURCE_CALLBACK", player);
        activeScenario.put("hp_delta", Integer.toString(player.currentHealth - hpBefore));
        activeScenario.put("gold_delta", Integer.toString(player.gold - goldBefore));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static int powerAmount(AbstractPlayer player, String powerId) {
        return player.getPower(powerId) == null ? 0 : player.getPower(powerId).amount;
    }

    private static int powerAmount(AbstractCreature creature, String powerId) {
        return creature.getPower(powerId) == null ? 0 : creature.getPower(powerId).amount;
    }

    /** Focused stock onUseCard probe; primary card effects are excluded by projection. */
    private static void applyRelicCardUseProbe(String relicId) {
        ParityRng.resetRelicProbeStreams();
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!CARD_USE_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic card-use probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        normalizeProbeTarget();
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.currentHealth = 40;
        player.maxHealth = 80;
        player.energy.energy = 10;
        EnergyPanel.setEnergy(10);
        int repetitions = 1;
        AbstractCard probeCard = card("Strike_R");
        if ("BIRD_FACED_URN".equals(normalizedRelic)) {
            probeCard = card("Inflame");
        } else if ("MUMMIFIED_HAND".equals(normalizedRelic)) {
            probeCard = card("Inflame");
            player.hand.addToBottom(card("Strike_R"));
            player.hand.addToBottom(card("Defend_R"));
        } else if ("ORANGE_PELLETS".equals(normalizedRelic)) {
            player.powers.add(new WeakPower(player, 2, true));
            repetitions = 3;
        } else if ("NECRONOMICON".equals(normalizedRelic)) {
            probeCard = card("Bash");
            relic.atTurnStart();
        } else if ("BLUE_CANDLE".equals(normalizedRelic)) {
            probeCard = card("Necronomicurse");
        } else if ("LETTER_OPENER".equals(normalizedRelic)) {
            probeCard = card("Defend_R");
            repetitions = 3;
        } else if ("MEDICAL_KIT".equals(normalizedRelic)) {
            probeCard = card("Wound");
        } else if ("KUNAI".equals(normalizedRelic)
                || "ORNAMENTAL_FAN".equals(normalizedRelic)
                || "SHURIKEN".equals(normalizedRelic)) {
            repetitions = 3;
        }
        if ("INK_BOTTLE".equals(normalizedRelic)) relic.counter = 9;
        else if ("NUNCHAKU".equals(normalizedRelic)) relic.counter = 9;
        else if ("PEN_NIB".equals(normalizedRelic)) relic.counter = 8;
        else if ("KUNAI".equals(normalizedRelic)
                || "LETTER_OPENER".equals(normalizedRelic)
                || "ORNAMENTAL_FAN".equals(normalizedRelic)
                || "SHURIKEN".equals(normalizedRelic)) relic.counter = 0;
        player.drawPile.clear();
        player.drawPile.addToBottom(card("Defend_R"));
        int hpBefore = player.currentHealth;
        int energyBefore = EnergyPanel.totalCount;
        int blockBefore = player.currentBlock;
        int drawBefore = player.drawPile.size();
        AbstractMonster monster = AbstractDungeon.getMonsters().monsters.get(0);
        int monsterBefore = monster.currentHealth;
        UseCardAction lastAction = null;
        AbstractCard lastCard = null;
        boolean checkBefore = !"NECRONOMICON".equals(normalizedRelic)
            || relic.checkTrigger();
        boolean duplicated = false;
        for (int index = 0; index < repetitions; ++index) {
            if ("ORANGE_PELLETS".equals(normalizedRelic)) {
                probeCard = index == 0 ? card("Strike_R")
                    : index == 1 ? card("Defend_R") : card("Inflame");
            }
            lastCard = probeCard.makeCopy();
            lastAction = new UseCardAction(lastCard, monster);
            if ("NECRONOMICON".equals(normalizedRelic)) {
                duplicated = !AbstractDungeon.actionManager.cardQueue.isEmpty();
            }
            drainActions();
        }
        activate("relic_card_use_probe:" + normalizedRelic,
            "STOCK_RELIC_ON_USE_CARD", player);
        activeScenario.put("counter", Integer.toString(relic.counter));
        activeScenario.put("hp_delta", Integer.toString(player.currentHealth - hpBefore));
        activeScenario.put("energy_bonus", Integer.toString(EnergyPanel.totalCount - energyBefore));
        activeScenario.put("block_delta", Integer.toString(player.currentBlock - blockBefore));
        activeScenario.put("drawn", Integer.toString(drawBefore - player.drawPile.size()));
        activeScenario.put("monster_hp_delta", Integer.toString(monster.currentHealth - monsterBefore));
        activeScenario.put("dexterity", Integer.toString(powerAmount(player, "Dexterity")));
        activeScenario.put("strength", Integer.toString(powerAmount(player, "Strength")));
        activeScenario.put("pen_nib", Integer.toString(powerAmount(player, "Pen Nib")));
        activeScenario.put("exhaust", Boolean.toString(
            lastAction != null && lastAction.exhaustCard && lastCard != null && lastCard.exhaust
        ));
        String zeroCard = "";
        for (AbstractCard item : player.hand.group) {
            if (item.costForTurn == 0) zeroCard = item.cardID;
        }
        activeScenario.put("zero_card", zeroCard);
        activeScenario.put("weak", Integer.toString(powerAmount(player, "Weak")));
        activeScenario.put("check_before", Boolean.toString(checkBefore));
        activeScenario.put("check_after", Boolean.toString(
            !"NECRONOMICON".equals(normalizedRelic) || relic.checkTrigger()
        ));
        activeScenario.put("duplicated", Boolean.toString(duplicated));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicObtainCardProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!OBTAIN_CARD_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic obtain-card probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.masterDeck.clear();
        player.currentHealth = 40;
        player.maxHealth = 80;
        player.gold = 100;
        AbstractCard probe = card("Strike_R");
        if ("DARKSTONE_PERIAPT".equals(normalizedRelic)) {
            probe = card("Injury");
        } else if ("FROZEN_EGG".equals(normalizedRelic)) {
            probe = card("Inflame");
        } else if ("TOXIC_EGG".equals(normalizedRelic)) {
            probe = card("Defend_R");
        }
        int hpBefore = player.currentHealth;
        int maxHpBefore = player.maxHealth;
        int goldBefore = player.gold;
        AbstractCard obtain = probe.makeCopy();
        relic.onObtainCard(obtain);
        AbstractCard preview = probe.makeCopy();
        if ("FROZEN_EGG".equals(normalizedRelic)
                || "MOLTEN_EGG".equals(normalizedRelic)
                || "TOXIC_EGG".equals(normalizedRelic)) {
            relic.onPreviewObtainCard(preview);
        }
        activate("relic_obtain_card_probe:" + normalizedRelic,
            "STOCK_RELIC_OBTAIN_CARD_CALLBACK", player);
        activeScenario.put("obtain_upgrades", Integer.toString(obtain.timesUpgraded));
        activeScenario.put("preview_upgrades", Integer.toString(preview.timesUpgraded));
        activeScenario.put("hp_delta", Integer.toString(player.currentHealth - hpBefore));
        activeScenario.put("max_hp_delta", Integer.toString(player.maxHealth - maxHpBefore));
        activeScenario.put("gold_delta", Integer.toString(player.gold - goldBefore));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicHpLossProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!HP_LOSS_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic hp-loss probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.currentHealth = 60;
        player.maxHealth = 80;
        player.hand.clear();
        player.drawPile.clear();
        for (int index = 0; index < 5; ++index) {
            player.drawPile.addToBottom(card("Defend_R"));
        }
        relic.atPreBattle();
        int hpBefore = player.currentHealth;
        int drawBefore = player.drawPile.size();
        player.damage(new DamageInfo(player, 5, DamageInfo.DamageType.HP_LOSS));
        drainActions();
        activate("relic_hp_loss_probe:" + normalizedRelic,
            "STOCK_RELIC_WAS_HP_LOST", player);
        activeScenario.put("hp_delta", Integer.toString(player.currentHealth - hpBefore));
        activeScenario.put("drawn", Integer.toString(drawBefore - player.drawPile.size()));
        activeScenario.put("next_turn_block", Integer.toString(
            powerAmount(player, "Next Turn Block")
        ));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicVictoryResourceProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!VICTORY_RESOURCE_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic victory-resource probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.currentHealth = 40;
        player.maxHealth = 80;
        int hpBefore = player.currentHealth;
        int maxHpBefore = player.maxHealth;
        if ("MEAT_ON_THE_BONE".equals(normalizedRelic)) {
            relic.onBloodied();
            relic.onNotBloodied();
            relic.onTrigger();
        } else {
            relic.onVictory();
        }
        drainActions();
        activate("relic_victory_resource_probe:" + normalizedRelic,
            "STOCK_RELIC_VICTORY_RESOURCE", player);
        activeScenario.put("hp_delta", Integer.toString(player.currentHealth - hpBefore));
        activeScenario.put("max_hp_delta", Integer.toString(player.maxHealth - maxHpBefore));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicDamageProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!"STRIKE_DUMMY".equals(normalizedRelic)
                && !"THE_BOOT".equals(normalizedRelic)) {
            throw new IllegalArgumentException("relic damage probe is not allowlisted");
        }
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        int result;
        if ("STRIKE_DUMMY".equals(normalizedRelic)) {
            result = Math.round(relic.atDamageModify(6.0f, card("Strike_R")));
        } else {
            DamageInfo info = new DamageInfo(AbstractDungeon.player, 4, DamageInfo.DamageType.NORMAL);
            result = relic.onAttackToChangeDamage(info, 4);
        }
        activate("relic_damage_probe:" + normalizedRelic,
            "STOCK_RELIC_DAMAGE_MODIFIER", AbstractDungeon.player);
        activeScenario.put("damage", Integer.toString(result));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicShuffleProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!"SUNDIAL".equals(normalizedRelic)
                && !"THE_ABACUS".equals(normalizedRelic)) {
            throw new IllegalArgumentException("relic shuffle probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.energy.energy = 10;
        EnergyPanel.setEnergy(10);
        if ("SUNDIAL".equals(normalizedRelic)) relic.counter = 2;
        int energyBefore = EnergyPanel.totalCount;
        int blockBefore = player.currentBlock;
        relic.onShuffle();
        drainActions();
        activate("relic_shuffle_probe:" + normalizedRelic,
            "STOCK_RELIC_ON_SHUFFLE", player);
        activeScenario.put("counter", Integer.toString(relic.counter));
        activeScenario.put("energy_delta", Integer.toString(EnergyPanel.totalCount - energyBefore));
        activeScenario.put("block_delta", Integer.toString(player.currentBlock - blockBefore));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicSpecialResourceProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!"TOY_ORNITHOPTER".equals(normalizedRelic)
                && !"MAW_BANK".equals(normalizedRelic)) {
            throw new IllegalArgumentException("special resource probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.currentHealth = 40;
        player.maxHealth = 80;
        player.gold = 100;
        int hpBefore = player.currentHealth;
        int goldBefore = player.gold;
        int firstGain = 0;
        int secondGain = 0;
        boolean used = false;
        if ("TOY_ORNITHOPTER".equals(normalizedRelic)) {
            relic.onUsePotion();
            drainActions();
        } else {
            relic.onEnterRoom(AbstractDungeon.getCurrRoom());
            firstGain = player.gold - goldBefore;
            relic.onSpendGold();
            used = relic.counter == -2;
            int beforeSecond = player.gold;
            relic.onEnterRoom(AbstractDungeon.getCurrRoom());
            secondGain = player.gold - beforeSecond;
        }
        activate("relic_special_resource_probe:" + normalizedRelic,
            "STOCK_RELIC_SPECIAL_RESOURCE", player);
        activeScenario.put("toy_heal", Integer.toString(player.currentHealth - hpBefore));
        activeScenario.put("first_gain", Integer.toString(firstGain));
        activeScenario.put("second_gain", Integer.toString(secondGain));
        activeScenario.put("used", Boolean.toString(used));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicTurnStateProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!Arrays.asList("ANCIENT_TEA_SET", "ART_OF_WAR", "POCKETWATCH",
                "VELVET_CHOKER").contains(normalizedRelic)) {
            throw new IllegalArgumentException("turn-state probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        player.energy.energy = 10;
        EnergyPanel.setEnergy(10);
        int energyBefore = EnergyPanel.totalCount;
        int attackBonus = 0;
        int skillBonus = 0;
        boolean canPlay = true;
        if ("ANCIENT_TEA_SET".equals(normalizedRelic)) {
            relic.onEnterRestRoom();
            relic.atPreBattle();
            relic.atTurnStart();
            drainActions();
        } else if ("POCKETWATCH".equals(normalizedRelic)) {
            relic.atBattleStart();
            relic.onPlayCard(card("Strike_R"), null);
        } else if ("VELVET_CHOKER".equals(normalizedRelic)) {
            relic.atTurnStart();
            for (int index = 0; index < 6; ++index) {
                relic.onPlayCard(card("Strike_R"), null);
            }
            canPlay = relic.canPlay(card("Strike_R"));
        } else {
            relic.atPreBattle();
            relic.atTurnStart();
            new UseCardAction(card("Strike_R"), null);
            energyBefore = EnergyPanel.totalCount;
            relic.atTurnStart();
            drainActions();
            attackBonus = EnergyPanel.totalCount - energyBefore;
            relic.onVictory();

            AbstractRelic skillRelic = RelicLibrary.getRelic(gameId).makeCopy();
            player.relics.clear();
            player.relics.add(skillRelic);
            player.energy.energy = 10;
            EnergyPanel.setEnergy(10);
            skillRelic.atPreBattle();
            skillRelic.atTurnStart();
            new UseCardAction(card("Defend_R"), null);
            energyBefore = EnergyPanel.totalCount;
            skillRelic.atTurnStart();
            drainActions();
            skillBonus = EnergyPanel.totalCount - energyBefore;
        }
        activate("relic_turn_state_probe:" + normalizedRelic,
            "STOCK_RELIC_TURN_STATE", player);
        activeScenario.put("counter", Integer.toString(relic.counter));
        activeScenario.put("energy_delta", Integer.toString(EnergyPanel.totalCount - energyBefore));
        activeScenario.put("attack_bonus", Integer.toString(attackBonus));
        activeScenario.put("skill_bonus", Integer.toString(skillBonus));
        activeScenario.put("can_play", Boolean.toString(canPlay));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void applyRelicEndTurnProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!Arrays.asList("NILRYS_CODEX", "ORICHALCUM", "STONE_CALENDAR", "SLAVERS_COLLAR")
                .contains(normalizedRelic)) {
            throw new IllegalArgumentException("end-turn probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        normalizeProbeTarget();
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        int blockBefore = player.currentBlock;
        int monsterBefore = AbstractDungeon.getMonsters().monsters.get(0).currentHealth;
        int roundedBlock = 0;
        int eliteEnergyBonus = 0;
        int persistentEnergyDelta = 0;
        int optionCount = 0;
        if ("NILRYS_CODEX".equals(normalizedRelic)) {
            relic.onPlayerEndTurn();
            drainActions();
            optionCount = AbstractDungeon.cardRewardScreen.rewardGroup.size();
            if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
            AbstractDungeon.isScreenUp = false;
        } else if ("ORICHALCUM".equals(normalizedRelic)) {
            relic.onPlayerEndTurn();
            drainActions();
            roundedBlock = relic.onPlayerGainedBlock(2.8f);
            relic.onVictory();
        } else if ("STONE_CALENDAR".equals(normalizedRelic)) {
            relic.counter = 7;
            relic.onPlayerEndTurn();
            drainActions();
        } else {
            int before = player.energy.energyMaster;
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            boolean previousEliteTrigger = room.eliteTrigger;
            room.eliteTrigger = true;
            try {
                ((SlaversCollar)relic).beforeEnergyPrep();
                eliteEnergyBonus = player.energy.energyMaster - before;
                relic.onVictory();
                persistentEnergyDelta = player.energy.energyMaster - before;
            } finally {
                room.eliteTrigger = previousEliteTrigger;
            }
        }
        activate("relic_end_turn_probe:" + normalizedRelic,
            "STOCK_RELIC_END_TURN_LIFECYCLE", player);
        activeScenario.put("block_delta", Integer.toString(player.currentBlock - blockBefore));
        activeScenario.put("monster_hp_delta", Integer.toString(
            AbstractDungeon.getMonsters().monsters.get(0).currentHealth - monsterBefore
        ));
        activeScenario.put("rounded_block", Integer.toString(roundedBlock));
        activeScenario.put("elite_energy_bonus", Integer.toString(eliteEnergyBonus));
        activeScenario.put("persistent_energy_delta", Integer.toString(persistentEnergyDelta));
        activeScenario.put("option_count", Integer.toString(optionCount));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Focused direct trigger probes for stock relic callbacks with observable effects. */
    private static void applyRelicTriggerProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!Arrays.asList("CHAMPION_BELT", "CHARONS_ASHES", "DEAD_BRANCH",
                "GREMLIN_HORN", "HAND_DRILL", "LIZARD_TAIL", "RED_SKULL",
                "UNCEASING_TOP").contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic trigger probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        normalizeProbeTarget();
        AbstractMonster monster = AbstractDungeon.getMonsters().monsters.get(0);
        String gameId = RELIC_ALLOWLIST.get(normalizedRelic);
        AbstractRelic relic = RelicLibrary.getRelic(gameId).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        monster.currentHealth = 999;
        monster.maxHealth = 999;
        int monsterBefore = monster.currentHealth;
        int hpBefore = player.currentHealth;
        int handBefore = player.hand.size();
        int energyBefore = EnergyPanel.totalCount;
        int strengthOn = 0;
        if ("CHAMPION_BELT".equals(normalizedRelic)) {
            relic.onTrigger(monster);
            drainActions();
        } else if ("CHARONS_ASHES".equals(normalizedRelic)) {
            relic.onExhaust(card("Defend_R"));
            drainActions();
        } else if ("DEAD_BRANCH".equals(normalizedRelic)) {
            handBefore = player.hand.size();
            relic.onExhaust(card("Defend_R"));
            drainActions();
        } else if ("GREMLIN_HORN".equals(normalizedRelic)) {
            player.hand.clear();
            player.drawPile.clear();
            player.drawPile.addToTop(card("Defend_R"));
            player.energy.energy = 3;
            EnergyPanel.setEnergy(3);
            handBefore = player.hand.size();
            energyBefore = EnergyPanel.totalCount;
            MonsterGroup previous = AbstractDungeon.getMonsters();
            MonsterGroup pair = MonsterHelper.getEncounter("2 Louse");
            AbstractDungeon.getCurrRoom().monsters = pair;
            AbstractMonster dead = pair.monsters.get(0);
            dead.currentHealth = 0;
            try {
                relic.onMonsterDeath(dead);
                drainActions();
            } finally {
                AbstractDungeon.getCurrRoom().monsters = previous;
            }
        } else if ("HAND_DRILL".equals(normalizedRelic)) {
            relic.onBlockBroken(monster);
            drainActions();
        } else if ("LIZARD_TAIL".equals(normalizedRelic)) {
            player.currentHealth = 0;
            hpBefore = player.currentHealth;
            relic.onTrigger();
            drainActions();
        } else if ("RED_SKULL".equals(normalizedRelic)) {
            relic.onBloodied();
            drainActions();
            strengthOn = powerAmount(player, "Strength");
            relic.onNotBloodied();
            drainActions();
            relic.onVictory();
        }
        if ("UNCEASING_TOP".equals(normalizedRelic)) {
            player.hand.clear();
            player.drawPile.clear();
            player.drawPile.addToTop(card("Defend_R"));
            handBefore = player.hand.size();
            relic.atTurnStart();
            relic.onRefreshHand();
            drainActions();
        }
        activate("relic_trigger_probe:" + normalizedRelic,
            "STOCK_RELIC_TRIGGER_CALLBACK", player);
        activeScenario.put("weak", Integer.toString(powerAmount(monster, "Weakened")));
        activeScenario.put("vulnerable", Integer.toString(powerAmount(monster, "Vulnerable")));
        activeScenario.put("monster_hp_delta", Integer.toString(monster.currentHealth - monsterBefore));
        activeScenario.put("hp_delta", Integer.toString(player.currentHealth - hpBefore));
        activeScenario.put("hp_after", Integer.toString(player.currentHealth));
        activeScenario.put("counter", Integer.toString(relic.counter));
        activeScenario.put("strength_on", Integer.toString(strengthOn));
        activeScenario.put("strength_after", Integer.toString(powerAmount(player, "Strength")));
        activeScenario.put("hand_delta", Integer.toString(player.hand.size() - handBefore));
        activeScenario.put("energy_delta", Integer.toString(EnergyPanel.totalCount - energyBefore));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Focused world-state probes for chest and master-deck callbacks. */
    private static void applyRelicWorldProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!Arrays.asList("CURSED_KEY", "DU_VU_DOLL", "MATRYOSHKA",
                "MEAL_TICKET", "NLOTHS_HUNGRY_FACE").contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic world probe is not allowlisted");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        AbstractRelic relic = RelicLibrary.getRelic(
            RELIC_ALLOWLIST.get(normalizedRelic)).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        room.rewards.clear();
        int delta = 0;
        int value = relic.counter;
        boolean used = false;
        if ("CURSED_KEY".equals(normalizedRelic)) {
            int before = AbstractDungeon.topLevelEffects.size();
            relic.onChestOpen(false);
            delta = AbstractDungeon.topLevelEffects.size() - before;
        } else if ("DU_VU_DOLL".equals(normalizedRelic)) {
            player.masterDeck.clear();
            player.masterDeck.addToBottom(card("Injury"));
            relic.onMasterDeckChange();
            value = relic.counter;
        } else if ("MATRYOSHKA".equals(normalizedRelic)) {
            int before = room.rewards.size();
            relic.onChestOpen(false);
            delta = room.rewards.size() - before;
            value = relic.counter;
            used = relic.counter == -2;
        } else if ("MEAL_TICKET".equals(normalizedRelic)) {
            player.currentHealth = 40;
            player.maxHealth = 80;
            int before = player.currentHealth;
            relic.justEnteredRoom(new ShopRoom());
            delta = player.currentHealth - before;
        } else {
            room.rewards.add(new RewardItem(RelicLibrary.getRelic("Anchor").makeCopy()));
            room.rewards.add(new RewardItem(RelicLibrary.getRelic("Blood Vial").makeCopy()));
            int before = room.rewards.size();
            relic.onChestOpenAfter(false);
            delta = room.rewards.size() - before;
            value = relic.counter;
            used = relic.counter == -2;
        }
        activate("relic_world_probe:" + normalizedRelic,
            "STOCK_RELIC_WORLD_CALLBACK", player);
        activeScenario.put("delta", Integer.toString(delta));
        activeScenario.put("value", Integer.toString(value));
        activeScenario.put("used", Boolean.toString(used));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static int upgradedCards(AbstractPlayer player) {
        int count = 0;
        for (AbstractCard item : player.masterDeck.group) if (item.upgraded) ++count;
        return count;
    }

    /** Full stock acquisition boundary for relics whose onEquip opens UI. */
    private static void applyRelicEquipProbe(String relicId) {
        String normalizedRelic = relicId.toUpperCase(Locale.ROOT);
        if (!INTERACTIVE_EQUIP_RELICS.contains(normalizedRelic)) {
            throw new IllegalArgumentException("relic equip probe is not allowlisted");
        }
        ParityRng.resetRelicProbeStreams();
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
        AbstractDungeon.getCurrRoom().rewards.clear();
        AbstractDungeon.combatRewardScreen.rewards.clear();
        player.masterDeck.clear();
        for (int i = 0; i < 4; ++i) player.masterDeck.addToBottom(card("Strike_R"));
        for (int i = 0; i < 4; ++i) player.masterDeck.addToBottom(card("Defend_R"));
        player.masterDeck.addToBottom(card("Bash"));
        player.masterDeck.addToBottom(card("Inflame"));
        player.currentHealth = 40;
        player.maxHealth = 80;
        player.gold = 100;
        AbstractRelic relic = RelicLibrary.getRelic(
            RELIC_ALLOWLIST.get(normalizedRelic)).makeCopy();
        player.relics.clear();
        player.relics.add(relic);
        int deckBefore = player.masterDeck.size();
        int upgradedBefore = upgradedCards(player);
        int maxHpBefore = player.maxHealth;
        relic.onEquip();
        int optionCount = AbstractDungeon.gridSelectScreen.targetGroup == null
            ? 0 : AbstractDungeon.gridSelectScreen.targetGroup.size();
        int affected = 0;
        int marked = 0;
        int unmarked = 0;
        if ("ASTROLABE".equals(normalizedRelic)) {
            for (int i = 0; i < 3; ++i) {
                AbstractDungeon.gridSelectScreen.selectedCards.add(
                    AbstractDungeon.gridSelectScreen.targetGroup.group.get(i));
            }
            relic.update();
            affected = 3;
        } else if (Arrays.asList("BOTTLED_FLAME", "BOTTLED_LIGHTNING",
                "BOTTLED_TORNADO").contains(normalizedRelic)) {
            AbstractCard selected = AbstractDungeon.gridSelectScreen.targetGroup.group.get(0);
            AbstractDungeon.gridSelectScreen.selectedCards.add(selected);
            relic.update();
            marked = (selected.inBottleFlame || selected.inBottleLightning || selected.inBottleTornado) ? 1 : 0;
            relic.onUnequip();
            unmarked = (selected.inBottleFlame || selected.inBottleLightning || selected.inBottleTornado) ? 0 : 1;
            affected = 1;
        } else if ("CALLING_BELL".equals(normalizedRelic)) {
            affected = optionCount;
            AbstractDungeon.closeCurrentScreen();
            AbstractDungeon.isScreenUp = false;
            AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
            relic.update();
        } else if ("DOLLYS_MIRROR".equals(normalizedRelic)) {
            AbstractDungeon.gridSelectScreen.selectedCards.add(
                AbstractDungeon.gridSelectScreen.targetGroup.group.get(0));
            relic.update();
            affected = 1;
        } else if ("EMPTY_CAGE".equals(normalizedRelic)) {
            AbstractDungeon.gridSelectScreen.selectedCards.add(
                AbstractDungeon.gridSelectScreen.targetGroup.group.get(0));
            AbstractDungeon.gridSelectScreen.selectedCards.add(
                AbstractDungeon.gridSelectScreen.targetGroup.group.get(1));
            relic.update();
            affected = deckBefore - player.masterDeck.size();
        } else if ("PANDORAS_BOX".equals(normalizedRelic)) {
            affected = deckBefore - player.masterDeck.size();
        }
        int relicRewards = 0;
        int potionRewards = 0;
        int cardRewards = 0;
        int goldReward = 0;
        for (RewardItem reward : AbstractDungeon.combatRewardScreen.rewards) {
            if (reward.type == RewardItem.RewardType.RELIC) ++relicRewards;
            else if (reward.type == RewardItem.RewardType.POTION) ++potionRewards;
            else if (reward.type == RewardItem.RewardType.CARD) ++cardRewards;
            else if (reward.type == RewardItem.RewardType.GOLD) goldReward += reward.goldAmt;
        }
        activate("relic_equip_probe:" + normalizedRelic,
            "STOCK_RELIC_ON_EQUIP_UI_LIFECYCLE", player);
        activeScenario.put("option_count", Integer.toString(optionCount));
        activeScenario.put("affected", Integer.toString(affected));
        activeScenario.put("marked", Integer.toString(marked));
        activeScenario.put("unmarked", Integer.toString(unmarked));
        activeScenario.put("relic_rewards", Integer.toString(relicRewards));
        activeScenario.put("potion_rewards", Integer.toString(potionRewards));
        activeScenario.put("card_rewards", Integer.toString(cardRewards));
        activeScenario.put("gold_reward", Integer.toString(goldReward));
        activeScenario.put("max_hp_delta", Integer.toString(player.maxHealth - maxHpBefore));
        activeScenario.put("upgraded_delta", Integer.toString(upgradedCards(player) - upgradedBefore));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Construct a stock encounter using the live stock RNG streams. */
    private static void applyEncounterProbe(String encounterId) {
        String gameId = ENCOUNTER_ALLOWLIST.get(encounterId.toUpperCase(Locale.ROOT));
        if (gameId == null) {
            throw new IllegalArgumentException("parity_encounter is not in the Act 1 allowlist");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        installProbeRelics(player, CardType.SKILL);
        player.currentHealth = 80;
        player.maxHealth = 80;
        player.hand.addToBottom(card("Defend_R"));
        player.hand.addToBottom(card("Strike_R"));
        player.drawPile.addToBottom(card("Defend_R"));
        player.drawPile.addToTop(card("Strike_R"));
        MonsterGroup monsters = MonsterHelper.getEncounter(gameId);
        AbstractDungeon.getCurrRoom().monsters = monsters;
        monsters.init();
        monsters.usePreBattleAction();
        monsters.showIntent();
        activate("encounter_probe:" + encounterId.toUpperCase(Locale.ROOT),
            "STOCK_MONSTER_HELPER:ACT1_ALLOWLIST", player);
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    private static void drainActions() {
        // Commands may arrive inside an unusually short render frame. Stock
        // actions decrement duration by that frame's delta, so retain a hard
        // failure bound but allow enough updates for the smallest observed
        // delta instead of making the semantic probe timing-flaky.
        for (int update = 0; update < 4096; ++update) {
            AbstractDungeon.actionManager.update();
            if (AbstractDungeon.actionManager.actions.isEmpty()
                    && AbstractDungeon.actionManager.currentAction == null) return;
        }
        throw new IllegalStateException("engine probe actions did not drain");
    }

    /** Execute stock stance/orb primitives without requiring character cards. */
    private static void applyEngineProbe(String engineId) {
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        installProbeRelics(player, CardType.SKILL);
        normalizeProbeTarget();
        player.currentHealth = 80;
        player.maxHealth = 80;
        activate("engine_probe:" + engineId.toUpperCase(Locale.ROOT),
            "STOCK_SHARED_ENGINE", player);
        if ("stance".equalsIgnoreCase(engineId)) {
            player.energy.energy = 0;
            EnergyPanel.setEnergy(0);
            player.stance = new CalmStance();
            AbstractDungeon.actionManager.addToBottom(new ChangeStanceAction("Wrath"));
            drainActions();
            activeScenario.put("calm_exit_energy", Integer.toString(EnergyPanel.totalCount));
            activeScenario.put("calm_exit_stance", player.stance.ID.toUpperCase(Locale.ROOT));

            clearCombatState(player);
            player.energy.energy = 2;
            EnergyPanel.setEnergy(2);
            player.stance = new NeutralStance();
            AbstractDungeon.actionManager.addToBottom(new ChangeStanceAction("Divinity"));
            drainActions();
            activeScenario.put("divinity_entry_energy", Integer.toString(EnergyPanel.totalCount));
            activeScenario.put("divinity_entry_stance", player.stance.ID.toUpperCase(Locale.ROOT));
        } else if ("orb".equalsIgnoreCase(engineId)) {
            player.orbs.clear();
            player.maxOrbs = 0;
            player.increaseMaxOrbSlots(1, false);
            player.energy.energy = 0;
            EnergyPanel.setEnergy(0);
            player.channelOrb(new Plasma());
            player.evokeOrb();
            drainActions();
            activeScenario.put("plasma_evoke_energy", Integer.toString(EnergyPanel.totalCount));

            player.orbs.clear();
            player.maxOrbs = 0;
            player.powers.clear();
            player.currentBlock = 0;
            player.increaseMaxOrbSlots(1, false);
            player.powers.add(new FocusPower(player, 2));
            player.channelOrb(new Frost());
            player.evokeOrb();
            drainActions();
            activeScenario.put("frost_evoke_block", Integer.toString(player.currentBlock));

            player.orbs.clear();
            player.maxOrbs = 0;
            for (int slot = 0; slot < 12; ++slot) player.increaseMaxOrbSlots(1, false);
            activeScenario.put("slot_cap", Integer.toString(player.maxOrbs));
        } else {
            throw new IllegalArgumentException("parity_engine requires STANCE or ORB");
        }
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    /** Construct one allowlisted stock event at a deterministic run boundary. */
    private static void applyEventProbe(String eventId) {
        String gameId = EVENT_ALLOWLIST.get(eventId.toUpperCase(Locale.ROOT));
        if (gameId == null || "NEOW".equalsIgnoreCase(eventId)) {
            throw new IllegalArgumentException("parity_event is not in the non-Neow scope");
        }
        AbstractPlayer player = AbstractDungeon.player;
        clearCombatState(player);
        installProbeRelics(player, CardType.SKILL);
        player.currentHealth = 80;
        player.maxHealth = 80;
        player.gold = 99;
        player.masterDeck.clear();
        for (int index = 0; index < 5; ++index) player.masterDeck.addToTop(card("Strike_R"));
        for (int index = 0; index < 4; ++index) player.masterDeck.addToTop(card("Defend_R"));
        player.masterDeck.addToTop(card("Bash"));
        player.potions.clear();
        for (int slot = 0; slot < player.potionSlots; ++slot) player.potions.add(new PotionSlot(slot));
        if ("NLOTH".equalsIgnoreCase(eventId)) {
            // N'loth is only generated when at least two relics exist. Supply
            // deterministic inert candidates in addition to Burning Blood so
            // its stock constructor precondition is represented explicitly.
            player.relics.add(RelicLibrary.getRelic("Anchor").makeCopy());
            player.relics.add(RelicLibrary.getRelic("Bag of Marbles").makeCopy());
        }
        // The first-combat presentation can still own a BattleStartEffect even
        // after CommunicationMod exposes the command boundary. It dereferences
        // the combat room on its next frame, so remove presentation-only
        // effects before replacing that room with the targeted event.
        AbstractDungeon.effectList.clear();
        AbstractDungeon.effectsQueue.clear();
        AbstractDungeon.topLevelEffects.clear();
        AbstractDungeon.topLevelEffectsQueue.clear();
        // A preceding event probe can legitimately leave a grid/reward overlay
        // open (for example Bonfire Spirits).  That overlay is presentation and
        // selection state owned by the old event; carrying it into the next
        // constructor makes the next independent probe observe the wrong
        // screen and may dispatch the old callback on a later frame.
        if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
        // RoomEventDialog stores options and its input latch globally. Normal
        // room traversal clears them; direct event replacement must do so too.
        RoomEventDialog.optionList.clear();
        RoomEventDialog.selectedOption = -1;
        RoomEventDialog.waitForInput = true;

        EventRoom room = new EventRoom();
        AbstractDungeon.currMapNode.room = room;
        AbstractEvent event = EventHelper.getEvent(gameId);
        if (event == null) throw new IllegalStateException("EventHelper returned null for " + gameId);
        // AbstractEvent.type is (surprisingly) global. EventHelper constructors
        // for legacy room-dialog events do not restore it after a preceding
        // image event, so protocol serializers can read the wrong dialog.
        if (event.hasDialog) AbstractEvent.type = AbstractEvent.EventType.ROOM;
        room.event = event;
        room.phase = AbstractRoom.RoomPhase.EVENT;
        event.onEnterRoom();
        // Direct room replacement bypasses the render transition that reveals
        // legacy RoomEventDialog events. Showing the already-constructed
        // dialog changes no options or event state; it only completes that
        // presentation lifecycle step. Image events follow their stock timer.
        if (event.hasDialog) event.roomEventText.show();
        activate("event_probe:" + eventId.toUpperCase(Locale.ROOT),
            "STOCK_EVENT_HELPER:SCOPED_ALLOWLIST", player);
        activeScenario.put("event_has_dialog", Boolean.toString(event.hasDialog));
        activeScenario.put("event_has_focus", Boolean.toString(event.hasFocus));
        activeScenario.put("event_wait_timer", Float.toString(event.waitTimer));
        activeScenario.put("event_option_count", Integer.toString(RoomEventDialog.optionList.size()));
        activeScenario.put("event_wait_for_input", Boolean.toString(RoomEventDialog.waitForInput));
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }

    @SpirePatch(clz = CommandExecutor.class, method = "getAvailableCommands")
    public static class Advertise {
        @SpirePostfixPatch
        public static ArrayList<String> Postfix(ArrayList<String> commands) {
            // A targeted event remains on a choose boundary, where the stock
            // combat "end" command is intentionally unavailable. Keep only
            // the event-reset probe advertised there so the next independent
            // event can be constructed in the same protected process.
            if (AbstractDungeon.player != null && !commands.contains(EVENT_PROBE_COMMAND)) {
                commands.add(EVENT_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_SPAWN_PROBE_COMMAND)) {
                commands.add(RELIC_SPAWN_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_UNEQUIP_PROBE_COMMAND)) {
                commands.add(RELIC_UNEQUIP_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_COUNTER_PROBE_COMMAND)) {
                commands.add(RELIC_COUNTER_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_REWARD_PROBE_COMMAND)) {
                commands.add(RELIC_REWARD_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_HEAL_PROBE_COMMAND)) {
                commands.add(RELIC_HEAL_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_NEUTRAL_PROBE_COMMAND)) {
                commands.add(RELIC_NEUTRAL_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_VICTORY_PROBE_COMMAND)) {
                commands.add(RELIC_VICTORY_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_CAMPFIRE_PROBE_COMMAND)) {
                commands.add(RELIC_CAMPFIRE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_RESOURCE_PROBE_COMMAND)) {
                commands.add(RELIC_RESOURCE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_CARD_USE_PROBE_COMMAND)) {
                commands.add(RELIC_CARD_USE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_OBTAIN_CARD_PROBE_COMMAND)) {
                commands.add(RELIC_OBTAIN_CARD_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_HP_LOSS_PROBE_COMMAND)) {
                commands.add(RELIC_HP_LOSS_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_VICTORY_RESOURCE_PROBE_COMMAND)) {
                commands.add(RELIC_VICTORY_RESOURCE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_DAMAGE_PROBE_COMMAND)) {
                commands.add(RELIC_DAMAGE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_SHUFFLE_PROBE_COMMAND)) {
                commands.add(RELIC_SHUFFLE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_SPECIAL_RESOURCE_PROBE_COMMAND)) {
                commands.add(RELIC_SPECIAL_RESOURCE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_TURN_STATE_PROBE_COMMAND)) {
                commands.add(RELIC_TURN_STATE_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_END_TURN_PROBE_COMMAND)) {
                commands.add(RELIC_END_TURN_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_TRIGGER_PROBE_COMMAND)) {
                commands.add(RELIC_TRIGGER_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_WORLD_PROBE_COMMAND)) {
                commands.add(RELIC_WORLD_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_EQUIP_PROBE_COMMAND)) {
                commands.add(RELIC_EQUIP_PROBE_COMMAND);
            }
            if (AbstractDungeon.player != null && !commands.contains(RELIC_PROBE_COMMAND)) {
                commands.add(RELIC_PROBE_COMMAND);
            }
            // CommunicationMod's stock availability check misses legacy
            // RoomEventDialog choices after a targeted room replacement even
            // though the stock option list is live and its choose executor is
            // valid. Advertise that existing semantic boundary explicitly.
            if (AbstractDungeon.player != null
                && AbstractDungeon.currMapNode != null
                && AbstractDungeon.getCurrRoom() instanceof EventRoom
                && !RoomEventDialog.optionList.isEmpty()
                && RoomEventDialog.waitForInput
                && !commands.contains("choose")) {
                commands.add("choose");
            }
            if (CommandExecutor.isEndCommandAvailable()) {
                if (!commands.contains(COMMAND)) commands.add(COMMAND);
                if (!commands.contains(CARD_PROBE_COMMAND)) commands.add(CARD_PROBE_COMMAND);
                if (!commands.contains(POTION_PROBE_COMMAND)) commands.add(POTION_PROBE_COMMAND);
                if (!commands.contains(RELIC_PROBE_COMMAND)) commands.add(RELIC_PROBE_COMMAND);
                if (!commands.contains(ENCOUNTER_PROBE_COMMAND)) commands.add(ENCOUNTER_PROBE_COMMAND);
                if (!commands.contains(ENGINE_PROBE_COMMAND)) commands.add(ENGINE_PROBE_COMMAND);
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
                if (normalized.startsWith(RELIC_EQUIP_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_equip requires RELIC_ID"
                    );
                    applyRelicEquipProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_WORLD_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_world requires RELIC_ID"
                    );
                    applyRelicWorldProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_TRIGGER_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_trigger requires RELIC_ID"
                    );
                    applyRelicTriggerProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_END_TURN_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_end_turn requires RELIC_ID"
                    );
                    applyRelicEndTurnProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_TURN_STATE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_turn_state requires RELIC_ID"
                    );
                    applyRelicTurnStateProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_SPECIAL_RESOURCE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_special_resource requires RELIC_ID"
                    );
                    applyRelicSpecialResourceProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_SHUFFLE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_shuffle requires RELIC_ID"
                    );
                    applyRelicShuffleProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_DAMAGE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) throw new IllegalArgumentException(
                        "parity_relic_damage requires RELIC_ID"
                    );
                    applyRelicDamageProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_VICTORY_RESOURCE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                            "parity_relic_victory_resource requires RELIC_ID"
                        );
                    }
                    applyRelicVictoryResourceProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_HP_LOSS_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                            "parity_relic_hp_loss requires RELIC_ID"
                        );
                    }
                    applyRelicHpLossProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_OBTAIN_CARD_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                            "parity_relic_obtain_card requires RELIC_ID"
                        );
                    }
                    applyRelicObtainCardProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_CARD_USE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                            "parity_relic_card_use requires RELIC_ID"
                        );
                    }
                    applyRelicCardUseProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_RESOURCE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                            "parity_relic_resource requires RELIC_ID"
                        );
                    }
                    applyRelicResourceProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_CAMPFIRE_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException(
                            "parity_relic_campfire requires RELIC_ID PRESET"
                        );
                    }
                    applyRelicCampfireProbe(parts[1], parts[2]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_VICTORY_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException(
                            "parity_relic_victory requires RELIC_ID INITIAL_COUNTER"
                        );
                    }
                    applyRelicVictoryProbe(parts[1], Integer.parseInt(parts[2]));
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_NEUTRAL_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException(
                            "parity_relic_neutral requires RELIC_ID CALLBACK"
                        );
                    }
                    applyRelicNeutralProbe(parts[1], parts[2]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_HEAL_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException(
                            "parity_relic_heal requires RELIC_ID VALUE"
                        );
                    }
                    applyRelicHealProbe(parts[1], Integer.parseInt(parts[2]));
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_REWARD_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException(
                            "parity_relic_reward requires RELIC_ID VALUE"
                        );
                    }
                    applyRelicRewardProbe(parts[1], Integer.parseInt(parts[2]));
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_COUNTER_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException(
                            "parity_relic_counter requires RELIC_ID VALUE"
                        );
                    }
                    applyRelicCounterProbe(parts[1], Integer.parseInt(parts[2]));
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_UNEQUIP_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                            "parity_relic_unequip requires RELIC_ID"
                        );
                    }
                    applyRelicUnequipProbe(parts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_SPAWN_PROBE_COMMAND + " ")) {
                    String[] parts = command.trim().split("\\s+");
                    if (parts.length != 5) {
                        throw new IllegalArgumentException(
                            "parity_relic_spawn requires RELIC_ID FLOOR SHOP PRESET"
                        );
                    }
                    applyRelicSpawnProbe(
                        parts[1], Integer.parseInt(parts[2]),
                        Boolean.parseBoolean(parts[3]), parts[4]
                    );
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(EVENT_PROBE_COMMAND + " ")) {
                    String[] eventParts = command.trim().split("\\s+");
                    if (eventParts.length != 2) {
                        throw new IllegalArgumentException("parity_event requires EVENT_ID");
                    }
                    applyEventProbe(eventParts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(ENGINE_PROBE_COMMAND + " ")) {
                    String[] engineParts = command.trim().split("\\s+");
                    if (engineParts.length != 2) {
                        throw new IllegalArgumentException("parity_engine requires ENGINE_ID");
                    }
                    applyEngineProbe(engineParts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(ENCOUNTER_PROBE_COMMAND + " ")) {
                    String[] encounterParts = command.trim().split("\\s+");
                    if (encounterParts.length != 2) {
                        throw new IllegalArgumentException("parity_encounter requires ENCOUNTER_ID");
                    }
                    applyEncounterProbe(encounterParts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(RELIC_PROBE_COMMAND + " ")) {
                    String[] relicParts = command.trim().split("\\s+");
                    if (relicParts.length != 2) {
                        throw new IllegalArgumentException("parity_relic requires RELIC_ID");
                    }
                    applyRelicProbe(relicParts[1]);
                    return SpireReturn.Return(Boolean.TRUE);
                }
                if (normalized.startsWith(POTION_PROBE_COMMAND + " ")) {
                    String[] potionParts = command.trim().split("\\s+");
                    if (potionParts.length != 3) {
                        throw new IllegalArgumentException("parity_potion requires POTION_ID and SACRED_BARK");
                    }
                    applyPotionProbe(potionParts[1], Boolean.parseBoolean(potionParts[2]));
                    return SpireReturn.Return(Boolean.TRUE);
                }
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

    @SpirePatch(clz = SmokeBomb.class, method = "use")
    public static class AttestSmokeBombEffect {
        @SpirePostfixPatch
        public static void Postfix(SmokeBomb __instance, AbstractCreature target) {
            if (activeScenario != null && String.valueOf(activeScenario.get("scenario_id"))
                    .toUpperCase(Locale.ROOT).startsWith("POTION_PROBE:SMOKE_BOMB:")) {
                activeScenario.put("effect_smoked", "true");
            }
        }
    }
}
