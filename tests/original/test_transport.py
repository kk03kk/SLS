from __future__ import annotations

import threading

import pytest

from sls.backends.original.transport import StdioTransport


class BlockingInput:
    def __init__(self) -> None:
        self.release = threading.Event()

    def readline(self) -> str:
        self.release.wait()
        return ""


def test_stdio_transport_times_out_when_original_stops_replying() -> None:
    source = BlockingInput()
    transport = StdioTransport(stdin=source, read_timeout_seconds=0.01)
    with pytest.raises(TimeoutError, match="command boundary"):
        transport.receive()
    source.release.set()
