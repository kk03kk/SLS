# Ending death boundaries: controlled evidence, not whole-run qualification

## Latest update: lethal ordering and terminal RNG corrections completed

Current native source:
`4a9b8854560d61ff490785a7e2e1c1533b3b08162c34c1cbacb503fee877c5f1`.
The earlier sections below retain the first two batches' identities and status.

- Seeds131200126–128: lethal Anger at playerHP1/HeartHP1 still loses to Beat of
  Death in both original and native. Terminal resources and RNG match.
- Seeds131200129–131: lethal Fire Potion wins, HP1 becomesHP7 via Burning Blood.
  HP/outcome/potions/RNG now match after two independently recorded corrections.
- All captures completed and recovered;63 protected entries per journal were
  independently rehashed with zero mismatches. No natural win-rate claim.

Isolated native Act4 restoration lost room context and used an ordinary-combat
reward callback. Restoring the room alone did not remove the RNG failure; the
correct Heart callback was also required. The before/intermediate failures remain.
This API correction1d21→0dcbb898 is distinct from the actual terminal RNG rule.

Original AbstractRoom.update offsets587–661 still construct Boss gold with
100+miscRng.random(-5,5), even when Heart completion has no reward page. Native
GameContext.afterBattle(act4 BOSS) now preserves this one draw, without granting
gold or rolling cards/potions. Source0dcbb898→4a9b8854 changes terminal RNG
semantics and is not globally state-preserving. Act1+Act2 branches are untouched.
No model/checkpoint layout, observation/action schema, reward or PPO change;
no automatic source waiver. See local heart-terminal-semantics-migration-r1.json.

Compact stock expectations: heart-lethal-stock.json; regression before RNG fix
3fail/3pass, after fix6pass. These tests verify the terminal resource subset,
not all final-win cleanup fields or the full dungeon route.

Current full Python suite1288pass/1 intended skip/4 warnings; focused57pass;
all33 configurations, Ruff and diff checks pass.285 fixed normal native
snapshots/actions are identical before/API/after. Adjacent12 ending-interaction
and6 Heart-loss captures replay unchanged. Oracle r15/1.3.12 production isolation
smoke completed; final recovery rehash is recorded in the local continuation.

Next: remaining death/revival/phase-transition branches and ordered Boss/key/Act4
flow. Natural Neow-to-Heart and all relic-modified terminal branches remain open.

This local continuation resumes the human's full-run audit. No commit/push,
server checkout change, PPO/reward/network or training distribution change.

## Completed batches

| Obligation | Seeds | Observed result | Limit |
| --- | --- | --- | --- |
| Kill Spire Shield, keep Spear alive, two subsequent turns | 131200114–116 | First boundary differs only in dead Shield BackAttack1; subsequent boundaries strictly match, including actions and RNG | Declared trace-scoped corpse display residue, not a strict pass or all death branches |
| Kill Spire Spear, keep Shield alive, two subsequent turns | 131200117–119 | All compared boundaries strictly match | Controlled one-survivor death only |
| Anger against living Heart with player HP1 or HP2, fatal Beat of Death | 131200120–125 | Six strict matches, including terminal loss, raw card zones/powers, zero HP, no legal actions and RNG | No final victory or natural-start qualification |

Stock SpireShield/SpireSpear.die queues removal of Surrounded and surviving
BackAttack and turns toward the living enemy. The measured survivor effects
match. The dead Shield residue remains in the raw strict report; no generic
dead-power normalizer or production rule change was made.

## Tool changes and identity

Oracle adds explicit stock-ID mappings for controlled initial HP of Shield,
Spear and Heart. Only initial conditions are set; original damage, death and
victory callbacks still execute. Native production source remains
`1d21b21a02c6c4fa44ff8dba4ae092b1c864b9987c7dd1935b4625caede87bb3`.

The audit comparator accepts an explicit zero-HP combat-loss boundary and rejects
terminal legal actions. Isolated native battle outcome is translated to GAME_OVER
for the audit's public adapter only; the original payload/direct evidence stays
intact. Victory remains a separate unsupported flow obligation, rather than being
silently declared equivalent to an ordinary battle boundary.

Frozen manifests: fullrun-ending-deaths-r1.json and fullrun-heart-loss-r1.json.
Sealed Oracle r13/1.3.10 SHA
`b4833c1b1b67a023baa4f0b49c74859d9925df4c4c752f016c16a23067984aa7`;
Oracle r14/1.3.11 SHA
`99597568625a16b4c5b17492503ecf0f77d612fa3bdb8df912843e0848d5a4d5`.
Each capture completed and restored all63 protected entries with independent
zero-mismatch hash checks. Raw captures/replays remain under local/audits.

Compact original-derived regressions: ending-death-stock.json (six cases,
explicit retained residue) and heart-loss-stock.json (six strict cases).
Focused audit/build tests34 pass; one-survivor regression6 pass. Ruff and diff
checks pass. Complete Python suite is running; do not claim its result yet.

## In progress and remaining

Heart lethal Anger versus lethal Fire Potion at playerHP1 / HeartHP1,
seeds131200126–131, has been frozen from independently reviewed original methods.
Oracle r15/1.3.12 SHA
`28e46d47bc21721e54a4e6eb4ba5e55799cbe4cd53a296377d99cfe2ce1dad9f`.
Original capture is running; these scenarios are not yet qualified. Subsequent
final-win flow and double-Boss/keys/Act4 route remain separate obligations.

No final holdout used. Late-only missing evidence does not automatically block
Act1+Act2 training; future shared-rule changes require a new compatibility review.
