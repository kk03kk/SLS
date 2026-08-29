"""Original-game versus simulator differential validation."""

from sls.validation.compare import canonical_original, canonical_simulator, parity_differences
from sls.validation.coverage import CoverageSummary, summarize
from sls.validation.runner import run_paired
from sls.validation.trace import ParityTrace, TraceStep
from sls.validation.truth import TruthBundleRecorder, evidence_at_least, load_bundle
from sls.validation.transfer import (
    POLICY_TRANSFER_SCHEMA, BackendPolicySummary, DistributionComparison,
    PolicyTransferReport, compare_distributions, contract_differences,
)

__all__ = [
    "CoverageSummary",
    "BackendPolicySummary",
    "DistributionComparison",
    "POLICY_TRANSFER_SCHEMA",
    "PolicyTransferReport",
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
    "compare_distributions",
    "contract_differences",
]
