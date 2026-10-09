# Python rules (loaded for ai-service/**)
Python 3.11, FastAPI, ruff + pytest clean. Stateless detection only; no business logic. Never commit weights, datasets or runs.
Metrics only from eval scripts writing docs/*.json. Pin dependencies after checking current versions and licences.
