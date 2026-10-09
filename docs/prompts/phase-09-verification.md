# Phase 9: Proof-of-visit verification + state machines
**Branch:** `phase/09-verification` | **Builder:** plan Claude Opus 5.5, build Claude Sonnet 5.5 | **Verifier:** Gemini 3.1 Pro | **Quota:** heavy (plan), medium (build) | **Depends on:** Phases 5-8

## Goal
A defect never becomes "fixed" because it was missing from a photo. After photos pass explicit checks, an engineer signs off, and recurrences are caught.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (5.6, 5.7, 5.9), thresholds.yaml (verify.*). PHASE 9: verification + lifecycle. Java.
STEP 1 (use your strongest reasoning): Implementation Plan with BOTH state machines as tables (state, event, guard, next state, audit note), every illegal transition, and the check flow. Wait for approval.
STEP 2:
 - lifecycle: enums MachineState {NEW, REPORTED_AGAIN, NEEDS_REVIEW, AFTER_PHOTO_CLEAR, AFTER_PHOTO_STILL_DAMAGED, AFTER_PHOTO_REJECTED, REAPPEARED} and WorkflowState {UNREVIEWED, CONFIRMED, REJECTED, REPAIR_ORDERED, REPAIR_IN_PROGRESS, REPAIRED_CLAIMED, VERIFIED_FIXED}
   exactly as design 5.7; DefectStateMachine (State pattern) enforcing legal transitions; IllegalTransitionException; every transition writes an audit row in the same transaction.
 - ProofOfVisitChecker with one Strategy/check per row of design 5.6: time (after repair-claimed date; label EXIF time as evidence, not proof), location (within visit_radius_m, accuracy considered), scene match (ImageSimilarity >= min_scene_similarity),
   quality (QualityGate + defect area plausibly in frame), detector (no same-family damage at >= clear_conf; patch class not counted as damage). The result lists EVERY failed check with reasons.
 - VerificationService: AFTER_PHOTO_CLEAR / AFTER_PHOTO_STILL_DAMAGED / AFTER_PHOTO_REJECTED; VERIFIED_FIXED only via an Engineer/Admin sign-off with a mandatory note (guard: requires AFTER_PHOTO_CLEAR or an explicit override note).
   A later report at the same place after VERIFIED_FIXED sets REAPPEARED and increases the recurrence factor (use the existing RecurrenceFactor).
 - Wire into ui only through existing view-models: a "Submit after photo" action in the defect detail screen and the sign-off dialog (Engineer/Admin only).
TESTS: exhaustive transition table test (every state x event; legal -> expected state, illegal -> exception); each check has pass and fail cases with FakeDetector and generated images; combined multi-failure result; sign-off without note rejected;
 Surveyor cannot sign off; reappearance raises priority (compute before/after); EXIF time edited case documented as limitation in a test name/comment; audit rows counted per transition.
RULES: no staged data presented as real; mvn verify green.
OUTPUT: state tables saved in docs/uml/lifecycle.md (Mermaid stateDiagram), files, commands + output.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 9. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-09.md.
Checks:
1. `mvn -B verify` green; counts and coverage for lifecycle.
2. Enum constants equal design 5.7 exactly (diff the lists); no extra or missing states.
3. Exhaustive transition test: show its size (states x events) and that illegal transitions throw IllegalTransitionException.
4. One test per ProofOfVisit check (pass + fail) as in design 5.6; the multi-failure test lists all reasons.
5. A defect can reach VERIFIED_FIXED only via sign-off with a note; try to bypass it through every public API (list the attempts and the outcomes).
6. Role enforcement: Surveyor sign-off denied (test evidence).
7. Reappearance: show priority before and after with real numbers from the test.
8. Each transition writes exactly one audit row in the same transaction (rollback test).
9. "Absence from a photo" never triggers fixed: grep for any logic that marks fixed from a detection-free report; show the test.
10. UML state diagrams exist in docs/uml/lifecycle.md and match code.
Verdict + defects.
````

## EXPECTED OUTPUTS
- Lifecycle package, checker + service, UI hooks, tests, `docs/uml/lifecycle.md`.
- Pass criteria: no path to VERIFIED_FIXED without sign-off; all failed checks reported; every transition audited.

## Commit / PR
`feat(lifecycle): proof-of-visit verification and state machines`
