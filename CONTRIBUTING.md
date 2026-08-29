# Contributing

Use Python 3.12 and keep changes deterministic across Windows and Linux.

Before submitting a change, run:

```bash
python tools/bootstrap.py --with-model
python -m ruff check .
python -m pytest -q
```

Native simulator changes must add a regression seed or mechanism test. Changes
to observations, actions, rewards, curriculum, vocabulary, model architecture,
or checkpoints require a schema version update and an exact-resume test.
