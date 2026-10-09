# Architecture (summary of design §2, §4, §5; the design document wins on conflict)

## Layers
Perception (Python, stateless) -> Decision engine (Java) -> Presentation (JavaFX). Training/eval is offline Python.

## Core concepts
Report (one photo) -> Observation (one detection) -> Defect (real-world defect) -> Verification (after photo).

## Java package map (`com.roadai`)
domain, perception, imaging, geo, analysis, priority, lifecycle, persistence, service, security, config, ui.
Full class list: design §4.1. OOP concept mapping: design §4.2 (maintained in `docs/OOP_DESIGN.md`).

## Pipeline (template method `ReportPipeline`)
Upload -> read EXIF + mark for stripping -> quality gates (flag, never silently drop) -> detect ->
resolve location (EXIF -> device -> manual pin; store source + accuracy) -> redact -> create observations ->
assemble defects (dedup) -> best view -> severity -> priority (with breakdown) -> snap to OSM road link -> persist + audit.

## Data tables (Phase 7)
reports, observations, defects, verifications, audit, users, schema_version.

## Two state machines
Machine state (system, from photos) and workflow state (officers, audited). See design §5.7.

## Decisions
See `docs/adr/`.
