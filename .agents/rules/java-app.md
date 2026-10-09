# Java rules (loaded for java-app/**)
Java 21, Maven, JUnit 5 + AssertJ. `mvn verify` green before commit. `ui` has no domain logic; `domain` has no I/O imports.
Records for value objects; guarded state in `Defect`; interfaces injected; `FakeDetector` in tests; thresholds only from `Thresholds` (immutable, loaded once).
No video/tracker concepts. Doubt on merge => NEEDS_REVIEW. Record every dependency in docs/DEPENDENCIES.md with licence.
