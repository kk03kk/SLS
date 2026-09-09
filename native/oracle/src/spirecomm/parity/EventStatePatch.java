package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.GremlinMatchGame;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.neow.NeowReward;
import communicationmod.GameStateConverter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import com.megacrit.cardcrawl.cards.AbstractCard;

/** The attempt counter is rendered publicly by GremlinMatchGame.render. */
public class EventStatePatch {
    private static Object field(Object event, String name) throws ReflectiveOperationException {
        Field f = event.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(event);
    }

    private static void subject(HashMap<String, Object> details, int option,
                                String zone, List<?> objects, Object target) {
        if (target == null) return;
        for (int i = 0; i < objects.size(); ++i) {
            if (objects.get(i) == target) {
                HashMap<String, Object> row = new HashMap<>();
                row.put("subject_id", zone + ":" + i);
                details.put("" + option, row);
                return;
            }
        }
        throw new IllegalStateException("Displayed event target absent from " + zone);
    }

    private static void properties(HashMap<String, Object> details, int option, Object... pairs) {
        HashMap<String, Object> p = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) p.put((String)pairs[i], pairs[i + 1]);
        HashMap<String, Object> row = new HashMap<>();
        row.put("properties", p);
        details.put("" + option, row);
    }

    private static HashMap<String, Object> eventDetails(Object event) throws ReflectiveOperationException {
        HashMap<String, Object> details = new HashMap<>();
        String name = event.getClass().getSimpleName();
        // Phase checks prevent initial hidden selections and stale result data
        // from entering the policy. Only fields already rendered in the UI.
        if (name.equals("Falling") && field(event, "screen").toString().equals("CHOICE")) {
            subject(details, 0, "DECK", AbstractDungeon.player.masterDeck.group, field(event, "skillCard"));
            subject(details, 1, "DECK", AbstractDungeon.player.masterDeck.group, field(event, "powerCard"));
            subject(details, 2, "DECK", AbstractDungeon.player.masterDeck.group, field(event, "attackCard"));
        } else if (name.equals("WeMeetAgain") && field(event, "screen").toString().equals("INTRO")) {
            subject(details, 0, "POTION", AbstractDungeon.player.potions, field(event, "potionOption"));
            subject(details, 2, "DECK", AbstractDungeon.player.masterDeck.group, field(event, "cardOption"));
            int gold = (Integer)field(event, "goldAmount");
            if (gold >= 0) properties(details, 1, "gold_loss", gold);
        } else if (name.equals("Nloth") && (Integer)field(event, "screenNum") == 0) {
            subject(details, 0, "RELIC", AbstractDungeon.player.relics, field(event, "choice1"));
            subject(details, 1, "RELIC", AbstractDungeon.player.relics, field(event, "choice2"));
        } else if (name.equals("GoopPuddle") && field(event, "screen").toString().equals("INTRO")) {
            properties(details, 0, "hp_loss", field(event, "damage"), "gold_gain", field(event, "gold"));
            properties(details, 1, "gold_loss", field(event, "goldLoss"));
        } else if (name.equals("ScrapOoze") && (Integer)field(event, "screenNum") == 0) {
            properties(details, 0, "hp_loss", field(event, "dmg"), "displayed_chance", field(event, "relicObtainChance"));
        } else if (name.equals("DeadAdventurer") && field(event, "screen").toString().equals("INTRO")) {
            int enemy = (Integer)field(event, "enemy");
            properties(details, 0, "displayed_chance", field(event, "encounterChance"),
                "hint_sentries", enemy == 0, "hint_nob", enemy == 1, "hint_lagavulin", enemy == 2);
        } else if (name.equals("KnowingSkull") && field(event, "screen").toString().equals("ASK")) {
            properties(details, 0, "hp_loss", field(event, "goldCost"), "gold_gain", 90);
            properties(details, 1, "hp_loss", field(event, "cardCost"));
            properties(details, 2, "hp_loss", field(event, "potionCost"));
            properties(details, 3, "hp_loss", field(event, "leaveCost"));
        } else if (name.equals("NoteForYourself") && field(event, "screen").toString().equals("CHOOSE")) {
            AbstractCard card = (AbstractCard)field(event, "obtainCard");
            HashMap<String, Object> preview = new HashMap<>();
            preview.put("content_id", card.cardID);
            preview.put("upgrades", card.timesUpgraded);
            preview.put("base_cost", card.cost);
            preview.put("current_cost", card.costForTurn);
            preview.put("base_damage", card.baseDamage);
            // Use the same public fields as every other card projection,
            // including explicit false flags (presence is part of model input).
            CardStatePatch.AddDynamicFields.Postfix(preview, card);
            HashMap<String, Object> row = new HashMap<>();
            row.put("card", preview);
            details.put("0", row);
        }
        return details;
    }

    @SpirePatch(clz = GameStateConverter.class, method = "getEventState")
    public static class AddMatchAttempts {
        @SpirePostfixPatch
        public static HashMap<String, Object> Postfix(HashMap<String, Object> result) {
            try {
                result.put("event_option_details", eventDetails(AbstractDungeon.getCurrRoom().event));
                Object event = AbstractDungeon.getCurrRoom().event;
                if (event.getClass().getSimpleName().equals("Falling")) {
                    // The older generic continuation reader can find inherited
                    // screenNum=0 before the event's actual private screen.
                    result.put("event_choice_phase", field(event, "screen").toString());
                }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Cannot project displayed event choices", error);
            }
            if (AbstractDungeon.getCurrRoom().event instanceof GremlinMatchGame) {
                try {
                    Field counter = GremlinMatchGame.class.getDeclaredField("attemptCount");
                    counter.setAccessible(true);
                    result.put("attempts_remaining", counter.getInt(AbstractDungeon.getCurrRoom().event));
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Cannot project public Match and Keep attempts", error);
                }
            }
            if (AbstractDungeon.getCurrRoom().event instanceof NeowEvent) {
                try {
                    Field rewards = NeowEvent.class.getDeclaredField("rewards");
                    rewards.setAccessible(true);
                    List<?> visible = (List<?>) rewards.get(AbstractDungeon.getCurrRoom().event);
                    ArrayList<HashMap<String, Object>> offers = new ArrayList<>();
                    for (Object value : visible) {
                        NeowReward reward = (NeowReward) value;
                        HashMap<String, Object> offer = new HashMap<>();
                        // Only label semantics. Never inspect generated cards/relics or RNG.
                        offer.put("bonus", reward.type.name());
                        offer.put("drawback", reward.drawback.name());
                        offers.add(offer);
                    }
                    if (offers.size() == 2 || offers.size() == 4) {
                        result.put("neow_options", offers);
                    }
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Cannot project public Neow offers", error);
                }
            }
            return result;
        }
    }
}
