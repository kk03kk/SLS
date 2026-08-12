"""Native and Python backends for high-speed headless STS simulation."""

from spirecomm.simulator.catalog import ACT1_ENCOUNTERS, IRONCLAD_CARDS

try:
    from spirecomm.simulator._lightspeed import LightspeedBattle, lightspeed_commit
except ImportError:  # Native module is an optional build artifact.
    LightspeedBattle = None
    lightspeed_commit = None

__all__ = [
    "ACT1_ENCOUNTERS", "IRONCLAD_CARDS", "LightspeedBattle", "lightspeed_commit"
]
