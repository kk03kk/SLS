"""Public-action interpretation of a player escape, separate from victory."""
from sls.contracts import ActionKind, ScreenType


def player_escaped(decision, action, transition):
    # Smoke Bomb is the supported player escape action. The public potion
    # identity and actual transition must both agree; discarding it is not an
    # escape, nor is an attempted use that leaves the battle running.
    return (action.kind is ActionKind.USE_POTION
            and transition.decision.observation.screen is not ScreenType.COMBAT
            and any(p.instance_id == action.subject_id and p.content_id == 'SMOKE_BOMB'
                    for p in decision.observation.potions))
