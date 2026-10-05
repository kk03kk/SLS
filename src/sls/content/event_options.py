"""Project displayed event details; never sample or expose unrevealed outcomes."""

from dataclasses import replace

from sls.content.card_features import public_card_option_properties
from sls.content.normalize import normalize_card_id
from sls.contracts import PublicEntity

EVENT_NUMERIC_FIELDS = (
    "hp_loss", "gold_loss", "gold_gain", "displayed_chance",
    "hint_sentries", "hint_nob", "hint_lagavulin",
)
EVENTS_WITH_PUBLIC_DETAILS = frozenset({
    "FALLING", "WE_MEET_AGAIN", "NLOTH", "NOTE_FOR_YOURSELF",
    "WORLD_OF_GOOP", "DEAD_ADVENTURER", "SCRAP_OOZE", "KNOWING_SKULL",
})


def require_event_details(event_id, state):
    if event_id in EVENTS_WITH_PUBLIC_DETAILS and not isinstance(state.get("event_option_details"), dict):
        raise ValueError("public event details missing; rebuild simulator / update observation oracle")


def project_event_options(options, actions, commands, details):
    """Share projection and candidate remapping between both backends.

    Keys are canonical option indices. Subjects refer to already visible owned
    objects; an offered card becomes a preview entity with its public properties.
    """
    if details is None:
        return actions, commands
    rows = {str(key): value for key, value in details.items()}
    subjects = {}
    event_entities = []
    previews = list(options["choice"])
    for entity in options["event"]:
        index = entity.instance_id.removeprefix("event-option:")
        row = rows.get(index, {})
        properties = dict(entity.properties)
        for name, value in row.get("properties", {}).items():
            if name not in EVENT_NUMERIC_FIELDS:
                raise ValueError(f"unknown public event property: {name}")
            properties[name] = value
        event_entities.append(replace(entity, properties=tuple(sorted(properties.items()))))
        subject = row.get("subject_id")
        if "card" in row:
            card = row["card"]
            subject = f"event-preview:{index}"
            content_id = normalize_card_id(card["content_id"])
            previews.append(PublicEntity(subject, content_id,
                public_card_option_properties(content_id, card)))
        if subject is not None:
            subjects[entity.instance_id] = str(subject)
    options["event"] = tuple(event_entities)
    options["choice"] = tuple(previews)
    enriched, remapped = [], {}
    for action in actions:
        updated = replace(action, subject_id=subjects[action.option_id]) if action.option_id in subjects else action
        enriched.append(updated)
        remapped[updated.candidate_id] = commands[action.candidate_id]
    return tuple(enriched), remapped
