"""Original-game versus simulator differential validation."""

from sls.validation.compare import canonical_original, canonical_simulator, parity_differences
from sls.validation.coverage import CoverageSummary, summarize
from sls.validation.runner import run_paired
from sls.validation.trace import ParityTrace, TraceStep
from sls.validation.truth import TruthBundleRecorder, evidence_at_least, load_bundle

__all__ = [
    "CoverageSummary",
    "ParityTrace",
    "TraceStep",
    "TruthBundleRecorder",
    "evidence_at_least",
    "canonical_original",
    "canonical_simulator",
    "parity_differences",
    "run_paired",
    "load_bundle",
    "summarize",
]
