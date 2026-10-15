"""Observational diagnostics; no optimizer steps or random-stream consumption."""

from __future__ import annotations

import torch


def act_diagnostics(rollout) -> dict[str, float]:
    if rollout.acts is None:
        return {}
    result = {}
    if rollout.acts.shape != rollout.advantages.shape:
        raise ValueError("rollout act metadata shape mismatch")
    for act in range(1, 5):
        mask = rollout.acts == act
        count = int(mask.sum())
        prefix = f"act_{act}_"
        result[prefix + "decisions"] = float(count)
        if not count:
            continue
        advantages = rollout.advantages[mask]
        result[prefix + "advantage_mean"] = float(advantages.mean())
        result[prefix + "advantage_std"] = float(advantages.std(unbiased=False))
        for label, quantile in (("min", 0.), ("p10", .1), ("median", .5), ("p90", .9), ("max", 1.)):
            result[prefix + "advantage_" + label] = float(torch.quantile(advantages, quantile))
        result[prefix + "value_mse_old"] = float((rollout.returns[mask] - rollout.old_values[mask]).square().mean())
        if rollout.normalized_entropies is not None:
            result[prefix + "normalized_entropy"] = float(rollout.normalized_entropies[mask].mean())
        if rollout.termination_reasons is not None:
            for reason in ("success", "death", "cycle_limit", "step_limit", "backend_truncated"):
                result[prefix + reason] = float(sum(
                    value == reason and int(rollout.acts[t, e]) == act
                    for t, row in enumerate(rollout.termination_reasons) for e, value in enumerate(row)
                ))
    return result


def shared_gradients(trainer, rollout, chunks, normalized_advantages) -> dict[str, float]:
    from sls.rl.ppo import clipped_policy_loss

    devices = [trainer.device.index if trainer.device.index is not None else torch.cuda.current_device()] if trainer.device.type == "cuda" else []
    with torch.random.fork_rng(devices=devices):
        logp, values, _ = trainer._evaluate_sequences(rollout, chunks)
        old_logp = trainer._select_sequences(rollout.old_log_probabilities, chunks).to(trainer.device)
        advantages = trainer._select_sequences(normalized_advantages, chunks).to(trainer.device)
        old_values = trainer._select_sequences(rollout.old_values, chunks).to(trainer.device)
        returns = trainer._select_sequences(rollout.returns, chunks).to(trainer.device)
        policy_loss = clipped_policy_loss((logp - old_logp).exp(), advantages, trainer.config.clip_ratio)
        clipped = old_values + (values - old_values).clamp(-trainer.config.value_clip_ratio, trainer.config.value_clip_ratio)
        value_loss = 0.5 * torch.maximum((values - returns).square(), (clipped - returns).square()).mean()
        parameters = [p for name, p in trainer.model.named_parameters()
                      if not name.startswith("value_head.") and p.requires_grad]
        policy_grad = torch.autograd.grad(policy_loss, parameters, retain_graph=True, allow_unused=True)
        value_grad = torch.autograd.grad(value_loss, parameters, allow_unused=True)
        zero = values.new_zeros(())
        shared = [(a, b) for a, b in zip(policy_grad, value_grad, strict=True)
                  if a is not None and b is not None]
        pn = sum((a.square().sum() for a, _ in shared), zero).sqrt()
        vn = sum((b.square().sum() for _, b in shared), zero).sqrt()
        dot = sum(((a * b).sum() for a, b in shared), zero)
        return {"shared_policy_grad_norm": float(pn), "shared_value_grad_norm": float(vn),
                "shared_grad_cosine": float(dot / (pn * vn).clamp_min(1e-20)),
                "shared_weighted_value_to_policy_grad_ratio": float(
                    trainer.config.value_coefficient * vn / pn.clamp_min(1e-20))}
