package spirecomm.parity;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.random.Random;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ParityRng {
    public static Random mathRng;
    public static long mathSeed;

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
