# How to use these prompts

1. Put your design document at `docs/03_image_only_java_redesign.md`. Commit this scaffold to GitHub first (`main`), then branch per phase.
2. Open the repo folder in Antigravity. It loads `AGENTS.md` and `.agents/rules/` automatically.
3. Per phase: new conversation -> choose the **Builder** model from `docs/MODEL_GUIDE.md` -> Planning mode -> paste the MASTER PROMPT -> review the Implementation Plan -> approve.
4. After the build: new conversation -> choose the **Verifier** model (different family) -> paste the VERIFICATION PROMPT.
5. Paste any FAIL items back into the builder conversation. Repeat until all PASS. Then PR and tick `docs/PHASE_STATUS.md`.

Every master prompt begins with the same context line so a fresh model is never guessing. Every verification prompt is read-only
and must write evidence (commands + output), not opinions.

Common verifier rules (repeated in each verification prompt):
- You are an independent reviewer. Do not modify source or tests. You may create `docs/verification/phase-NN.md` only.
- Run the commands yourself. Quote output. Mark each check PASS / FAIL / NOT RUN (with reason).
- Any number in the docs that no script produced is a FAIL.
