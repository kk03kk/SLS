// Read-only oracle for the JDK shuffle used by Slay the Spire card/relic pools.
// Run with the pinned game's jjs.exe; no game classes or assets are modified.
var Arrays = Java.type("java.util.Arrays");
var ArrayList = Java.type("java.util.ArrayList");
var Collections = Java.type("java.util.Collections");
var Random = Java.type("java.util.Random");
var Long = Java.type("java.lang.Long");

var seeds = ["0", "1", "-1", "123456789", "-8210630576094413445"];
for each (var seedText in seeds) {
    var values = new ArrayList(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    Collections.shuffle(values, new Random(Long.valueOf(seedText)));
    var output = [];
    for (var i = 0; i < values.size(); ++i) output.push(Number(values.get(i)));
    print(JSON.stringify({seed_bits: Long.toUnsignedString(Long.valueOf(seedText)), values: output}));
}
