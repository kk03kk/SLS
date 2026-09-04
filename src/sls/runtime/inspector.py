"""Thread-safe browser inspector for a live recurrent policy."""

from __future__ import annotations

import hashlib
import json
import threading
import time
from collections import deque
from copy import deepcopy
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Callable, Iterable, Mapping

import torch

from sls.contracts import Action, ActionKind, Decision, ScreenType
from sls.runtime.artifact import POLICY_ARTIFACT_SCHEMA, PolicyArtifactMetadata
from sls.runtime.controller import AgentRuntime, PolicyScore, boundary_id


class ControlConflict(RuntimeError):
    pass


def discover_policy_artifacts(
    roots: Iterable[Path], *, extra_paths: Iterable[Path] = (),
) -> tuple[dict[str, Any], ...]:
    """Find exported policy artifacts without treating training checkpoints as models."""

    candidates: set[Path] = {path.resolve() for path in extra_paths}
    for root in roots:
        resolved = root.resolve()
        if resolved.is_file():
            candidates.add(resolved)
            continue
        if resolved.is_dir():
            candidates.update(path.resolve() for path in resolved.glob("**/stages/*/*.pt"))
            candidates.update(path.resolve() for path in resolved.glob("*.pt"))
    models: list[dict[str, Any]] = []
    for path in sorted(candidates):
        if not path.is_file():
            continue
        try:
            payload = torch.load(path, map_location="cpu", weights_only=True)
            if payload.get("schema") != POLICY_ARTIFACT_SCHEMA:
                continue
            raw = payload.get("metadata")
            if not isinstance(raw, Mapping):
                continue
            metadata = PolicyArtifactMetadata(**dict(raw))
            metadata.validate()
        except (OSError, RuntimeError, TypeError, ValueError):
            continue
        identifier = hashlib.sha256(str(path).encode("utf-8")).hexdigest()
        manifest: dict[str, Any] = {}
        manifest_path = path.with_suffix(".json")
        if manifest_path.is_file():
            try:
                parsed = json.loads(manifest_path.read_text(encoding="utf-8"))
                if (
                    isinstance(parsed, dict)
                    and parsed.get("schema") == "sls-test-model-manifest-v1"
                    and parsed.get("artifact_filename") == path.name
                    and parsed.get("model_sha256") == metadata.model_sha256
                ):
                    manifest = parsed
            except (OSError, UnicodeError, json.JSONDecodeError):
                manifest = {}
        models.append({
            "model_id": identifier,
            "name": path.stem,
            "path": str(path),
            "goal": metadata.goal,
            "ascension_min": metadata.ascension_min,
            "ascension_max": metadata.ascension_max,
            "model_sha256": metadata.model_sha256,
            "size_mb": round(path.stat().st_size / (1024 * 1024), 2),
            "profile": manifest.get("profile"),
            "environment_steps": manifest.get("environment_steps"),
            "updates": manifest.get("updates"),
            "source_checkpoint": manifest.get("source_checkpoint"),
            "verified_weight_match": manifest.get("verified_weight_match", False),
        })
    return tuple(models)


class InspectorLauncher:
    """Hold the setup screen until the user explicitly selects a policy."""

    def __init__(
        self,
        models: Iterable[Mapping[str, Any]],
        runtime_factory: Callable[[Path], InteractiveAgentRuntime],
        *,
        preselected_path: Path | None = None,
    ) -> None:
        self._models = tuple(dict(model) for model in models)
        self._models_by_id = {str(model["model_id"]): model for model in self._models}
        self._runtime_factory = runtime_factory
        self._runtime: InteractiveAgentRuntime | None = None
        self._lock = threading.Lock()
        self._status = "SETUP"
        self._error: str | None = None
        self._selected_model_id = next((
            str(model["model_id"]) for model in self._models
            if preselected_path is not None
            and Path(str(model["path"])) == preselected_path.resolve()
        ), str(self._models[0]["model_id"]) if self._models else None)

    def state(self) -> dict[str, Any]:
        with self._lock:
            runtime = self._runtime
            status = self._status
            error = self._error
            selected = self._selected_model_id
        if runtime is not None:
            state = runtime.state()
        else:
            state = {
                "schema": "sls-live-inspector-state-v1",
                "revision": 0,
                "status": status,
                "mode": "PAUSED",
                "delay_seconds": None,
                "card_reward_preview_seconds": None,
                "countdown_remaining": None,
                "boundary_id": None,
                "terminal": False,
                "value": None,
                "recommended_candidate_id": None,
                "actions": [],
                "summary": None,
                "observation": None,
                "error": error or (
                    None if self._models else
                    "No exported policy artifacts were found under the configured model roots."
                ),
            }
        return {
            **state,
            "models": list(self._models),
            "selected_model_id": selected,
            "selected_model": self._models_by_id.get(str(selected)),
        }

    def submit(self, payload: dict[str, Any]) -> dict[str, Any]:
        command = str(payload.get("command") or "")
        with self._lock:
            runtime = self._runtime
            status = self._status
        if runtime is not None:
            return runtime.submit(payload)
        if command != "start":
            raise ControlConflict("select a model and start the test first")
        if status == "LOADING":
            raise ControlConflict("the selected model is already loading")
        model_id = str(payload.get("model_id") or "")
        model = self._models_by_id.get(model_id)
        if model is None:
            raise ControlConflict("selected model is unavailable")
        with self._lock:
            if self._status == "LOADING":
                raise ControlConflict("the selected model is already loading")
            self._status = "LOADING"
            self._error = None
            self._selected_model_id = model_id
        threading.Thread(
            target=self._launch,
            args=(Path(str(model["path"])),),
            name="sls-inspector-loader",
            daemon=True,
        ).start()
        return {"accepted": True, "command": "start", "model_id": model_id}

    def _launch(self, path: Path) -> None:
        try:
            runtime = self._runtime_factory(path)
            with self._lock:
                self._runtime = runtime
                self._status = "CONNECTING"
            runtime.start()
        except Exception as error:
            with self._lock:
                self._status = "SETUP_ERROR"
                self._error = f"{type(error).__name__}: {error}"


def _action_label(action: Action) -> str:
    details = [
        value for value in (
            action.subject_id, action.target_id, action.option_id,
            action.node_id, action.reward_id,
        ) if value
    ]
    suffix = f" · {' → '.join(details)}" if details else ""
    return f"{action.kind.value}{suffix}"


def _action_presentation(decision: Decision, action: Action) -> dict[str, Any]:
    """Describe one policy decision without pretending it is one UI click."""

    observation = decision.observation
    entity_names = {
        entity.instance_id: entity.content_id
        for collection in (
            observation.choice_options,
            observation.reward_options,
            observation.event_options,
            observation.rest_options,
            observation.boss_relic_options,
            observation.relics,
            observation.potions,
        )
        for entity in collection
    }
    entity_names.update({item.instance_id: item.content_id for item in observation.shop_items})
    entity_names.update({card.instance_id: card.card_id for card in (
        observation.deck + observation.hand + observation.draw_pile
        + observation.discard_pile + observation.exhaust_pile
    )})
    subject = action.subject_id
    content = entity_names.get(subject or "")
    label = _action_label(action)
    composite = (
        action.kind is ActionKind.CHOOSE_CARD_REWARD
        and observation.screen is ScreenType.COMBAT_REWARD
    )
    ui_steps: list[str] = []
    execution_note = "1 个模型决策"
    if composite:
        chosen = content or subject or "该牌"
        label = f"选择卡牌奖励：{chosen}"
        ui_steps = ["打开三选一卡牌奖励", f"选择 {chosen}"]
        execution_note = "1 个模型决策 / 2 个游戏点击：" + " → ".join(ui_steps)
    elif content:
        label = f"{action.kind.value} · {content}"
    return {
        "label": label,
        "composite": composite,
        "ui_steps": ui_steps,
        "execution_note": execution_note,
    }


def _observation_summary(decision: Decision) -> dict[str, Any]:
    observation = decision.observation
    player = observation.player
    run = observation.run
    return {
        "act": run.act,
        "floor": run.floor,
        "screen": observation.screen.value,
        "ascension": run.ascension,
        "hp": player.current_hp,
        "max_hp": player.max_hp,
        "block": player.block,
        "energy": player.energy,
        "max_energy": player.max_energy,
        "gold": run.gold,
        "hand": [card.card_id for card in observation.hand],
        "enemies": [
            {
                "id": enemy.monster_id,
                "hp": enemy.current_hp,
                "max_hp": enemy.max_hp,
                "block": enemy.block,
                "intent": enemy.intent,
                "damage": enemy.intent_damage,
                "hits": enemy.intent_hits,
            }
            for enemy in observation.enemies
        ],
        "deck": [card.card_id for card in observation.deck],
        "relics": [item.content_id for item in observation.relics],
        "potions": [item.content_id for item in observation.potions],
    }


class InteractiveAgentRuntime(AgentRuntime):
    """Own policy/backend state in one thread and accept queued UI controls."""

    def __init__(
        self,
        *args: Any,
        delay_seconds: float = 1.0,
        card_reward_preview_seconds: float = 3.0,
        **kwargs: Any,
    ) -> None:
        super().__init__(*args, **kwargs)
        if not 0.0 <= delay_seconds <= 10.0:
            raise ValueError("delay_seconds must be between zero and ten")
        if not 0.0 <= card_reward_preview_seconds <= 10.0:
            raise ValueError("card_reward_preview_seconds must be between zero and ten")
        self._condition = threading.Condition()
        self._commands: deque[dict[str, Any]] = deque()
        self._exclusive_queued = False
        self._delay_seconds = float(delay_seconds)
        self._card_reward_preview_seconds = float(card_reward_preview_seconds)
        self._set_backend_card_reward_preview()
        self._revision = 0
        self._snapshot: dict[str, Any] = {
            "schema": "sls-live-inspector-state-v1",
            "revision": 0,
            "status": "CONNECTING",
            "mode": "PAUSED",
            "delay_seconds": self._delay_seconds,
            "card_reward_preview_seconds": self._card_reward_preview_seconds,
            "boundary_id": None,
            "actions": [],
            "error": None,
        }
        self._done = threading.Event()
        self._thread: threading.Thread | None = None

    @property
    def done(self) -> threading.Event:
        return self._done

    def start(self) -> None:
        if self._thread is not None:
            raise RuntimeError("inspector runtime has already started")
        self._thread = threading.Thread(
            target=self.run_interactive, name="sls-inspector-control", daemon=True,
        )
        self._thread.start()

    def state(self) -> dict[str, Any]:
        with self._condition:
            return deepcopy(self._snapshot)

    def _publish(
        self,
        status: str,
        *,
        mode: str,
        decision: Decision | None = None,
        score: PolicyScore | None = None,
        countdown_remaining: float | None = None,
        error: str | None = None,
    ) -> None:
        self._revision += 1
        state: dict[str, Any] = {
            "schema": "sls-live-inspector-state-v1",
            "revision": self._revision,
            "status": status,
            "mode": mode,
            "delay_seconds": self._delay_seconds,
            "card_reward_preview_seconds": self._card_reward_preview_seconds,
            "countdown_remaining": countdown_remaining,
            "boundary_id": boundary_id(decision) if decision is not None else None,
            "terminal": bool(decision.terminal) if decision is not None else False,
            "value": score.value if score is not None else None,
            "recommended_candidate_id": (
                score.recommended.candidate_id if score is not None else None
            ),
            "actions": [],
            "summary": _observation_summary(decision) if decision is not None else None,
            "observation": (
                decision.observation.to_dict() if decision is not None else None
            ),
            "error": error,
        }
        if decision is not None and score is not None:
            ranks = {
                item.candidate_id: rank for rank, item in enumerate(
                    sorted(score.actions, key=lambda item: item.probability, reverse=True),
                    start=1,
                )
            }
            actions = []
            for item in sorted(
                score.actions, key=lambda item: item.probability, reverse=True,
            ):
                action = decision.actions[item.index]
                actions.append({
                    "rank": ranks[item.candidate_id],
                    "index": item.index,
                    "candidate_id": item.candidate_id,
                    **_action_presentation(decision, action),
                    "action": action.to_dict(),
                    "logit": item.logit,
                    "probability": item.probability,
                    "recommended": item.candidate_id == score.recommended.candidate_id,
                })
            state["actions"] = actions
        with self._condition:
            if status != "PAUSED":
                self._exclusive_queued = False
            self._snapshot = state
            self._condition.notify_all()

    def submit(self, payload: dict[str, Any]) -> dict[str, Any]:
        command = str(payload.get("command") or "")
        if command not in {
            "pause", "resume", "step", "set_delay", "set_card_reward_preview",
            "execute", "stop",
        }:
            raise ValueError("unknown control command")
        with self._condition:
            status = str(self._snapshot["status"])
            if status in {"TERMINAL", "ERROR", "STOPPED"} and command != "stop":
                raise ControlConflict(f"runtime is {status.lower()}")
            queued: dict[str, Any] = {"command": command}
            if command == "set_delay":
                try:
                    delay = float(payload["delay_seconds"])
                except (KeyError, TypeError, ValueError) as error:
                    raise ValueError("delay_seconds must be a number") from error
                if not 0.0 <= delay <= 10.0:
                    raise ValueError("delay_seconds must be between zero and ten")
                queued["delay_seconds"] = delay
            elif command == "set_card_reward_preview":
                try:
                    preview = float(payload["card_reward_preview_seconds"])
                except (KeyError, TypeError, ValueError) as error:
                    raise ValueError(
                        "card_reward_preview_seconds must be a number"
                    ) from error
                if not 0.0 <= preview <= 10.0:
                    raise ValueError(
                        "card_reward_preview_seconds must be between zero and ten"
                    )
                queued["card_reward_preview_seconds"] = preview
            elif command in {"resume", "step", "execute"}:
                if status != "PAUSED":
                    raise ControlConflict(f"{command} requires a paused boundary")
                if self._exclusive_queued:
                    raise ControlConflict("another boundary command is already queued")
                if command == "execute":
                    requested_boundary = str(payload.get("boundary_id") or "")
                    candidate_id = str(payload.get("candidate_id") or "")
                    if requested_boundary != self._snapshot.get("boundary_id"):
                        raise ControlConflict("decision boundary is stale")
                    legal = {
                        str(item["candidate_id"]) for item in self._snapshot.get("actions", [])
                    }
                    if candidate_id not in legal:
                        raise ControlConflict("candidate is stale or illegal")
                    queued["boundary_id"] = requested_boundary
                    queued["candidate_id"] = candidate_id
                self._exclusive_queued = True
            self._commands.append(queued)
            self._condition.notify_all()
            return {"accepted": True, "command": command}

    def _next_command(self, timeout: float | None = None) -> dict[str, Any] | None:
        with self._condition:
            if not self._commands:
                self._condition.wait(timeout)
            if not self._commands:
                return None
            command = self._commands.popleft()
            return command

    def _apply_common(self, command: dict[str, Any]) -> str | None:
        name = command["command"]
        if name == "set_delay":
            self._delay_seconds = float(command["delay_seconds"])
            return "delay"
        if name == "set_card_reward_preview":
            self._card_reward_preview_seconds = float(
                command["card_reward_preview_seconds"]
            )
            self._set_backend_card_reward_preview()
            return "preview"
        if name == "stop":
            return "stop"
        if name == "pause":
            return "pause"
        return None

    def _set_backend_card_reward_preview(self) -> None:
        setter = getattr(self.backend, "set_card_reward_preview_seconds", None)
        if setter is not None:
            setter(self._card_reward_preview_seconds)

    def _wait_paused(
        self, decision: Decision, score: PolicyScore,
    ) -> tuple[str, int | None]:
        self._publish("PAUSED", mode="PAUSED", decision=decision, score=score)
        while True:
            command = self._next_command()
            assert command is not None
            common = self._apply_common(command)
            if common in {"delay", "preview", "pause"}:
                self._publish("PAUSED", mode="PAUSED", decision=decision, score=score)
                continue
            if common == "stop":
                return "stop", None
            if command["command"] == "resume":
                return "auto", None
            if command["command"] == "step":
                return "step", score.recommended.index
            if command["command"] == "execute":
                if command["boundary_id"] != boundary_id(decision):
                    continue
                index = next(
                    (item.index for item in score.actions
                     if item.candidate_id == command["candidate_id"]),
                    None,
                )
                if index is not None:
                    return "manual", index

    def _wait_auto(
        self, decision: Decision, score: PolicyScore,
    ) -> tuple[str, int | None]:
        started = time.monotonic()
        while True:
            remaining = max(0.0, self._delay_seconds - (time.monotonic() - started))
            if remaining <= 0.0:
                return "auto", score.recommended.index
            self._publish(
                "COUNTDOWN", mode="AUTO", decision=decision, score=score,
                countdown_remaining=remaining,
            )
            command = self._next_command(min(remaining, 0.1))
            if command is None:
                continue
            common = self._apply_common(command)
            if common == "stop":
                return "stop", None
            if common == "pause":
                return "paused", None
            if common in {"delay", "preview"}:
                continue

    def _drain_after_execution(self, mode: str) -> str:
        while True:
            command = self._next_command(0.0)
            if command is None:
                return mode
            common = self._apply_common(command)
            if common == "stop":
                return "stop"
            if common == "pause":
                mode = "paused"

    def run_interactive(self) -> None:
        decision: Decision | None = None
        score: PolicyScore | None = None
        mode = "paused"
        try:
            decision = self.attach()
            if decision.terminal:
                self._publish("TERMINAL", mode="PAUSED", decision=decision)
                return
            score = self.score(decision)
            while True:
                if mode == "paused":
                    operation, index = self._wait_paused(decision, score)
                else:
                    operation, index = self._wait_auto(decision, score)
                if operation == "stop":
                    self._publish("STOPPED", mode="PAUSED", decision=decision, score=score)
                    return
                if operation == "paused":
                    mode = "paused"
                    continue
                if operation == "auto" and index is None:
                    mode = "auto"
                    continue
                assert index is not None
                selection_source = "manual" if operation == "manual" else "model"
                next_mode = "paused" if operation in {"step", "manual"} else "auto"
                self._publish("EXECUTING", mode=next_mode.upper(), decision=decision, score=score)
                transition = self.execute_scored_action(
                    decision,
                    score,
                    index,
                    selection_source=selection_source,
                    inspector_mode=operation,
                    delay_seconds=self._delay_seconds,
                    card_reward_preview_seconds=self._card_reward_preview_seconds,
                )
                decision = transition.decision
                if decision.terminal:
                    self._publish("TERMINAL", mode="PAUSED", decision=decision)
                    return
                score = self.score(decision)
                mode = self._drain_after_execution(next_mode)
                if mode == "stop":
                    self._publish("STOPPED", mode="PAUSED", decision=decision, score=score)
                    return
        except Exception as error:  # Surface controller failures in the dashboard.
            with self._condition:
                self._publish(
                    "ERROR", mode="PAUSED", decision=decision, score=score,
                    error=f"{type(error).__name__}: {error}",
                )
        finally:
            self._done.set()


def make_handler(runtime: Any):  # type: ignore[no-untyped-def]
    class InspectorHandler(BaseHTTPRequestHandler):
        server_version = "SLSInspector/1"

        def log_message(self, _format: str, *args: Any) -> None:
            return

        def _json(self, status: int, payload: dict[str, Any]) -> None:
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self) -> None:  # noqa: N802
            if self.path == "/":
                body = INSPECTOR_HTML.encode("utf-8")
                self.send_response(HTTPStatus.OK)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(body)))
                self.send_header("Cache-Control", "no-store")
                self.end_headers()
                self.wfile.write(body)
            elif self.path == "/api/state":
                self._json(HTTPStatus.OK, runtime.state())
            else:
                self._json(HTTPStatus.NOT_FOUND, {"error": "not found"})

        def do_POST(self) -> None:  # noqa: N802
            if self.path != "/api/control":
                self._json(HTTPStatus.NOT_FOUND, {"error": "not found"})
                return
            if self.headers.get_content_type() != "application/json":
                self._json(HTTPStatus.UNSUPPORTED_MEDIA_TYPE, {"error": "JSON required"})
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
                if length <= 0 or length > 65536:
                    raise ValueError("invalid request size")
                payload = json.loads(self.rfile.read(length))
                if not isinstance(payload, dict):
                    raise ValueError("request body must be an object")
                result = runtime.submit(payload)
            except ControlConflict as error:
                self._json(HTTPStatus.CONFLICT, {"error": str(error)})
            except (ValueError, json.JSONDecodeError) as error:
                self._json(HTTPStatus.BAD_REQUEST, {"error": str(error)})
            else:
                self._json(HTTPStatus.ACCEPTED, result)

    return InspectorHandler


def create_server(
    runtime: Any, host: str = "127.0.0.1", port: int = 8765,
) -> ThreadingHTTPServer:
    if host not in {"127.0.0.1", "localhost", "::1"}:
        raise ValueError("live inspector may only bind to a loopback address")
    if not 0 <= port <= 65535:
        raise ValueError("port must be between 0 and 65535")
    return ThreadingHTTPServer((host, port), make_handler(runtime))


INSPECTOR_HTML = r"""<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<title>SLS 模型检查器</title><style>
:root{color-scheme:dark;--bg:#11151c;--panel:#1b2230;--line:#354158;--muted:#9eabc0;--accent:#64d6c4;--warn:#ffcf70}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:#edf2fa;font:14px system-ui,sans-serif}
main{max-width:1400px;margin:auto;padding:20px}.bar,.grid{display:flex;gap:12px;flex-wrap:wrap}.bar{align-items:center;background:var(--panel);padding:14px;border-radius:10px;position:sticky;top:0;z-index:2}
button,input{font:inherit}button{background:#28344a;color:#fff;border:1px solid var(--line);padding:8px 13px;border-radius:7px;cursor:pointer}button.primary{background:#187b70}button:disabled{opacity:.4;cursor:not-allowed}
.pill{padding:5px 9px;border:1px solid var(--line);border-radius:99px}.muted{color:var(--muted)}.panel{background:var(--panel);padding:16px;border-radius:10px;margin-top:14px}.grid>div{min-width:110px}h2{font-size:16px;margin:0 0 12px}
table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:9px;border-bottom:1px solid var(--line)}tr.recommended{background:#173933}tr.selected{outline:2px solid var(--warn)}code{font-size:12px}pre{white-space:pre-wrap;overflow:auto;max-height:500px}#error{color:#ff8d8d}input[type=range]{width:220px}
</style></head><body><main>
<section id="setup" class="panel"><h2>开始本地模型测试</h2><p class="muted">选择已导出的策略模型。只有点击“加载并连接游戏”后，检查器才会连接 CommunicationMod；连接完成后仍保持暂停。</p><div class="bar"><label>测试模型 <select id="models"></select></label><button id="start" class="primary">加载并连接游戏</button></div><div id="modelInfo" class="grid"></div><ol><li>先在这里选择模型并点击开始。</li><li>再在游戏中创建与模型匹配的 Ironclad / Ascension 新局。</li><li>页面显示 PAUSED 后用“模型单步”开始检查。</li></ol></section>
<div class="bar"><span id="status" class="pill">CONNECTING</span><button id="resume" class="primary">运行</button><button id="pause">暂停</button><button id="step">模型单步</button><button id="execute">执行所选动作</button><button id="stop">停止控制器</button><label>出牌延迟 <input id="delay" type="range" min="0" max="10" step="0.1"><b id="delayText"></b></label><label>三选一展示 <input id="rewardPreview" type="range" min="0" max="10" step="0.1"><b id="rewardPreviewText"></b></label><span id="countdown" class="muted"></span></div>
<div id="error"></div><section class="panel"><h2>当前状态</h2><div id="summary" class="grid"></div><p>State value：<b id="value">—</b> <span class="muted">（这是状态价值估计，不是动作 Q 值）</span></p></section>
<section class="panel"><h2>全部合法动作</h2><p id="semanticNote" class="muted">模型单步执行一个语义决策；通常对应一次游戏点击。</p><table><thead><tr><th>选择</th><th>排名</th><th>动作</th><th>概率</th><th>Logit</th><th>执行语义</th></tr></thead><tbody id="actions"></tbody></table></section>
<details class="panel"><summary>公开 Observation JSON</summary><pre id="raw"></pre></details>
</main><script>
let state=null, selected=null, delayDragging=false, previewDragging=false;
async function control(command, extra={}){const r=await fetch('/api/control',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({command,...extra})});const x=await r.json();if(!r.ok)throw Error(x.error||r.statusText)}
function esc(x){return String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function render(s){state=s;document.querySelector('#status').textContent=`${s.status} · ${s.mode}`;document.querySelector('#error').textContent=s.error||'';
 const setup=['SETUP','SETUP_ERROR','LOADING'].includes(s.status);document.querySelector('#setup').style.display=setup?'block':'none';const modelSelect=document.querySelector('#models');const wanted=modelSelect.value||s.selected_model_id||'';modelSelect.innerHTML=(s.models||[]).map(m=>`<option value="${esc(m.model_id)}" ${m.model_id===wanted?'selected':''}>${esc(m.name)} · ${esc(m.goal)} · A${m.ascension_min}-${m.ascension_max}</option>`).join('');const chosen=(s.models||[]).find(m=>m.model_id===modelSelect.value);document.querySelector('#modelInfo').innerHTML=chosen?`<div><span class="muted">阶段</span><br><b>${esc(chosen.profile||chosen.goal)}</b></div><div><span class="muted">训练步数</span><br><b>${chosen.environment_steps==null?'未记录':Number(chosen.environment_steps).toLocaleString()}</b></div><div><span class="muted">更新次数</span><br><b>${chosen.updates??'未记录'}</b></div><div><span class="muted">Ascension</span><br><b>${chosen.ascension_min}–${chosen.ascension_max}</b></div><div><span class="muted">权重验证</span><br><b>${chosen.verified_weight_match?'与 latest.pt 一致':'独立工件'}</b></div><div><span class="muted">大小</span><br><b>${chosen.size_mb} MB</b></div><div><span class="muted">模型文件</span><br><code>${esc(chosen.path)}</code></div><div><span class="muted">来源 checkpoint</span><br><code>${esc(chosen.source_checkpoint||'未记录')}</code></div>`:'';document.querySelector('#start').disabled=s.status==='LOADING'||!chosen;
 if(s.delay_seconds!=null){if(!delayDragging)document.querySelector('#delay').value=s.delay_seconds;document.querySelector('#delayText').textContent=` ${Number(s.delay_seconds).toFixed(1)}s`;}document.querySelector('#countdown').textContent=s.countdown_remaining==null?'':`倒计时 ${s.countdown_remaining.toFixed(1)}s`;
 if(s.card_reward_preview_seconds!=null){if(!previewDragging)document.querySelector('#rewardPreview').value=s.card_reward_preview_seconds;document.querySelector('#rewardPreviewText').textContent=` ${Number(s.card_reward_preview_seconds).toFixed(1)}s`;}
 const paused=s.status==='PAUSED';document.querySelector('#resume').disabled=!paused;document.querySelector('#step').disabled=!paused;document.querySelector('#execute').disabled=!paused||!selected;document.querySelector('#pause').disabled=!['PAUSED','COUNTDOWN','EXECUTING'].includes(s.status);
 const q=s.summary||{};document.querySelector('#summary').innerHTML=Object.entries(q).map(([k,v])=>`<div><span class="muted">${esc(k)}</span><br><b>${esc(typeof v==='object'?JSON.stringify(v):v)}</b></div>`).join('');document.querySelector('#value').textContent=s.value==null?'—':Number(s.value).toFixed(5);document.querySelector('#raw').textContent=JSON.stringify(s.observation,null,2);
 const hasComposite=s.actions.some(a=>a.composite);document.querySelector('#semanticNote').textContent=hasComposite?'注意：这里的卡牌奖励已经包含三张牌的独立评分。选择卡牌是 1 个模型决策，但会连续执行“打开奖励”和“选择该牌”2 个游戏点击。':'模型单步执行一个语义决策；通常对应一次游戏点击。';
 if(!s.actions.some(a=>a.candidate_id===selected))selected=null;document.querySelector('#actions').innerHTML=s.actions.map(a=>`<tr class="${a.recommended?'recommended ':''}${a.candidate_id===selected?'selected':''}" data-id="${encodeURIComponent(a.candidate_id)}"><td><input type="radio" name="action" ${a.candidate_id===selected?'checked':''}></td><td>${a.rank}${a.recommended?' ★':''}</td><td>${esc(a.label)}</td><td>${(a.probability*100).toFixed(3)}%</td><td>${a.logit.toFixed(5)}</td><td>${esc(a.execution_note)}</td></tr>`).join('');document.querySelectorAll('tbody tr').forEach(tr=>tr.onclick=()=>{selected=decodeURIComponent(tr.dataset.id);render(state)})}
async function poll(){try{const r=await fetch('/api/state',{cache:'no-store'});render(await r.json())}catch(e){document.querySelector('#error').textContent=e}setTimeout(poll,250)}
for(const id of ['resume','pause','step','stop'])document.querySelector('#'+id).onclick=()=>control(id).catch(e=>alert(e));document.querySelector('#execute').onclick=()=>control('execute',{boundary_id:state.boundary_id,candidate_id:selected}).catch(e=>alert(e));
document.querySelector('#start').onclick=()=>control('start',{model_id:document.querySelector('#models').value}).catch(e=>alert(e));document.querySelector('#models').onchange=()=>render(state);
const slider=document.querySelector('#delay');slider.onpointerdown=()=>delayDragging=true;slider.oninput=()=>document.querySelector('#delayText').textContent=` ${Number(slider.value).toFixed(1)}s`;slider.onchange=()=>{delayDragging=false;control('set_delay',{delay_seconds:Number(slider.value)}).catch(e=>alert(e))};
const preview=document.querySelector('#rewardPreview');preview.onpointerdown=()=>previewDragging=true;preview.oninput=()=>document.querySelector('#rewardPreviewText').textContent=` ${Number(preview.value).toFixed(1)}s`;preview.onchange=()=>{previewDragging=false;control('set_card_reward_preview',{card_reward_preview_seconds:Number(preview.value)}).catch(e=>alert(e))};poll();
</script></body></html>"""
