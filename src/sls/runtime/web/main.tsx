import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";

type Data = Record<string, any>;
type Node = {
  node_id: string;
  x: number;
  y: number;
  visible_room_type: string;
  reachable: boolean;
  outgoing_node_ids: string[];
};
const rooms: Record<string, string> = {
  MONSTER: "⚔",
  ELITE: "◆",
  BURNING_ELITE: "◆",
  EVENT: "?",
  SHOP: "＄",
  REST: "♨",
  TREASURE: "▣",
  BOSS: "♛",
};
const phases: Record<string, string> = {
  NEOW: "涅奥祝福",
  MAP: "选择路线",
  COMBAT: "战斗",
  CARD_REWARD: "卡牌奖励",
  COMBAT_REWARD: "战斗奖励",
  EVENT: "事件",
  SHOP: "商店",
  TREASURE: "宝箱",
  REST: "休息处",
  BOSS_REWARD: "Boss 奖励",
  GAME_OVER: "游戏结束",
};
const fmt = (v: any) => (v == null ? "—" : String(v));
const pct = (v: number) => `${(100 * v).toFixed(1)}%`;
function NameList({
  title,
  items,
  field = "content_id",
}: {
  title: string;
  items: Data[];
  field?: string;
}) {
  return items?.length ? (
    <div className="tokens">
      <label>{title}</label>
      {items.map((item, i) => (
        <span key={item.instance_id || i} title={JSON.stringify(item)}>
          {item[field]}
          {item.upgrades > 0 ? "+" : ""}
          {item.current_cost != null
            ? item.current_cost === -2
              ? " · 不可打出"
              : item.current_cost === -1
                ? " · X费"
                : ` · ${item.current_cost}费`
            : ""}
          {item.properties?.amount != null
            ? ` · ${item.properties.amount}`
            : ""}
          {item.owner_id ? ` · ${item.owner_id}` : ""}
          {item.price != null ? ` · ${item.price}金币` : ""}
        </span>
      ))}
    </div>
  ) : null;
}

function MapView({
  visual,
  actions,
  onSelect,
  selected,
}: {
  visual: Data;
  actions: Data[];
  onSelect: (id: string) => void;
  selected: string | null;
}) {
  const [scale, setScale] = useState(1),
    [pan, setPan] = useState({ x: 0, y: 0 });
  const drag = useRef<Data | null>(null);
  const nodes: Node[] = visual?.map || [],
    route: string[] = visual?.route || [],
    current = visual?.context?.node_id;
  const lookup = new Map(nodes.map((n) => [n.node_id, n]));
  const pos = (n: Node) => ({ x: 44 + n.x * 43, y: 748 - n.y * 45 });
  const preference = new Map(
    actions
      .filter((a) => a.action.kind === "CHOOSE_MAP_NODE")
      .map((a) => [a.action.node_id, a]),
  );
  function center() {
    const n = lookup.get(current);
    setScale(1);
    setPan(
      n
        ? { x: 0, y: Math.max(-280, Math.min(0, 350 - pos(n).y)) }
        : { x: 0, y: 0 },
    );
  }
  return (
    <section className="panel map-panel">
      <div className="section-title">
        <div>
          <label>THE SPIRE</label>
          <h2>第一幕 · 真实地图</h2>
        </div>
        <div className="map-buttons">
          <button onClick={() => setScale(Math.min(2.5, scale + 0.2))}>
            ＋
          </button>
          <button onClick={() => setScale(Math.max(0.6, scale - 0.2))}>
            −
          </button>
          <button onClick={center}>定位</button>
        </div>
      </div>
      <div className="map-canvas">
        <svg
          viewBox="0 0 350 800"
          role="img"
          aria-label="原版公开地图"
          onWheel={(e) =>
            setScale(Math.max(0.6, Math.min(2.5, scale - e.deltaY * 0.001)))
          }
          onPointerDown={(e) => {
            if ((e.target as Element).closest(".node")) return;
            drag.current = { x: e.clientX, y: e.clientY, pan };
            e.currentTarget.setPointerCapture(e.pointerId);
          }}
          onPointerMove={(e) => {
            if (drag.current)
              setPan({
                x: drag.current.pan.x + (e.clientX - drag.current.x),
                y: drag.current.pan.y + (e.clientY - drag.current.y),
              });
          }}
          onPointerUp={() => {
            drag.current = null;
          }}
        >
          <defs>
            <pattern
              id="dots"
              width="25"
              height="25"
              patternUnits="userSpaceOnUse"
            >
              <circle cx="1" cy="1" r=".7" fill="#2a323a" />
            </pattern>
            <filter id="glow">
              <feGaussianBlur stdDeviation="2" />
            </filter>
          </defs>
          <rect width="350" height="800" fill="url(#dots)" />
          <g transform={`translate(${pan.x},${pan.y}) scale(${scale})`}>
            {nodes.flatMap((n) =>
              n.outgoing_node_ids.map((id) => {
                const child = lookup.get(id);
                if (!child) return null;
                const a = pos(n),
                  b = pos(child),
                  taken = route.some(
                    (r, i) => r === n.node_id && route[i + 1] === id,
                  ),
                  recommended =
                    n.node_id === current && preference.get(id)?.recommended;
                return (
                  <path
                    key={`${n.node_id}-${id}`}
                    d={`M${a.x},${a.y} C${a.x},${a.y - 22} ${b.x},${b.y + 22} ${b.x},${b.y}`}
                    className={
                      taken
                        ? "edge visited"
                        : recommended
                          ? "edge recommended"
                          : "edge"
                    }
                  />
                );
              }),
            )}
            {nodes.map((n) => {
              const p = pos(n),
                choice = preference.get(n.node_id),
                visited = route.includes(n.node_id),
                active = current === n.node_id;
              return (
                <g
                  key={n.node_id}
                  className={`node ${active ? "current" : ""} ${visited ? "visited" : ""} ${choice ? "available" : ""} ${selected === n.node_id ? "selected" : ""}`}
                  transform={`translate(${p.x},${p.y})`}
                  onClick={() => onSelect(n.node_id)}
                  tabIndex={0}
                  role="button"
                  aria-label={`${n.visible_room_type} ${n.node_id}`}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") onSelect(n.node_id);
                  }}
                >
                  <title>{`${n.visible_room_type} · ${n.node_id}${choice ? " · 动作概率 " + pct(choice.probability) : ""}`}</title>
                  <circle r="15" />
                  <text textAnchor="middle" dy="5">
                    {rooms[n.visible_room_type] || "·"}
                  </text>
                  {choice && (
                    <text className="route-prob" textAnchor="middle" y="-20">
                      {pct(choice.probability)}
                    </text>
                  )}
                </g>
              );
            })}
          </g>
        </svg>
        {!nodes.length && (
          <div className="empty map-empty">
            等待原版公开地图
            <br />
            <small>不会生成模拟地图</small>
          </div>
        )}
      </div>
      <div className="legend">
        <span className="gold">● 当前</span>
        <span className="green">● 已走路线</span>
        <span className="blue">● 合法选择</span>
        <span>拖动平移 · 滚轮缩放</span>
      </div>
    </section>
  );
}

function Scores({
  actions,
  value,
  selected,
}: {
  actions: Data[];
  value: any;
  selected?: string;
}) {
  const [all, setAll] = useState(false);
  return (
    <section className="panel scores">
      <div className="section-title">
        <div>
          <label>POLICY · DETERMINISTIC</label>
          <h2>AI 决策 · Top 5</h2>
        </div>
        <span className="value">
          State Value <b>{value == null ? "—" : Number(value).toFixed(3)}</b>
        </span>
      </div>
      <p className="hint">动作概率表示策略偏好，不是胜率。Logits 不是 Q 值。</p>
      {(all ? actions : actions.slice(0, 5)).map((a) => (
        <div
          key={a.candidate_id}
          className={`score-row ${a.recommended ? "top" : ""}`}
        >
          <div className="rank">{a.rank}</div>
          <div className="score-main">
            <div className="score-label">
              <span>{a.label}</span>
              <strong>{pct(a.probability)}</strong>
            </div>
            <div className="bar">
              <i style={{ width: pct(a.probability) }} />
            </div>
            <small>
              Logit {Number(a.logit).toFixed(4)}{" "}
              {a.candidate_id === selected
                ? " · 实际选择"
                : a.recommended
                  ? " · 模型推荐"
                  : ""}
              {a.composite ? " · 两个原版点击，一个模型决策" : ""}
            </small>
          </div>
        </div>
      ))}
      {!actions.length && <div className="empty">等待合法决策边界</div>}
      {actions.length > 5 && (
        <button className="text-button" onClick={() => setAll(!all)}>
          {all ? "收起" : "展开全部 " + actions.length + " 个合法动作"}
        </button>
      )}
    </section>
  );
}

function CurrentState({ state }: { state: Data }) {
  const o = state.observation || {},
    q = state.summary || {},
    ctx = state.visual?.context || {};
  return (
    <section className="panel current-panel">
      <div className="section-title">
        <div>
          <label>CONFIRMED GAME STATE</label>
          <h2>{phases[q.screen] || "等待游戏连接"}</h2>
        </div>
        <span className="floor">F{q.floor ?? "—"}</span>
      </div>
      <p className="hint">
        节点 {ctx.node_id || "尚未进入地图"} · 回合 {fmt(ctx.turn)}
      </p>
      <div className="stats">
        <div>
          <label>生命</label>
          <b className="green">
            {fmt(q.hp)}
            <small> / {fmt(q.max_hp)}</small>
          </b>
        </div>
        <div>
          <label>格挡</label>
          <b>{fmt(q.block)}</b>
        </div>
        <div>
          <label>能量</label>
          <b className="gold">{fmt(q.energy)}</b>
        </div>
        <div>
          <label>金币</label>
          <b>{fmt(q.gold)}</b>
        </div>
      </div>
      <div className="pile-counts">
        抽牌 {state.observation ? (o.draw_pile?.length ?? 0) : "—"} · 弃牌{" "}
        {state.observation ? (o.discard_pile?.length ?? 0) : "—"} · 消耗{" "}
        {state.observation ? (o.exhaust_pile?.length ?? 0) : "—"}
      </div>
      {(q.enemies || []).map((e: Data, i: number) => (
        <div className="enemy" key={i}>
          <b>{e.id}</b>
          <span>
            {e.hp}/{e.max_hp} HP · 格挡 {e.block}
          </span>
          <small>
            {e.intent}
            {e.damage ? ` · ${e.damage} × ${e.hits}` : ""}
          </small>
        </div>
      ))}
      {ctx.event_name && <p>{ctx.event_name}</p>}
      {ctx.event_body && <p className="event-body">{ctx.event_body}</p>}
      <NameList title="手牌" items={o.hand} field="card_id" />
      <NameList title="状态 / Buffs" items={o.powers} />
      <NameList title="奖励" items={o.reward_options} />
      <NameList title="卡牌 / 选择" items={o.choice_options} />
      <NameList title="事件选项" items={o.event_options} />
      <NameList title="商店" items={o.shop_items} />
      <NameList title="休息" items={o.rest_options} />
      <NameList title="遗物" items={o.relics} />
      <NameList title="药水" items={o.potions} />
      <details>
        <summary>完整公开 Observation</summary>
        <pre>{JSON.stringify(o, null, 2)}</pre>
      </details>
    </section>
  );
}

function App() {
  const [seed, setSeed] = useState("0");
  const [state, setState] = useState<Data>({
      status: "CONNECTING",
      actions: [],
    }),
    [error, setError] = useState(""),
    [model, setModel] = useState(""),
    [busy, setBusy] = useState(false),
    [entries, setEntries] = useState<Data[]>([]),
    [node, setNode] = useState<string | null>(null),
    [review, setReview] = useState<Data | null>(null);
  const cursor = useRef(0);
  useEffect(() => {
    let active = true,
      timer: ReturnType<typeof setTimeout>;
    async function poll() {
      try {
        const r = await fetch("/api/state", { cache: "no-store" });
        if (!r.ok) throw Error("状态接口 " + r.status);
        const s = await r.json();
        if (!active) return;
        setState(s);
        setError("");
        if ((s.visual?.history_count || 0) < cursor.current) {
          cursor.current = 0;
          setEntries([]);
        }
        if ((s.visual?.history_count || 0) > cursor.current) {
          const h = await fetch(
            `/api/history?offset=${cursor.current}&limit=100`,
          );
          if (!h.ok) throw Error("历史读取失败");
          const data = await h.json();
          if (active) {
            cursor.current += data.entries.length;
            setEntries((prev) => [...prev, ...data.entries]);
          }
        }
      } catch (e) {
        if (active) setError(String(e));
      } finally {
        if (active) timer = setTimeout(poll, 300);
      }
    }
    poll();
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, []);
  async function command(name: string, extra: Data = {}) {
    setBusy(true);
    try {
      const r = await fetch("/api/control", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          command: name,
          boundary_id: state.boundary_id,
          ...extra,
        }),
      });
      const data = await r.json();
      if (!r.ok) throw Error(data.error);
      setError("");
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  }
  const setup = ["SETUP", "SETUP_ERROR", "LOADING"].includes(state.status),
    paused = state.status === "PAUSED",
    visual = state.visual || {},
    shown = review
      ? {
          ...state,
          observation: review.observation,
          summary: review.summary || {
            ...review.observation.run,
            ...review.observation.player,
            hp: review.observation.player.current_hp,
            screen: review.screen,
          },
          visual: { context: review.context || { node_id: review.node_id } },
        }
      : state;
  const nodeInfo = visual.nodes?.[node || ""];
  const filtered = node ? entries.filter((e) => e.node_id === node) : entries;
  return (
    <main>
      <header>
        <div className="brand-mark">S</div>
        <div>
          <label>SLS · ORIGINAL GAME</label>
          <h1>Visual Inspector</h1>
        </div>
        <span className={`status ${error ? "error" : ""}`}>
          {error ? "连接异常" : state.status}
        </span>
      </header>
      <div className="run-strip">
        <span>IRONCLAD · A{fmt(state.summary?.ascension)}</span>
        <span>ACT {fmt(state.summary?.act)}</span>
        <span>
          SEED <b>{visual.context?.seed || "—"}</b>
        </span>
        <span>{visual.history_count || 0} decisions</span>
      </div>
      {(error || state.error) && (
        <div className="alert">{error || state.error}</div>
      )}
      {setup && (
        <section className="panel setup">
          <label>选择推理模型 · 由原版游戏启动唯一控制器</label>
          <select
            value={model || state.selected_model_id || ""}
            onChange={(e) => setModel(e.target.value)}
          >
            {(state.models || []).map((m: Data) => (
              <option key={m.model_id} value={m.model_id}>
                {m.name} · {m.goal} · A{m.ascension_min}–{m.ascension_max}
              </option>
            ))}
          </select>
          <button
            className="primary"
            disabled={
              busy || state.status === "LOADING" || !state.models?.length
            }
            onClick={() =>
              command("start", { model_id: model || state.selected_model_id })
            }
          >
            连接所选模型
          </button>
          <div className="new-run">
            <input
              aria-label="新局 Seed"
              value={seed}
              onChange={(e) => setSeed(e.target.value)}
              placeholder="64 位整数 Seed"
            />
            <button
              disabled={
                busy || state.status === "LOADING" || !state.models?.length
              }
              onClick={() =>
                command("start", {
                  model_id: model || state.selected_model_id,
                  new_run: true,
                  seed,
                })
              }
            >
              从原版主菜单开始新局
            </button>
          </div>
          <p className="hint">
            也可在右侧原版游戏手动开始战士 A20
            新局；控制器只从涅奥边界开始，或恢复已确认的记忆。不会覆盖存档。
          </p>
        </section>
      )}
      <div className="model-line">
        {state.selected_model?.name || "尚未选择模型"}
        <details>
          <summary>来源身份</summary>
          <pre>{JSON.stringify(state.selected_model, null, 2)}</pre>
        </details>
      </div>
      {review && (
        <div className="review-banner">
          只读回顾 · Step {review.step}
          <button onClick={() => setReview(null)}>返回实时</button>
        </div>
      )}
      <div className="workspace">
        <MapView
          visual={visual}
          actions={state.actions || []}
          selected={node}
          onSelect={(id) => {
            setNode(id);
            setReview(null);
          }}
        />
        <div className="side">
          <CurrentState state={shown} />
          <Scores
            actions={review?.actions || state.actions || []}
            value={review?.value ?? state.value}
            selected={
              review?.selected_candidate_id || state.selected_candidate_id
            }
          />
        </div>
      </div>
      <section className="panel controls">
        <div>
          <label>PLAYBACK</label>
          <p>
            {state.status === "EXECUTING"
              ? "等待原版确认 · 暂停将在确认后生效"
              : state.countdown_remaining != null
                ? `下一动作 ${state.countdown_remaining.toFixed(1)}s`
                : "每个动作都等待真实游戏确认"}
          </p>
        </div>
        <div className="control-buttons">
          <button
            disabled={busy || !paused || !!error}
            onClick={() => command("step")}
          >
            单步 →
          </button>
          <button
            className="primary"
            disabled={busy || !paused || !!error}
            onClick={() => command("resume")}
          >
            ▶ 自动
          </button>
          <button
            disabled={
              busy ||
              setup ||
              ["TERMINAL", "STOPPED", "ERROR"].includes(state.status)
            }
            onClick={() => command("pause")}
          >
            Ⅱ 暂停
          </button>
          <select
            aria-label="播放速度"
            value={state.delay_seconds ?? 1}
            onChange={(e) =>
              command("set_delay", { delay_seconds: Number(e.target.value) })
            }
          >
            <option value={3}>慢速 · 3s</option>
            <option value={1}>正常 · 1s</option>
            <option value={0.2}>快速 · 0.2s</option>
          </select>
        </div>
      </section>
      {state.terminal && (
        <section className="panel">
          <label>RUN COMPLETE · AI 已停止</label>
          <h2>{state.summary?.hp > 0 ? "任务终点已确认" : "游戏结束"}</h2>
          <p>
            Seed {fmt(visual.context?.seed)} · {visual.history_count} decisions
            · 最终生命 {fmt(state.summary?.hp)} · {visual.route?.length || 0}{" "}
            个已观测节点
          </p>
          <p>
            已确认战斗胜利{" "}
            {
              Object.values(visual.nodes || {}).filter(
                (n: any) => n.result === "Victory",
              ).length
            }{" "}
            · 未记录的数据不作推断
          </p>
        </section>
      )}
      <section className="panel history">
        <div className="section-title">
          <div>
            <label>CONFIRMED DECISION JOURNAL</label>
            <h2>{node ? "节点 " + node : "完整决策历史"}</h2>
          </div>
          {node && <button onClick={() => setNode(null)}>全部节点</button>}
        </div>
        {nodeInfo && (
          <div className="node-info">
            F{nodeInfo.floor} · HP {nodeInfo.hp_before} → {nodeInfo.hp_latest} ·{" "}
            {nodeInfo.decisions} decisions · {nodeInfo.result || nodeInfo.phase}
          </div>
        )}
        {!filtered.length && (
          <div className="empty">尚无已确认动作。点击地图节点可筛选历史。</div>
        )}
        <div className="history-list">
          {filtered.map((e) => (
            <button
              className={`history-entry ${review?.step === e.step ? "active" : ""}`}
              key={e.step}
              onClick={() => setReview(e)}
            >
              <span>
                #{e.step}
                <small>
                  F{e.floor} · {phases[e.screen] || e.screen}
                </small>
              </span>
              <b>
                {e.actions?.find(
                  (a: Data) => a.candidate_id === e.selected_candidate_id,
                )?.label || e.selected_action.kind}
              </b>
              <span className="ack">✓ {e.confirmation}</span>
            </button>
          ))}
        </div>
        {review && (
          <details open>
            <summary>Step {review.step} · 动作和确认详情</summary>
            <pre>{JSON.stringify(review, null, 2)}</pre>
          </details>
        )}
      </section>
      <footer>
        原版游戏是状态来源 · 暂停保持 GRU 记忆 · 浏览器刷新不执行动作
        <br />
        New
        Run：在原版菜单开启新局并重新启动控制器；模型切换需重新启动，避免继承旧记忆。
      </footer>
    </main>
  );
}
createRoot(document.getElementById("root")!).render(<App />);
