# Phase 7: Persistence, roles, pipeline
**Branch:** `phase/07-persistence-roles` | **Builder:** plan Claude Opus 5.5, build Claude Sonnet 5.5 | **Verifier:** Gemini 3.1 Pro | **Quota:** heavy (plan), medium (build) | **Depends on:** Phases 4-6

## Goal
Everything survives a restart, every change is audited, roles gate actions, and `ReportPipeline` ties phases 4-6 together with batch uploads.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (4, 5.2, 5.7, 5.10, 10), docs/ARCHITECTURE.md. PHASE 7: persistence + roles + pipeline. Java only.
STEP 1 (use your strongest reasoning): Implementation Plan with the SQLite schema (reports, observations, defects, verifications, audit, users, schema_version), indexes, transaction boundaries, the pipeline stage list, and the concurrency plan. Wait for approval.
STEP 2:
 - persistence: Repository<T,ID> generic interface; ReportRepository, DefectRepository, VerificationRepository, AuditRepository; SqliteDatabase (JDBC, candidate driver: verify licence), versioned migrations (hand-rolled or a library; record in DEPENDENCIES.md),
   PreparedStatements only, JDBC transactions where multi-row consistency matters, in-memory DB for tests.
 - Audit: every state change writes (who, when, from, to, note) in the same transaction as the change. Audit rows are append-only (no update/delete API).
 - security: abstract User; Surveyor, Engineer, Admin with permissions() returning a Permission set; a PermissionGuard used by services (e.g. only Engineer/Admin can set workflow states; only Admin manages users/thresholds).
   Passwords: if you add local login, use a vetted password-hashing approach (verify library) and never store plaintext; otherwise document that identity is a selected role for this prototype (state it plainly as a limitation).
 - config: Thresholds immutable, loaded once, validated (null/missing keys -> clear error).
 - service: ReportPipeline as a Template Method with the fixed stage order from design 5.2 (hooks overridable for the survey-mode pipeline later); ReportBuilder; BatchUploadService using ExecutorService/CompletableFuture with ProgressListener (Observer).
   One failing photo must not abort the batch; failures are recorded per file with a reason. Pipeline order: EXIF first, then mark image for stripped storage, quality gates (flag), detect, location, redact placeholder (real redaction in Phase 10 but the stage exists), observations, assembler, best view, severity, priority, snap, persist + audit.
 - Idempotency: the same file hash uploaded twice does not create duplicate reports (return the existing one, record an audit note).
TESTS: repository CRUD with in-memory DB; transaction rollback leaves no partial rows; audit append-only; permission matrix table-driven; pipeline end-to-end with FakeDetector and generated images (merge case, review case, no-location case, quality-flag case);
 concurrency test with N parallel uploads yields consistent counts; duplicate-hash upload; migration from empty DB.
RULES: no UI code; no network; no real photos; mvn verify green.
OUTPUT: schema SQL, class diagram in docs/uml/persistence.md, commands + output, decisions for me.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 7. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-07.md.
Checks:
1. `mvn -B verify` green; counts and coverage for persistence, security, service.
2. Schema file matches docs/ARCHITECTURE.md table list; indexes exist for location/defect lookups; no string-concatenated SQL (grep for "+ " inside SQL strings and Statement usage).
3. Rollback test: show a forced failure mid-transaction leaves zero partial rows.
4. Audit is append-only: no update/delete methods or SQL on the audit table (grep); each transition test shows exactly one audit row with who/when/from/to/note.
5. Permission matrix: list every (role, action) tested; Surveyor cannot set workflow state; Engineer cannot manage users/thresholds.
6. Pipeline: stage order equals design 5.2 (show the template method); end-to-end tests cover merge, NEEDS_REVIEW, no-location, quality-flag cases.
7. Batch: inject one failing file; the others succeed and the failure is reported; ProgressListener received events (test evidence).
8. Concurrency test is deterministic enough to be reliable (run it 5 times; report any flake).
9. Duplicate file hash: no duplicate report; audit note present.
10. Thresholds fail-fast on missing keys (test evidence). No passwords stored in plaintext (grep and schema).
11. Domain has no JDBC imports; ui untouched.
Verdict + defects.
````

## EXPECTED OUTPUTS
- Schema + migrations, repositories, audit, roles, `ReportPipeline`, `BatchUploadService`, tests, `docs/uml/persistence.md`.
- Pass criteria: transactional integrity proven, audit append-only, permissions enforced in services (not only the UI), batch resilient.

## Commit / PR
`feat: sqlite persistence, audit, roles, report pipeline and batch upload`
