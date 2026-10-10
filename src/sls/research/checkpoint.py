"""Strict research envelope around the unchanged v5/v6 training payload."""
from __future__ import annotations

import os
import tempfile
from pathlib import Path

import torch

from sls.research.protocol import PROTOCOL, digest
from sls.rl.checkpoint import load_checkpoint, save_checkpoint

SCHEMA = "sls-act2-research-checkpoint-v1"


def save(path, trainer, experiment):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=path.parent) as directory:
        inner = Path(directory) / "base.pt"
        trainer._research_checkpoint_envelope = True
        try:
            save_checkpoint(inner, trainer)
        finally:
            trainer._research_checkpoint_envelope = False
        payload = {"schema": SCHEMA, "protocol_sha256": digest(PROTOCOL),
                   "experiment": experiment, "base": torch.load(inner, map_location="cpu", weights_only=False),
                   "sampler": trainer.episode_initializer.state_dict(),
                   "diagnostics": trainer.research_hooks.state_dict()}
        temporary = Path(directory) / "outer.pt"
        torch.save(payload, temporary)
        with temporary.open("r+b") as stream:
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    return path


def load(path, trainer, experiment):
    payload = torch.load(path, map_location="cpu", weights_only=False)
    if payload.get("schema") != SCHEMA or payload.get("protocol_sha256") != digest(PROTOCOL) or payload.get("experiment") != experiment:
        raise ValueError("research checkpoint experiment identity mismatch")
    sampler = payload["sampler"]
    if sampler["bank_sha256"] != trainer.episode_initializer.bank.identity or sampler["probability"] != trainer.episode_initializer.probability:
        raise ValueError("research checkpoint bank/sampler identity mismatch")
    with tempfile.TemporaryDirectory() as directory:
        inner = Path(directory) / "base.pt"
        torch.save(payload["base"], inner)
        trainer._research_checkpoint_envelope = True
        try:
            load_checkpoint(inner, trainer)
        finally:
            trainer._research_checkpoint_envelope = False
    trainer.episode_initializer.load_state_dict(sampler)
    trainer.research_hooks.load_state_dict(payload["diagnostics"])
    return payload
