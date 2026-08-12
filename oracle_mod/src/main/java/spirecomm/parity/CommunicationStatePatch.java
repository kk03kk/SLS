package spirecomm.parity;

import com.autoplay.gson.Gson;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowEvent;
import communicationmod.GameStateConverter;
import java.util.LinkedHashMap;
import java.util.Map;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public final class CommunicationStatePatch {
    public static String inject(String json) {
        if (json == null || json.length() < 2 || json.charAt(json.length() - 1) != '}') {
            return json;
        }
        Map<String, Object> rng = new LinkedHashMap<String, Object>();
        rng.put("ai", ParityRng.state(AbstractDungeon.aiRng));
        rng.put("card_random", ParityRng.state(AbstractDungeon.cardRandomRng));
        rng.put("card", ParityRng.state(AbstractDungeon.cardRng));
        rng.put("event", ParityRng.state(AbstractDungeon.eventRng));
        rng.put("math_util", ParityRng.state(ParityRng.requireMathRng()));
        rng.put("merchant", ParityRng.state(AbstractDungeon.merchantRng));
        rng.put("misc", ParityRng.state(AbstractDungeon.miscRng));
        rng.put("monster_hp", ParityRng.state(AbstractDungeon.monsterHpRng));
        rng.put("monster", ParityRng.state(AbstractDungeon.monsterRng));
        rng.put("neow", ParityRng.state(NeowEvent.rng));
        rng.put("potion", ParityRng.state(AbstractDungeon.potionRng));
        rng.put("relic", ParityRng.state(AbstractDungeon.relicRng));
        rng.put("shuffle", ParityRng.state(AbstractDungeon.shuffleRng));
        rng.put("treasure", ParityRng.state(AbstractDungeon.treasureRng));
        Gson gson = new Gson();
        return json.substring(0, json.length() - 1)
            + ",\"_rng\":" + gson.toJson(rng)
            + ",\"math_seed\":" + Long.toUnsignedString(ParityRng.mathSeed)
            + "}";
    }

    @SpirePatch(clz = GameStateConverter.class, method = "getCommunicationState")
    public static class AddRngState {
        @SpireRawPatch
        public static void Raw(CtBehavior method) throws CannotCompileException {
            method.insertAfter("$_ = spirecomm.parity.CommunicationStatePatch.inject($_);");
        }
    }
}
