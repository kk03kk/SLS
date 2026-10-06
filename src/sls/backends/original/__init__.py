"""Original-game validation transport."""

from sls.backends.original.environment import (
    ORIGINAL_EXECUTION_CONTRACT,
    OriginalBackend,
)
from sls.backends.original.live import LiveGameBackend
from sls.backends.original.session import OriginalSession
from sls.backends.original.transport import StdioTransport, Transport

__all__ = [
    "LiveGameBackend", "OriginalBackend", "OriginalSession", "StdioTransport", "Transport",
    "ORIGINAL_EXECUTION_CONTRACT",
]
