"""Original-game validation transport."""

from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.backends.original.transport import StdioTransport, Transport

__all__ = ["OriginalBackend", "OriginalSession", "StdioTransport", "Transport"]
