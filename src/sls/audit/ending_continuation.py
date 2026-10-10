"""Bounded public-action probe of actual Act4 rooms; not a trained policy."""
from dataclasses import asdict

from sls.contracts import ActionKind


def choose_probe_action(decision):
    """Use only the public decision, with a fixed reproducible action ordering."""
    cards = {card.instance_id:card.card_id for card in decision.observation.hand}
    plays = [a for a in decision.actions if a.kind == ActionKind.PLAY_CARD
             and cards.get(a.subject_id) == 'SEARING_BLOW']
    if plays:
        return plays[0]
    priority = (ActionKind.END_TURN, ActionKind.REST, ActionKind.LEAVE_SHOP,
                ActionKind.SKIP_CARD_REWARD, ActionKind.TAKE_REWARD, ActionKind.SKIP_REWARD,
                ActionKind.CHOOSE_MAP_NODE, ActionKind.PROCEED, ActionKind.CONFIRM)
    for kind in priority:
        selected = next((a for a in decision.actions if a.kind == kind), None)
        if selected is not None:
            return selected
    raise ValueError('public decision has no reviewed probe action')


def collect_ending_continuation(session, record, flush, *, max_decisions=128):
    from sls.backends.original.environment import OriginalBackend
    from sls.curriculum import IRONCLAD_A20_HEART
    if type(max_decisions) is not int or not 1 <= max_decisions <= 256:
        raise ValueError('invalid diagnostic decision budget')
    backend = OriginalBackend(session=session, profile=IRONCLAD_A20_HEART)
    backend._adapted = backend._adapt(session.payload)
    initial = backend._adapted.decision
    if initial.observation.run.act != 4:
        raise ValueError('continuous probe requires witnessed actual Act4 entry')
    record.update(status='IN_PROGRESS', max_decisions=max_decisions,
                  initial_raw=session.payload, history=[], training_eligible=False, natural_trajectory=False)
    flush()
    for _ in range(max_decisions):
        decision = backend._adapted.decision
        if decision.terminal:
            raise ValueError('terminal before a recorded transition')
        action = choose_probe_action(decision)
        step = dict(observation=decision.observation.to_dict(), legal_actions=[asdict(a) for a in decision.actions],
                    selected_action=asdict(action), before_raw=backend.raw_payload)
        record['history'].append(step)
        flush()
        transition = backend.step(action)
        step.update(commands=backend.last_executed_commands, validation_evidence=backend.last_validation_evidence,
                    after_raw=backend.raw_payload, next_observation=transition.decision.observation.to_dict(),
                    terminated=transition.terminated, truncated=transition.truncated,
                    reward=transition.reward, info=transition.info)
        flush()
        if transition.terminated or transition.truncated:
            record.update(status='TERMINATED' if transition.terminated else 'BACKEND_TRUNCATED',
                          terminal_info=transition.info)
            flush()
            return
    record.update(status='DIAGNOSTIC_LIMIT_UNFINISHED', game_failure=False)
    flush()
