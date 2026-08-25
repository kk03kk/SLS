package spirecomm.parity;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.random.Random;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ParityRng {
    public static Random mathRng;
    public static long mathSeed;
    private static RngSnapshot[] relicProbeBaseline;

    private static final class RngSnapshot {
        private final long seed0;
        private final long seed1;
        private final int counter;

        private RngSnapshot(Random rng) {
            this.seed0 = rng.random.getState(0);
            this.seed1 = rng.random.getState(1);
            this.counter = rng.counter;
        }

        private void restore(Random rng) {
            rng.random.setState(this.seed0, this.seed1);
            rng.counter = this.counter;
        }
    }

    private ParityRng() {}

    public static void reset() {
        String configuredSeed = System.getProperty("spirecomm.math_seed");
        mathSeed = configuredSeed == null || configuredSeed.isEmpty()
            ? Settings.seed.longValue() - 897897L
            : Long.parseUnsignedLong(configuredSeed);
        int counter = Integer.parseInt(System.getProperty("spirecomm.math_counter", "0"));
        if (counter < 0) {
            throw new IllegalArgumentException("spirecomm.math_counter must be non-negative");
        }
        mathRng = new Random(Long.valueOf(mathSeed), counter);
    }

    public static Random requireMathRng() {
        if (mathRng == null) reset();
        return mathRng;
    }

    /** Restore every relic probe to the first live-combat RNG boundary. */
    public static void resetRelicProbeStreams() {
        Random[] streams = new Random[] {
            AbstractDungeon.aiRng,
            AbstractDungeon.cardRandomRng,
            AbstractDungeon.cardRng,
            AbstractDungeon.eventRng,
            requireMathRng(),
            AbstractDungeon.merchantRng,
            AbstractDungeon.miscRng,
            AbstractDungeon.monsterHpRng,
            AbstractDungeon.monsterRng,
            NeowEvent.rng,
            AbstractDungeon.potionRng,
            AbstractDungeon.relicRng,
            AbstractDungeon.shuffleRng,
            AbstractDungeon.treasureRng
        };
        if (relicProbeBaseline == null) {
            relicProbeBaseline = new RngSnapshot[streams.length];
            for (int index = 0; index < streams.length; ++index) {
                relicProbeBaseline[index] = new RngSnapshot(streams[index]);
            }
            return;
        }
        for (int index = 0; index < streams.length; ++index) {
            relicProbeBaseline[index].restore(streams[index]);
        }
    }

    public static BigInteger unsigned(long value) {
        return new BigInteger(Long.toUnsignedString(value));
    }

    public static Map<String, Object> state(Random rng) {
        if (rng == null) return null;
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("counter", Integer.valueOf(rng.counter));
        result.put("seed0", unsigned(rng.random.getState(0)));
        result.put("seed1", unsigned(rng.random.getState(1)));
        return result;
    }
}
