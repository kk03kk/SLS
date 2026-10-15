"""Publish immutable local rule evidence and the explicit weights-only bridge."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path

from sls.rl.normalstart_release import RELEASE, validate_qualification
from sls.rl.training_contract import (
    NATIVE_SOURCE_PATHS,
    ROOT,
    native_source_digest,
    sha256_file,
)


def git_native_files():
    names = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", "HEAD", "--", *NATIVE_SOURCE_PATHS],
                                    cwd=ROOT, text=True).splitlines()
    process = subprocess.Popen(["git", "cat-file", "--batch"], cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    raw, _ = process.communicate("".join("HEAD:" + name + "\n" for name in names).encode())
    offset, files = 0, {}
    for name in names:
        end = raw.index(b"\n", offset)
        size = int(raw[offset:end].split()[-1])
        offset = end + 1
        files[name] = raw[offset:offset + size].replace(b"\r\n", b"\n")
        offset += size + 1
    digest = hashlib.sha256()
    for name, data in sorted(files.items()):
        path = name.encode()
        digest.update(len(path).to_bytes(4, "big"))
        digest.update(path)
        digest.update(len(data).to_bytes(8, "big"))
        digest.update(data)
    return digest.hexdigest(), files


def publish(qualification: Path):
    report = validate_qualification(qualification, sha256_file(qualification))
    for path, digest in report["input_sha256"].items():
        if sha256_file(Path(path)) != digest:
            raise ValueError("local raw qualification evidence changed")
    old = ROOT / "tests/fixtures/evidence/act12-foundation"
    previous = json.loads((old / "qualification.json").read_text(encoding="utf-8"))
    legacy = json.loads((old / "simulator-transition.json").read_text(encoding="utf-8"))
    before, files = git_native_files()
    if before != previous["native_source_sha256"] or before != legacy["target_native_source_sha256"]:
        raise ValueError("Git baseline is not the independently qualified previous rule source")
    changes = []
    for name, data in files.items():
        after = (ROOT / name).read_bytes().replace(b"\r\n", b"\n")
        if data != after:
            changes.append({"path": name, "before_sha256": hashlib.sha256(data).hexdigest(),
                            "after_sha256": hashlib.sha256(after).hexdigest()})
    directory = ROOT / RELEASE
    if directory.exists():
        raise FileExistsError("immutable normal-start release already exists")
    directory.mkdir(parents=True)
    for name, data in (("qualification.json", qualification.read_bytes()),
                       ("legacy-transition.json", (old / "simulator-transition.json").read_bytes()),
                       ("previous-qualification.json", (old / "qualification.json").read_bytes())):
        (directory / name).write_bytes(data)
    (directory / "source-changes.json").write_text(json.dumps({
        "schema": "sls-native-rule-correction-bridge-v1", "before_native_source_sha256": before,
        "after_native_source_sha256": native_source_digest(), "changed_files": changes,
        "current_rule_validation": "qualification.json", "weights_only": True, "exact_resume_allowed": False,
        "review": "Replayed fixed Act1–2 corpus plus changed-rule regression; no old pass inherited",
        "impacted_mechanisms": ["death/revival/action order", "battle/permanent deck restore and large piles",
                                "event constructor RNG timing", "chest key/relic interactions"]}, indent=2) + "\n", encoding="utf-8")
    proof_names = ("qualification.json", "legacy-transition.json", "previous-qualification.json", "source-changes.json")
    transition = {"schema": "sls-curriculum-simulator-transition-v1", "status": "VERIFIED_RULE_CORRECTION",
        "semantics_revision": "sls-normalstart-act12-current-rule-corrections-v1",
        "source_native_source_sha256": legacy["source_native_source_sha256"],
        "target_native_source_sha256": native_source_digest(), "source_profile": "IRONCLAD_A20_ACT1",
        "target_profile": "IRONCLAD_A20_ACT2", "transfer_mode": "weights-only", "exact_resume_allowed": False,
        "evidence": {name: sha256_file(directory / name) for name in proof_names},
        "scope": report["scope"], "scope_is_exhaustive": False}
    (directory / "simulator-transition.json").write_text(json.dumps(transition, indent=2) + "\n", encoding="utf-8")
    return transition


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--qualification", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(publish(args.qualification)))


if __name__ == "__main__":
    main()
