from __future__ import annotations

import io
import json
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


def test_stdio_transport_decodes_localized_game_json_as_utf8() -> None:
    payload = {"game_state": {"choice_list": ["离开"]}}
    source = io.TextIOWrapper(
        io.BytesIO((json.dumps(payload, ensure_ascii=False) + "\n").encode("utf-8")),
        encoding="gbk",
    )

    transport = StdioTransport(stdin=source)

    assert transport.receive() == payload
    assert source.encoding == "utf-8"
