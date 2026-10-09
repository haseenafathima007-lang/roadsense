# Which model for which phase (Google AI Pro + Antigravity)

Checked early Oct 2026 from public docs and community reports; **re-check in Antigravity -> Settings -> Models**, because
availability and quotas change. Reported at that time for non-trial Pro: Gemini 3.1 Pro, Gemini 3.8/3.7/3.6 Flash,
Claude Sonnet 5.5 and Claude Opus 5.5. Older Claude 4.6 models and GPT-OSS-120B were scheduled for removal on 2 Nov 2026.
Pro quotas are limited (baseline refresh plus weekly caps); users report Claude quota being the scarcest.

## Strategy
1. **Plan with the strongest model, build with a mid model, verify with a different family.**
2. Spend Opus 5.5 only on design-critical planning (Phases 5, 7, 9 plans and the Phase 10 review).
3. Use Sonnet 5.5 for most Java implementation; Gemini 3.1 Pro for Python/data/ML and long documents; Flash for boilerplate, tests, docs.
4. One fresh conversation per phase. Keep tasks small. Keep "AI Credit Overages" on "Never" unless you decide otherwise.
5. If a model is locked, swap down one tier rather than waiting: Opus -> Sonnet -> Gemini 3.1 Pro -> Flash.

| Phase | Builder | Verifier (different family) |
|---|---|---|
| 0 Repo standards | Gemini 3.8 Flash | Gemini 3.1 Pro |
| 1 Data hardening | Gemini 3.1 Pro | Claude Sonnet 5.5 |
| 2 Training | Gemini 3.1 Pro | Claude Sonnet 5.5 |
| 3 Evaluation + Chennai | Gemini 3.1 Pro | Claude Sonnet 5.5 |
| 4 Perception + inputs | Claude Sonnet 5.5 | Gemini 3.1 Pro |
| 5 Analysis | Plan: Claude Opus 5.5; build: Sonnet 5.5 | Gemini 3.1 Pro |
| 6 Priority + OSM | Claude Sonnet 5.5 | Gemini 3.1 Pro |
| 7 Persistence + roles | Plan: Opus 5.5; build: Sonnet 5.5 | Gemini 3.1 Pro |
| 8 UI | Claude Sonnet 5.5 (Gemini 3.1 Pro if Claude is locked) | Gemini 3.1 Pro / manual checklist |
| 9 Verification | Plan: Opus 5.5; build: Sonnet 5.5 | Gemini 3.1 Pro |
| 10 Hardening | Claude Opus 5.5 (review) + Sonnet 5.5 | Gemini 3.1 Pro |
| 11 Demo + report | Gemini 3.1 Pro (writing), Flash (UML boilerplate) | Claude Sonnet 5.5 |

## Compute for training (Phase 2)
Training runs locally through the Antigravity terminal, on your own hardware (NVIDIA GPU or Apple Silicon is workable for a small model; CPU-only is very slow).
Phase 2 measures your machine first and sizes the plan to your time budget. Long runs are launched in the background so they do not burn agent quota.
Colab/Kaggle is an optional fallback documented in `docs/COLAB_FALLBACK.md`.
