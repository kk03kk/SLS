/*
 * Read-only RNG audit probe for the pinned original-game JAR.
 *
 * Run with the Java 8 Nashorn bundled with Slay the Spire:
 *   jjs -cp desktop-1.0.jar scripts/rng_oracle_probe.js
 *
 * This executes the original compiled Random classes directly. It neither
 * modifies the game nor stores decompiled game source in this repository.
 */

var GameRandom = Java.type("com.megacrit.cardcrawl.random.Random");
var Long = Java.type("java.lang.Long");
var Float = Java.type("java.lang.Float");

var seeds = [
    "0",
    "1",
    "-1",
    "123456789",
    "-8210630576094413445"
];

function signedToUnsignedString(value) {
    return Long.toUnsignedString(value);
}

function probe(seedText) {
    var rng = new GameRandom(Long.valueOf(seedText));
    var result = {
        seed_bits: signedToUnsignedString(Long.valueOf(seedText)),
        initial: {
            counter: rng.counter,
            seed0: signedToUnsignedString(rng.random.getState(0)),
            seed1: signedToUnsignedString(rng.random.getState(1))
        },
        values: {}
    };

    result.values.range_999 = rng["random(int)"](999);
    result.values.between_5_12 = rng["random(int,int)"](5, 12);
    result.values.long_range = String(
        rng["random(long)"](Long.valueOf("1000000000000"))
    );
    result.values.random_long = String(rng.randomLong());
    result.values.boolean = Boolean(rng.randomBoolean());
    result.values.chance_0_375 = Boolean(
        rng["randomBoolean(float)"](Float.valueOf("0.375"))
    );
    result.values.unit_float = String(rng["random()"]());
    result.values.float_range = String(
        rng["random(float)"](Float.valueOf("5.0"))
    );
    result.values.float_between = String(
        rng["random(float,float)"](Float.valueOf("-2.0"), Float.valueOf("3.0"))
    );
    result.final = {
        counter: rng.counter,
        seed0: signedToUnsignedString(rng.random.getState(0)),
        seed1: signedToUnsignedString(rng.random.getState(1))
    };
    return result;
}

for each (var seed in seeds) {
    print(JSON.stringify(probe(seed)));
}
