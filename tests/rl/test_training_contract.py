from __future__ import annotations

from types import SimpleNamespace

from sls.rl.training_contract import runtime_contract


class _UnavailableCuda:
    @staticmethod
    def is_available() -> bool:
        return False

    @staticmethod
    def device_count() -> int:
        raise AssertionError("CPU-only provenance must not initialize CUDA")

    @staticmethod
    def get_device_name(_: int) -> str:
        raise AssertionError("CPU-only provenance must not initialize CUDA")


class _UnavailableCudnn:
    @staticmethod
    def version() -> int:
        raise AssertionError("CPU-only provenance must not initialize cuDNN")


def test_cpu_runtime_contract_does_not_initialize_cuda_libraries() -> None:
    torch = SimpleNamespace(
        __version__="test",
        version=SimpleNamespace(cuda="13.0"),
        cuda=_UnavailableCuda(),
        backends=SimpleNamespace(cudnn=_UnavailableCudnn()),
        are_deterministic_algorithms_enabled=lambda: True,
    )

    contract = runtime_contract(torch)

    assert contract["cuda"] == "13.0"
    assert contract["cudnn"] is None
    assert contract["cuda_device_count"] == 0
    assert contract["cuda_device"] is None
    assert contract["deterministic_algorithms"] is True
