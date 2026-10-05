# Public observation field audit (working tree)

This is an implementation inventory and a queue of remaining checks, not a
parity certificate. Sources inspected: `contracts/observation.py`, both backend
adapters, native `python/module.cpp`, model `encoding.py` and `batching.py`.
Stock evidence comes from the local JAR and decompiled classes, not project docs.

| Public information | Native / stock source | Contract and model path | Current evidence / remaining gap |
| --- | --- | --- | --- |
| Character, HP, maximum HP, block, energy | public combat player / stock player | Player -> numeric and character token | Explicit fields retained; energy tests exist. Non-combat max energy is a constant 3 placeholder. |
| Act, floor, ascension, gold, keys, visible boss | public_run and player_state / game and parity_run | RunContext -> run row | Explicit fields retained; key filtering and horizon tests. |
| Turn | public_combat.turn / combat.turn | public_context -> run row | Retained. Other public screen context is currently absent. |
| Card identity, upgrade count, base/current costs, playable | public_inventory/public_combat / stock card JSON | Card -> card row | Upgrade and dynamic-cost regressions; non-hand stock cost normalization remains unresolved. |
| Rampage and Ritual Dagger mutable damage | native specialData converted to base_damage / Oracle baseDamage | Card or offer properties -> numeric fields | Adapter and model distinction tests. Rampage misc is deliberately not treated as damage. |
| Free once, retained, self-retain | native instance / stock AbstractCard | Card and offer properties -> numeric fields | Projected through hand, piles, deck, choices, reward and shop. Oracle patch compiled without game launch. |
| Draw order | native draw pile / stock draw pile | visible_order only with Frozen Eye | Hidden pile sorting and permutation tests; no unique ID as an input feature. |
| Enemy HP, block, intent, adjusted damage, hits, gone | public_combat.monsters / stock monster+intent patch | Enemy -> numeric/category fields | Projection explicit; intent masks and terminal/split tests. Exact coverage of all powers affecting intents still requires rule audit. |
| Power amount and owner | public player/monster powers / stock player/monster powers | PublicEntity owner_id -> directed relation | Owner validation, tensor distinction, target-score and permutation tests. |
| Relic ID and counter | persistent or live combat relic state / stock relic counter | PublicEntity -> counter numeric field | Neutral -1 maps to 0; meaningful -2 retained. Lizard Tail availability repaired; Maw Bank, Neow's Lament, Matryoshka and N'loth's face project stock sentinels. |
| Relic activated state | relic-specific gameplay state / public icon glow/used state | Counter where represented | Ancient Tea Set now retains a charge across noncombat rooms and projects -2 until combat; complete room-path and checkpoint tests pass. Other flags still require relic-by-relic review. |
| Bottle-card association | deck bottle indices / public bottled card | Three card flags in deck, combat, selections and numeric encoding | Flags now survive combat initialization, card movement and JSON restore; four native/adapter/model regressions pass. Combat stat-copy behavior still needs a dedicated scenario. |
| Potion identity and slot | public_inventory.potions / game.potions | PublicEntity -> slot | Retained; empty slots excluded. Capacity is implicit through ascension/relics rather than an explicit field. |
| Map room, coordinates, reachable, directed links, burning elite | public_map / game map + parity_run | MapNode -> numeric/category and edges | Structural tests and reachability filtering; map position is implicit through reachability. |
| Combat card-choice source and mutable card properties | choice options / hand or grid cards | choice_options -> choice rows | DRAW/EXHAUST aliases and visible source recovery repaired; 11 source-specific tests. |
| Choice operation and required count | cardSelectInfo / public selection screen | Only indirectly through actions and recurrent history | Operation and count still need explicit projection. |
| Selected cards/order | Native ordered pending selection / stock selected and selected_cards arrays | Observation.selected_cards -> CHOICE rows with selected and selected_order | Card identity, mutable properties, source and order now reach the model; pending JSON checkpoint tests compare the complete decision. |
| Card reward/selection offers | public_screen cards / reward/grid JSON | reward_options -> rows | Mutable properties now retained; deck_index is numeric, not a deck relation. |
| Shop prices, sold state and card properties | public_screen shop / stock shop JSON | ShopItem -> rows | Card upgrades/cost/damage now retained. Removal price is not represented by a shop entity; audit action metadata separately. |
| Gold, relic, potion, key rewards | public_screen rewards / stock rewards | reward_options -> rows | Identity/amount retained. Existing reward filtering and ordering tests. |
| Event choice labels and changing numerical effects | event info / public dialog options | event ID + option ordinal only | Dynamic costs/rewards/probabilities not generally represented; open. |
| Match and Keep known cards and attempts | match_slots and attempts_remaining / Oracle slots and displayed counter | event_options known/removed, action slot references, public_context attempts_remaining | Each pair references both public slots. Attempts now enter numeric encoding; maintained Oracle patch reads the counter used by stock render. Legacy payloads without the field remain explicitly missing. |
| Rest and boss-relic choices | legal rest/boss choices / stock UI | typed option entities | IDs retained; rest effect amounts are implicit in HP/relics. |
| Private RNG, queues, replay and action bits | backend checkpoint only | Not projected by adapters | Encoder rejects unknown properties. Raw snapshot is deliberately broader than Observation; each new field needs an explicit public justification. |

The open rows are retained as a future backlog. The user subsequently requested
stopping after an overall review and repair of apparent significant problems;
see [closeout](observation-simulator-closeout.md). Passing tests do not establish
complete public-information coverage or complete stock gameplay parity.
